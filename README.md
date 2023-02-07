### 1. 项目背景
随着数据量的增长,以及业务要求可以实现模糊检索,而,普通的数据库,会带出现性能瓶劲,这时候就要考虑用elasticsearch了,
现在市面上,普通数据库(比如:mysql)到elasticsearch之间的同步,皆是通过binlog订阅的方式,而这种方式会出现一定的延迟,有没有一种方案,可以解决延迟呢? 

### 2. 功能介绍
该组件的功能的主要功能是:
1) 拦截"业务"对数据库的操作. 
2) 在commit之前,反向生成sql语句(UPDATE/DELETE转换成SELECT),并且,根据sql语句检索出,受影响的"数据行"(称之为:beforeImage). 
3) 业务系统commit
4) 在commit之后,再次执行第2步的sql语句,并hold住所有结果(这个结果集,称之为:afterImage) 
5) 把快照信息(beforeImage/afterImage)通过一张undo表进行存储(该思想源于seata at模式的源码). 
6) 把快照信息(beforeImage/afterImage)交给elasticsearch进行操作. 

### 3. 缺点介绍

1. 没有模糊查询这种场景不要使用这个插件.  
2. 很在乎写入的性能,建议不要该插件,因为,这是一个同步双写操作.  