package org.example;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlClient;
import org.example.utils.Database;
import org.example.routes.Server;

public class Bootstrap
{
    public static final SqlClient client = Database.getClient();

    public static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args)
    {
        vertx.deployVerticle(new Server(),res->{
           if(res.succeeded())
           {
               System.out.println("Ok");
           }
           else
           {
               System.out.println("Error "+res.cause().getMessage());
           }
        });

    }
}