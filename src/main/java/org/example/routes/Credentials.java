package org.example.routes;

import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.example.database.QueryUtility;

import java.util.List;

public class Credentials implements CrudOperations
{
    private static final Logger logger = LoggerFactory.getLogger(Credentials.class);
    
    public void route(Router credentialRouter) 
    {
        try
        {
            credentialRouter.post("/create").handler(BodyHandler.create()).handler(this::create);

            credentialRouter.put("/:id").handler(BodyHandler.create()).handler(this::update);

            credentialRouter.get("/getAll").handler(this::getAll);

            credentialRouter.get("/:id").handler(this::get);

            credentialRouter.delete("/:id").handler(this::delete);
        }
        catch (Exception exception)
        {
            logger.error("Error in credential routing", exception);
        }
    }

    //Creating credential profile
    @Override
    public void create(RoutingContext context)
    {
        try
        {
            var requestBody = context.body().asJsonObject();

            var name = requestBody.getString("credential.profile.name");

            var protocol = requestBody.getString("credential.profile.protocol");

            if(name == null || name.isEmpty() || protocol == null || protocol.isEmpty())
            {
                context.response()
                        .setStatusCode(500)
                        .end(new JsonObject()
                                .put("status.code",500)
                                .put("message","Please enter both username and protocol").encodePrettily());

                return;
            }

            List<String> columns = List.of("profile_id");

            QueryUtility.getInstance().get("credentials", columns, new JsonObject().put("name", name))
                    .compose(result ->
                    {
                        if (!result.containsKey("error"))
                        {
                            // If discovery name already exists, return a failed future
                            return Future.failedFuture("Credential profile name should be unique");
                        }
                        else
                        {
                            // If name is unique, insert
                            return QueryUtility.getInstance().insert("credentials",new JsonObject()
                                    .put("profile_name",name)
                                    .put("profile_protocol",protocol)
                                    .put("user_name",requestBody.getString("user.name"))
                                    .put("user_password",requestBody.getString("user.password"))
                                    .put("community",requestBody.getString("community"))
                                    .put("version",requestBody.getString("version")));
                        }
                    })
                    .onComplete(result->
                    {
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

        var credentialID = context.pathParam("id");

        var requestBody = context.body().asJsonObject();

        var name = requestBody.getString("credential.profile.name");

        var protocol = requestBody.getString("credential.profile.protocol");

        if(name == null || name.isEmpty() || protocol == null || protocol.isEmpty() || credentialID == null || credentialID.isEmpty())
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code",500)
                            .put("message","Please enter all the details of credentialID , username and protocol").encodePrettily());
            return;
        }
        try
        {
            long id = Long.parseLong(credentialID);

            List<String> columns = List.of("profile_id");

            QueryUtility.getInstance().get("credentials", columns, new JsonObject().put("name", name))
                    .compose(result ->
                    {
                        if (!result.containsKey("error"))
                        {
                            // If discovery name already exists, return a failed future
                            return Future.failedFuture("Credential profile name should be unique");
                        }
                        else
                        {
                            // If name is unique, update
                            return QueryUtility.getInstance().update("credentials",new JsonObject()
                                    .put("profile_name",name)
                                    .put("profile_protocol",protocol)
                                    .put("user_name",requestBody.getString("user.name"))
                                    .put("user_password",requestBody.getString("user.password"))
                                    .put("community",requestBody.getString("community"))
                                    .put("version",requestBody.getString("version")),new JsonObject().put("profile_id",id));
                        }
                    })

                    .onComplete(result->
                    {
                       if(result.succeeded())
                       {
                           context.response()
                                   .setStatusCode(200)
                                   .end(new JsonObject()
                                           .put("status.code",200)
                                           .put("message","Credential profile updated successfully").encodePrettily());
                       }
                       else
                       {
                           if (result.cause().getMessage().contains("Information not found"))
                           {
                               context.response()
                                       .setStatusCode(404)
                                       .end(new JsonObject()
                                               .put("status.code",404)
                                               .put("message","Credential profile not found of this ID").encodePrettily());
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
        var credentialID = context.pathParam("id");

        if (credentialID == null || credentialID.isEmpty())
        {
            context.response()
                    .setStatusCode(404)
                    .end(new JsonObject()
                            .put("status.code", 404)
                            .put("message", "Please enter a valid credential ID").encodePrettily());
            return;
        }
        try
        {
            long id = Long.parseLong(credentialID);

            QueryUtility.getInstance().delete("credentials","profile_id",id)
                    .onComplete(result ->
                    {
                        if (result.succeeded()) {
                            context.response()
                                    .setStatusCode(200)
                                    .end(new JsonObject()
                                            .put("status.code", 200)
                                            .put("message", "Credential profile deleted successfully").encodePrettily());
                        }
                        else
                        {
                            if (result.cause().getMessage().contains("Information not found"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(new JsonObject()
                                                .put("status.code", 404)
                                                .put("message", "Credential profile not found for this ID").encodePrettily());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(500)
                                        .end(new JsonObject()
                                                .put("status.code", 500)
                                                .put("message", "Database error while deleting credential profile")
                                                .put("error", result.cause().getMessage()).encodePrettily());
                            }
                        }
                    });

        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("status.code", 500)
                            .put("message", "Server error in deleting credential profile")
                            .put("error", "Please enter a valid credential profile ID").encodePrettily());
        }
    }


    //Fetching credential profile
    @Override
    public void get(RoutingContext context)
    {
        var credentialID = context.pathParam("id");

        if (credentialID == null || credentialID.isEmpty())
        {
            context.response()
                    .setStatusCode(404)
                    .end(new JsonObject()
                            .put("status.code", 404)
                            .put("message", "Please enter a valid credential ID").encodePrettily());
            return;
        }
        try
        {
            long id = Long.parseLong(credentialID);

            List<String> columns = List.of("profile_name", "profile_protocol", "user_name","user_password","community","version");

            QueryUtility.getInstance().get("credentials",columns,new JsonObject().put("profile_id",id))
                    .onComplete(result->
                    {
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
                            if (result.result().containsKey("error"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(new JsonObject()
                                                .put("status.code",404)
                                                .put("message","Credential profile not found of this ID").encodePrettily());
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
                            .put("status.code", 500)
                            .put("message", "Server error in deleting credential profile")
                            .put("error", "Please enter a valid credential profile ID").encodePrettily());
        }
    }

    //Fetching credential profiles
    @Override
    public void getAll(RoutingContext context)
    {
        try
        {
            QueryUtility.getInstance().getAll("credentials")
                    .onComplete(result->
                    {
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
