package org.example;

import io.vertx.core.Vertx;
import org.example.poll.Poller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.poll.Scheduler;
import org.example.routes.Server;

public class Bootstrap
{
    public static final Vertx vertx = Vertx.vertx();

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args)
    {
        vertx.deployVerticle(new Server())

                .compose(result -> vertx.deployVerticle(new Scheduler()))

                .compose(result -> vertx.deployVerticle(new Poller()))

                .onComplete(result ->
                {
                    if (result.succeeded())
                    {
                        logger.info("All verticles deployed successfully: Server, Scheduler, and Poller");
                    }
                    else
                    {
                        logger.error("Error deploying verticles", result.cause());
                    }
                });
    }
}