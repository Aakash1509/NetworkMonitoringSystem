package org.example.routes;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;

public class Server extends AbstractVerticle
{
    public void start()
    {
        var router = Router.router(vertx);

        var discoveryRouter = Router.router(vertx);

        var discoveryObject = new Discovery();

        discoveryObject.route(discoveryRouter);

        router.route("/api/v1/discovery/*").subRouter(discoveryRouter);

        router.get("/").handler(ctx->{
            ctx.response().end("Welcome to Homepage!");
        });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080,http->{
                if(http.succeeded())
                {
                    System.out.println("Server started on port 8080");
                }
                else
                {
                    System.out.println("Failed to start the server");
                }
            });
    }

}
