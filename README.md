### 1. 项目背景

随着数据量的增长,以及业务要求可以实现模糊检索,而,普通的数据库,在模糊检索方面表现不佳,这时候就要考虑用elasticsearch了,
现在市面上,数据库(比如:mysql)到elasticsearch之间的同步,皆是通过binlog订阅的方式,而这种方式会出现延迟的可能性,有没有一种方案,可以解决延迟呢?

### 2. 功能介绍
> 注意:该功能的思想以及大量源码皆源于Seata AT模式,相当于在Seata AT模式上做了一下阉割,以符合业务.  

1) 拦截"业务"对数据库的操作.
2) 在"sql执行之前",反向生成sql语句(INSERT/UPDATE/DELETE转换成SELECT...FOR UPDATE),并且,根据sql语句检索出,受影响的"数据行"(称之为:beforeImage).
3) 执行"业务SQL语句".
4) 再次,执行第2步的SELECT语句,重新检索出"业务SQL"更新后的数据行(这个结果集,称之为:afterImage)
5) 把快照信息(beforeImage/afterImage)存储在"线程上下文中". 
6) 在"执行commit之前",从"线程上下文中"中取出快照信息,把快照信息(beforeImage/afterImage)交给elasticsearch/mongodb等存储设备进行操作. 
7) 第6步(es/mongodb)执行没异常的情况下,会调用:Connection.commit()方法,如果,有异常:调用:Connection.rollback()方法.


### 3. 底层原理详细介绍
```
# 1. 查看表结构(前置条件)
mysql> desc t_order_1;
+----------+---------------+------+-----+---------+-------+
| Field    | Type          | Null | Key | Default | Extra |
+----------+---------------+------+-----+---------+-------+
| order_id | bigint(20)    | NO   | PRI | NULL    |       |
| price    | decimal(10,2) | YES  |     | NULL    |       |
| user_id  | bigint(20)    | YES  |     | NULL    |       |
| status   | varchar(50)   | YES  |     | NULL    |       |
+----------+---------------+------+-----+---------+-------+

# 2. 查看表里的数据(前置条件)
mysql> SELECT * FROM t_order_1;
+--------------------+-------+---------+---------+
| order_id           | price | user_id | status  |
+--------------------+-------+---------+---------+
| 620244932191322112 | 22.50 |       2 | SUCCESS |
| 620244932388454400 | 24.50 |       2 | SUCCESS |
| 620244932535255040 | 26.50 |       2 | SUCCESS |
| 620244932744970240 | 28.50 |       2 | SUCCESS |
| 620244932891770880 | 30.50 |       2 | SUCCESS |
| 620244933034377216 | 32.50 |       1 | SUCCESS |
| 620244933168594944 | 34.50 |       1 | SUCCESS |
+--------------------+-------+---------+---------+


# 3. 假设要执行的业务操作是:更新订单表里,用户id为1的订单状态为:FAIL((前置条件)


# 4. beforeImage SQL(对业务SQL改写)
mysql> SELECT * FROM t_order_1 WHERE user_id = 1 FOR UPDATE;
beforeImage内容:
{ "order_id" : 620244933168594944 , "price": 34.50, , "user_id": 1, "status":"SUCCESS" }
{ "order_id" : 620244933034377216 , "price": 32.50, , "user_id": 1, "status":"SUCCESS" }

# 5. 业务操作SQL
mysql> UPDATE  t_order_1 SET status = "FAIL" WHERE user_id = 1;


# 6. afterImage SQL(对业务SQL改写)
mysql> SELECT * FROM t_order_1 WHERE user_id = 1 FOR UPDATE;
afterImage内容:
{ "order_id" : 620244933168594944 , "price": 34.50, , "user_id": 1, "status":"FAIL" }
{ "order_id" : 620244933034377216 , "price": 32.50, , "user_id": 1, "status":"FAIL" }


# 7. 需要说明的一点
# 7.1 INSERT语句时,beforeImage是没有内容的,afterImage是插入后的最新数据.
# 7.2 UPDATE语句时,beforeImage和afterImage都有数据. 
# 7.3 DELETE语句时,beforeImage有数据,而afterImage是没有数据的.
```

### 4. 插件缺点介绍

1) 没有模糊查询这种场景不要使用这个插件.
2) 业务场景,在乎写入的性能,建议不要用该插件,因为,这是一个同步双写操作. 
3) "批量数据",当"部份失败"的情况下,需要全部插件自己去"实现全量rollback"(暂时未找到更好的解决方案,如果有更好的方案,可提ISSUE一起沟通学习) 

### 5. 插件优点介绍
1) 实时性比较高.
2) 不与其它存储设备打交道的情况下,可,保证:事务一致性.    

### 6. 插件适应场景列举
1) mysq与es(mongodb)双写强一致性情况.    
2) 业务做完某个操作后,想让MQ"异步"去做某件事情,同时,"必须保证消息的不丢失",此时,可以写一个插件,把快照信息,保存到DB中(要求共用同一个Connection,保证事务一致),并且,直到MQ发送成功为止,再回来删表里的数据(不过会延生出另一个问题:消息顺序性).       
3) 想要监听业务的变化,列举出修改前后的对比详细信息.   

### 7. 插件不适应场景
1) 暂时不支持多写(即:es/mongodb/redis等之间只能选择一个),因为:rollback时,无法保证其它存储设备之间都可以进行:rollback.  

### 8. 插件编写步骤
> 1) 引入依赖

```
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
</dependency>
<dependency>
    <groupId>help.lixin.double.write</groupId>
    <artifactId>db-double-write-datasource</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> 2) 实现插件接口(IDataSyncService)

```
package help.lixin.plugin.api;

import help.lixin.plugin.ctx.SyncContext;

// 插件接口定义
public interface IDataSyncService {
    void sync(SyncContext ctx) throws Exception;
}
```

```
package help.lixin.data.sync.plugin;

import help.lixin.plugin.api.IDataSyncService;
import help.lixin.plugin.ctx.SyncContext;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.SQLType;

import java.util.List;

public class ElasticSearchDataSyncService implements IDataSyncService {
    @Override
    public void sync(SyncContext ctx) throws Exception {
        List<SQLUndoLog> items = ctx.getItems();
        for (SQLUndoLog log : items) {
            SQLType sqlType = log.getSqlType();
            TableRecords beforeImage = log.getBeforeImage();
            TableRecords afterImage = log.getAfterImage();
            System.out.println("\n\n");
            System.out.println("sqlType: " + sqlType);
            if (null != afterImage) {
                List<Row> rows = afterImage.getRows();
                for (Row row : rows) {
                    System.out.println("tableName:" + afterImage.getTableName());
                    List<Field> fields = row.getFields();
                    for (Field field : fields) {
                        System.out.println(field.getName() + "--------------" + field.getValue());
                    }
                }
            }
        }
    }
}
```

> 3) SPI发现插件

```
lixin-macbook:main lixin$ tree resources/
resources/
├── META-INF
│   └── services
│       └── help.lixin.plugin.api.IDataSyncService
```

```
help.lixin.data.sync.plugin.ElasticSearchDataSyncService
```

> 4) 对DataSource进行代理(BeanPostProcessor)

```
package help.lixin.data.sync;

import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;

public class DataSourceBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            // 对DataSource进行代理
            DataSourceProxy proxy = new DataSourceProxy((DataSource) bean);
            return proxy;
        } else {
            return bean;
        }
    }
}
```

> 5) 配置生效

```
@Configuration
public class DataSyncConfig {

    @Bean
    public BeanPostProcessor dataSourceBeanPostProcessor() {
        BeanPostProcessor beanPostProcessor = new DataSourceBeanPostProcessor();
        return beanPostProcessor;
    }
}
```

### 9. 案例
请参考example
