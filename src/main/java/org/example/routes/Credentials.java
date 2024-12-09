package org.example.routes;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.example.database.CredentialQuery;
import org.example.model.Credential;
import org.example.utils.ApiResponse;

import java.util.List;

public class Credentials implements CrudOperations
{
    private final CredentialQuery credentialQuery = new CredentialQuery();
    
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
            System.out.println("Error in credential routing : "+exception.getMessage());
        }
    }

    private void timeout(RoutingContext context)
    {
        if (!context.response().ended())
        {
            context.response()
                    .setStatusCode(408)
                    .end(ApiResponse.error(408,"Request Timed Out","The server timed out waiting for the request.").toJson());
        }
    }

    //Creating credential profile
    @Override
    public void create(RoutingContext context)
    {
        try
        {
            var requestBody = context.body().asJsonObject();

            Credential credential = new Credential(
                    null, requestBody.getString("credential.profile.name"),
                    requestBody.getString("credential.profile.protocol"),
                    requestBody.getString("user.name"),
                    requestBody.getString("user.password"),
                    requestBody.getString("community"),
                    requestBody.getString("version")
            );

            credentialQuery.insert(credential)
                    .onComplete(result->{
                        if (result.succeeded())
                        {
                            Long profileId = result.result();

                            JsonObject response = new JsonObject()
                                    .put("credential.profile.id", profileId);
                            context.response()
                                    .setStatusCode(201)
                                    .end(ApiResponse.success(201, "Credential profile created successfully", response).toJson());
                        }
                        else
                        {
                            context.response()
                                    .setStatusCode(500)
                                    .end(ApiResponse.error(500, "Failed to create credential profile", result.cause().getMessage()).toJson());
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in creating credential profile",exception.getCause().getMessage()).toJson());
        }
    }

    //Updating credential profile
    @Override
    public void update(RoutingContext context)
    {
        try
        {
            var profileID = Long.parseLong(context.pathParam("id"));

            JsonObject requestBody = context.body().asJsonObject();

            Credential credential = new Credential(
                    profileID,
                    requestBody.getString("credential.profile.name"),
                    requestBody.getString("credential.profile.protocol"),
                    requestBody.getString("user.name"),
                    requestBody.getString("user.password"),
                    requestBody.getString("community"),
                    requestBody.getString("version")
            );

            credentialQuery.update(credential)
                    .onComplete(result->{
                       if(result.succeeded())
                       {
                           context.response()
                                   .setStatusCode(200)
                                   .end(ApiResponse.success(200, "Credential profile updated successfully ", null).toJson());
                       }
                       else
                       {
                           if (result.cause().getMessage().contains("Credential profile not found")) {
                               context.response()
                                       .setStatusCode(404)
                                       .end(ApiResponse.error(404, result.cause().getMessage(), null).toJson());
                           } else {
                               context.response()
                                       .setStatusCode(500)
                                       .end(ApiResponse.error(500, "Error updating credential profile", result.cause().getMessage()).toJson());
                           }
                       }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in updating credential profile",exception.getCause().getMessage()).toJson());
        }
    }

    //Deleting credential profile
    @Override
    public void delete(RoutingContext context)
    {
        try
        {
            var profileID = Long.parseLong(context.pathParam("id"));

            credentialQuery.delete(profileID)
                    .onComplete(result->{
                       if(result.succeeded())
                       {
                           context.response()
                                   .setStatusCode(200)
                                   .end(ApiResponse.success(200, "Credential profile deleted successfully", null).toJson());
                       }
                       else
                       {
                           if (result.cause().getMessage().contains("Credential profile not found")) {
                               context.response()
                                       .setStatusCode(404)
                                       .end(ApiResponse.error(404, result.cause().getMessage(), null).toJson());
                           } else {
                               context.response()
                                       .setStatusCode(500)
                                       .end(ApiResponse.error(500, "Error deleting credential profile", result.cause().getMessage()).toJson());
                           }
                       }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in deleting credential profile",exception.getCause().getMessage()).toJson());
        }
    }

    //Fetching credential profile
    @Override
    public void get(RoutingContext context)
    {
        try
        {
            var profileID = Long.parseLong(context.pathParam("id"));

            credentialQuery.get(profileID)
                    .onComplete(result->{
                        if(result.succeeded())
                        {
                            Credential credential = result.result();

                            JsonObject response = new JsonObject()
                                    .put("credential.profile.id", credential.profileId())
                                    .put("credential.profile.name", credential.profileName())
                                    .put("credential.profile.protocol", credential.protocol())
                                    .put("user.name", credential.userName())
                                    .put("user.password", credential.password())
                                    .put("community", credential.community())
                                    .put("version", credential.version());

                            context.response()
                                    .setStatusCode(200)
                                    .end(ApiResponse.success(200, "Credential profile fetched successfully", response).toJson());

                        }
                        else
                        {
                            if (result.cause().getMessage().contains("Credential profile not found"))
                            {
                                context.response()
                                        .setStatusCode(404)
                                        .end(ApiResponse.error(404, "Credential profile not found with this ID ", result.cause().getMessage()).toJson());
                            }
                            else
                            {
                                context.response()
                                        .setStatusCode(500)
                                        .end(ApiResponse.error(500, "Error fetching credential profile", result.cause().getMessage()).toJson());
                            }
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in fetching credential profile for given id",exception.getCause().getMessage()).toJson());
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
                            JsonArray response = new JsonArray();

                            List<Credential> credentials = result.result();

                            for(Credential credential : credentials)
                            {
                                response.add(new JsonObject()
                                        .put("credential.profile.id", credential.profileId())
                                        .put("credential.profile.name", credential.profileName())
                                        .put("credential.profile.protocol", credential.protocol())
                                        .put("user.name", credential.userName())
                                        .put("user.password", credential.password())
                                        .put("community", credential.community())
                                        .put("version", credential.version())
                                );
                            }
                            context.response()
                                    .setStatusCode(200)
                                    .end(ApiResponse.success(200, "Fetched credential profiles successfully", response).toJson());
                        }
                        else
                        {
                            context.response()
                                    .setStatusCode(404)
                                    .end(ApiResponse.error(404, "Error in fetching credential profiles", result.cause().getMessage()).toJson());
                        }
                    });
        }
        catch (Exception exception)
        {
            context.response()
                    .setStatusCode(400)
                    .end(ApiResponse.error(400,"Error in fetching credential profiles",exception.getCause().getMessage()).toJson());
        }
    }
}
