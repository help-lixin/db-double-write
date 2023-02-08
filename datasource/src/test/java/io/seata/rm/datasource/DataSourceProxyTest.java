/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * @author ph3636
 */
public class DataSourceProxyTest {

    private DruidDataSource targetDataSource;

    @Before
    public void init() throws Exception {
        targetDataSource = new DruidDataSource();
        targetDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/order_db?characterEncoding=utf-8&useSSL=false");
        targetDataSource.setUsername("root");
        targetDataSource.setPassword("123456");
        targetDataSource.setDefaultAutoCommit(false);
        targetDataSource.init();
    }


    @Test
    public void testInsert() throws Exception {
        Connection connection = (Connection) targetDataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO t_order_1(order_id,price,user_id,status) VALUES(?,?,?,?)");
        preparedStatement.setLong(1, 620244933478973441L);
        preparedStatement.setBigDecimal(2, new BigDecimal(39.50));
        preparedStatement.setLong(3, 2l);
        preparedStatement.setString(4, "FAIL");
        boolean execute = preparedStatement.execute();
        connection.commit();
    }

    @Test
    public void testProxyInsert() throws Exception {
        DataSourceProxy proxy = new DataSourceProxy(targetDataSource);
        Connection connection = (Connection) proxy.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO t_order_1(order_id,price,user_id,status) VALUES(?,?,?,?)");
        preparedStatement.setLong(1, 620244933478973441L);
        preparedStatement.setBigDecimal(2, new BigDecimal(39.50));
        preparedStatement.setLong(3, 2l);
        preparedStatement.setString(4, "FAIL");
        boolean execute = preparedStatement.execute();
        connection.commit();
    }

    @Test
    public void testProxyUpdate() throws Exception {
        DataSourceProxy proxy = new DataSourceProxy(targetDataSource);
        Connection connection = (Connection) proxy.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE t_order_1 SET status = ? WHERE status = ?");
        preparedStatement.setString(1, "SUCCESS");
        preparedStatement.setString(2, "FAIL");
        int cout = preparedStatement.executeUpdate();
        connection.commit();
    }

    @Test
    public void testProxyDelete() throws Exception {
        DataSourceProxy proxy = new DataSourceProxy(targetDataSource);
        Connection connection = (Connection) proxy.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM t_order_1 WHERE user_id = ?");
        preparedStatement.setLong(1,1);
        int cout = preparedStatement.executeUpdate();
        connection.commit();
    }
}
