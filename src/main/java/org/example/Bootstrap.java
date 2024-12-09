package org.example;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgPool;
import org.example.utils.Database;
import org.example.routes.Server;

public class Bootstrap
{
    public static final Vertx vertx = Vertx.vertx();

    public static final PgPool client = Database.getClient();

    public static void main(String[] args)
    {
        vertx.deployVerticle(new Server())
                .onComplete(result->{
                   if(result.succeeded())
                   {
                       System.out.println("Server verticle deployed successfully");
                   }
                   else
                   {
                       System.out.println("Something went wrong");
                   }
                });
    }
}