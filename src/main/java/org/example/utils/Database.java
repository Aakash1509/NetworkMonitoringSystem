package org.example.utils;

import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import org.example.Bootstrap;

public class Database
{
        private static SqlClient client;

        public static SqlClient getClient()
        {
                if(client==null)
                {
                        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                                .setPort(Config.DB_PORT)
                                .setHost(Config.DB_HOST)
                                .setDatabase(Config.DB_DATABASE)
                                .setUser(Config.DB_USER)
                                .setPassword(Config.DB_PASSWORD);

                        //Pool Options
                        PoolOptions poolOptions = new PoolOptions()
                                .setMaxSize(Config.POOL_SIZE);

                        client = Pool.pool(Bootstrap.vertx,connectOptions,poolOptions);
                }
                return client;
        }

}
