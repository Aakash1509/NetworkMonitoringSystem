package org.example.routes;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.example.database.CredentialQuery;

public class Credentials implements CrudOperations
{
    private final CredentialQuery credentialQuery = new CredentialQuery();

    private static final Logger logger = LoggerFactory.getLogger(Credentials.class);
    
    public void route(Router credentialRouter) 
    {
        try
        {
            credentialRouter.post("/create")
                    .handler(TimeoutHandler.create(5000))
                    .handler(BodyHandler.create())
                    .handler(this::create)
                    .failureHandler(this::timeout);

            credentialRouter.put("/:id")
                    .handler(TimeoutHandler.create(5000))
                    .handler(BodyHandler.create())
                    .handler(this::update)
                    .failureHandler(this::timeout);

            credentialRouter.get("/getAll")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::getAll)
                    .failureHandler(this::timeout);

            credentialRouter.get("/:id")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::get)
                    .failureHandler(this::timeout);

            credentialRouter.delete("/:id")
                    .handler(TimeoutHandler.create(5000))
                    .handler(this::delete)
                    .failureHandler(this::timeout);
        }
        catch (Exception exception)
        {
            logger.error("Error in credential routing", exception);
        }
    }

    private void timeout(RoutingContext context)
    {
        if (!context.response().ended())
        {
            context.response()
                .setStatusCode(408)
                .end(new JsonObject()
                        .put("status.code",408)
                        .put("message","Request Timed Out")
                        .put("error","The server timed out waiting for the request").encodePrettily());
            
        }
    }

    //Creating credential profile
    @Override
    public void create(RoutingContext context)
    {
        try
        {
            var requestBody = context.body().asJsonObject();

            credentialQuery.insert(requestBody.getString("credential.profile.name"),
                            requestBody.getString("credential.profile.protocol"),
                            requestBody.getString("user.name"),
                            requestBody.getString("user.password"),
                            requestBody.getString("community"),
                            requestBody.getString("version"))
                    .onComplete(result->{
                        if (result.succeeded())
                        {
                            var profileId = result.result();

                            JsonObject response = new JsonObject()
                                    .put("credential.profile.id", profileId);
                            
                            context.response()
                                    .setStatusCode(201)
                                    .end(new JsonObject()
                                            .put("status.code",201)
                                            .put("message","Credential profile created successfully")
                                            .put("data",response).encodePrettily());
                        }
                        else
                        {
                            context.response()
                                    .setStatusCode(500)
                                    .end(new JsonObject()
                                            .put("status.code",500)
                                            .put("message","Failed to create credential profile")
                                            .put("error",result.cause().getMessage()).encodePrettily());
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in creating credential profile")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

    //Updating credential profile
    @Override
    public void update(RoutingContext context)
    {
        try
        {
            var profileID = Long.parseLong(context.pathParam("id"));

            var requestBody = context.body().asJsonObject();

            credentialQuery.update(profileID,
                            requestBody.getString("credential.profile.name"),
                            requestBody.getString("credential.profile.protocol"),
                            requestBody.getString("user.name"),
                            requestBody.getString("user.password"),
                            requestBody.getString("community"),
                            requestBody.getString("version"))
                    .onComplete(result->{
                       if(result.succeeded())
                       {
                           context.response()
                                   .setStatusCode(200)
                                   .end(new JsonObject()
                                           .put("status.code",404)
                                           .put("message","Credential profile updated successfully").encodePrettily());
                       }
                       else
                       {
                           if (result.cause().getMessage().contains("Credential profile not found"))
                           {
                               context.response()
                                       .setStatusCode(404)
                                       .end(new JsonObject()
                                               .put("status.code",404)
                                               .put("message","Credential profile not found of this ID")
                                               .put("error",result.cause().getMessage()).encodePrettily());
                           }
                           else
                           {
                               context.response()
                                       .setStatusCode(500)
                                       .end(new JsonObject()
                                               .put("status.code",500)
                                               .put("message","Database error while updating credential profile")
                                               .put("error",result.cause().getMessage()).encodePrettily());
                           }
                       }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in updating credential profile")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

    //Deleting credential profile
    @Override
    public void delete(RoutingContext context)
    {
        try
        {
            credentialQuery.delete(Long.parseLong(context.pathParam("id")))
                    .onComplete(result->{
                       if(result.succeeded())
                       {
                           context.response()
                                   .setStatusCode(200)
                                   .end(new JsonObject()
                                           .put("status.code",404)
                                           .put("message","Credential profile deleted successfully").encodePrettily());
                       }
                       else
                       {
                           if (result.cause().getMessage().contains("Credential profile not found"))
                           {
                               context.response()
                                       .setStatusCode(404)
                                       .end(new JsonObject()
                                               .put("status.code",404)
                                               .put("message","Credential profile not found of this ID")
                                               .put("error",result.cause().getMessage()).encodePrettily());
                           }
                           else
                           {
                               context.response()
                                       .setStatusCode(500)
                                       .end(new JsonObject()
                                               .put("status.code",500)
                                               .put("message","Database error while deleting credential profile")
                                               .put("error",result.cause().getMessage()).encodePrettily());
                           }
                       }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in deleting credential profile")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

    //Fetching credential profile
    @Override
    public void get(RoutingContext context)
    {
        try
        {
            credentialQuery.get(Long.parseLong(context.pathParam("id")))
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            context.response()
                                    .setStatusCode(200)
                                    .end(new JsonObject()
                                            .put("status.code",200)
                                            .put("message","Credential profile fetched successfully")
                                            .put("data",result.result()).encodePrettily());
                        }
                        else
                        {
                            if (result.cause().getMessage().contains("Credential profile not found"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(new JsonObject()
                                                .put("status.code",404)
                                                .put("message","Credential profile not found of this ID")
                                                .put("error",result.cause().getMessage()).encodePrettily());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(500)
                                        .end(new JsonObject()
                                                .put("status.code",500)
                                                .put("message","Database error while fetching credential profile")
                                                .put("error",result.cause().getMessage()).encodePrettily());
                            }
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in fetching credential profile for given ID")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }

    //Fetching credential profiles
    @Override
    public void getAll(RoutingContext context)
    {
        try
        {
            credentialQuery.getAll()
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            var credentials = result.result();

                            if(!credentials.isEmpty())
                            {
                                context.response()
                                        .setStatusCode(200)
                                        .end(new JsonObject()
                                                .put("status.code",200)
                                                .put("message","Credential profiles fetched successfully")
                                                .put("data",credentials).encodePrettily());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(200)
                                        .end(new JsonObject()
                                                .put("status.code",200)
                                                .put("message","Credential profiles fetched successfully")
                                                .put("data","No credential profiles currently").encodePrettily());
                            }
                        }
                        else
                        {
                            context.response()
                                    .setStatusCode(500)
                                    .end(new JsonObject()
                                            .put("status.code",500)
                                            .put("message","Database error while fetching credential profiles")
                                            .put("error",result.cause().getMessage()).encodePrettily());
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Server error in fetching credential profiles")
                            .put("error",exception.getCause().getMessage()).encodePrettily());
        }
    }
}
