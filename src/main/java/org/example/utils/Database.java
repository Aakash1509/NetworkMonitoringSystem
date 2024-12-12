package org.example.utils;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.example.Bootstrap;

public class Database
{
        public static PgPool client;

        static
        {
                // Database connection options
                PgConnectOptions connectOptions = new PgConnectOptions()
                        .setPort(Config.DB_PORT)
                        .setHost(Config.DB_HOST)
                        .setDatabase(Config.DB_DATABASE)
                        .setUser(Config.DB_USER)
                        .setPassword(Config.DB_PASSWORD);

                // Pool options
                PoolOptions poolOptions = new PoolOptions()
                        .setMaxSize(Config.POOL_SIZE); // Maximum connections in the pool

                // Create client
                client = PgPool.pool(Bootstrap.vertx, connectOptions, poolOptions);
        }
}
