### 1. 项目背景

随着数据量的增长,以及业务要求可以实现模糊检索,而,普通的数据库,在模糊检索方面表现不佳,这时候就要考虑用elasticsearch了,
现在市面上,普通数据库(比如:mysql)到elasticsearch之间的同步,皆是通过binlog订阅的方式,而这种方式会出现延迟的可能性,有没有一种方案,可以解决延迟呢?

### 2. 功能介绍(思想源于seata at模式)

该组件的功能的主要功能是:

1) 拦截"业务"对数据库的操作.
2) 在"sql执行之前",反向生成sql语句(INSERT/UPDATE/DELETE转换成SELECT...FOR UPDATE),并且,根据sql语句检索出,受影响的"数据行"(称之为:beforeImage).
3) 执行"业务SQL语句".
4) 在"sql执行之后",再次执行第2步的sql语句(这个结果集,称之为:afterImage)
5) 在"sql执行后",把快照信息(beforeImage/afterImage)存储在"线程上下文中".
6) 在"执行commit之前",从"线程上下文中"中取出快照信息,把快照信息(beforeImage/afterImage)通过一张undo表进行存储(该思想源于seata at模式的源码).
7) 把快照信息(beforeImage/afterImage)交给elasticsearch/mongodb等存储设备进行操作. 
8) 第7步(es/mongodb)执行没异常的情况下,会调用:Connection.commit()方法,如果,有异常:调用:Connection.rollback()方法.

### 3. 缺点介绍

1. 没有模糊查询这种场景不要使用这个插件.
2. 业务场景,在乎写入的性能,建议不要用该插件,因为,这是一个同步双写操作.
3. 暂时不支持多写(即:es和mongodb之间只能两选一),因为:rollback时,无法保证多写之间都可以进行:rollback.  

### 4. 优点介绍

1. 实时,这应该是最大的优点了.
2. 同一个Connection的情况下,是可以保证一致性的(比如:业务做完某个操作后,想让MQ异步去做某件事情,同时,消息不能丢失,此时,你可以写一个插件,保存到DB中,并且,异步直到MQ成功为止).

### 5. undo_log.sql

```
DROP TABLE IF EXISTS undo_log;

CREATE TABLE undo_log (
  id varchar(32) NOT NULL,
  context LONGBLOB NOT NULL,
  rollback_info longblob NOT NULL,
  log_status int(11) NOT NULL,
  log_created datetime NOT NULL,
  log_modified datetime NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

### 6. 测试类

io.seata.rm.datasource.DataSourceProxyTest