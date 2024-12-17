package org.example.database;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import static org.example.Bootstrap.client;

public class CredentialQuery
{
    //Query for creating credential profile
    public Future<Long> insert(String profile_name, String profile_protocol, String user_name, String user_password, String community, String version)
    {
        Promise<Long> promise = Promise.promise();

        client.preparedQuery("INSERT INTO credentials (profile_name, profile_protocol, user_name, user_password, community, version) " +
                        "VALUES ($1, $2, $3, $4, $5, $6) RETURNING profile_id")
                .execute(Tuple.of(
                        profile_name,
                        profile_protocol,
                        user_name,
                        user_password,
                        community,
                        version
                ), execute -> {
                    if (execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();
                        if (rows.size() > 0)
                        {
                            Long profileId = rows.iterator().next().getLong("profile_id");
                            promise.complete(profileId);
                        }
                        else {
                            promise.fail("Some problem");
                        }
                    }
                    else
                    {
                        var error = execute.cause().getMessage();

                        if(error.contains("credentials_profile_name_key"))
                        {
                            promise.fail("Credential profile name must be unique");
                        }
                        else if(error.contains("check_protocol_fields"))
                        {
                            promise.fail("Please enter necessary details according to your required protocol");
                        }
                        else
                        {
                            promise.fail(error);
                        }
                    }
                });
        return promise.future();
    }

    //Query for fetching all credential profiles
    public Future<JsonArray> getAll()
    {
        Promise<JsonArray> promise = Promise.promise();

        client.query("SELECT * FROM credentials")
                .execute(execute ->{
                    if(execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();

                        var credentials = new JsonArray();

                        for(Row row : rows)
                        {
                            var credential = new JsonObject()
                                    .put("credential.profile.id",row.getLong("profile_id"))
                                    .put("credential.profile.name",row.getString("profile_name"))
                                    .put("credential.profile.protocol",row.getString("profile_protocol"))
                                    .put("user.name",row.getString("user_name"))
                                    .put("user.password",row.getString("user_password"))
                                    .put("community",row.getString("community"))
                                    .put("version",row.getString("version"));

                            credentials.add(credential);
                        }
                        promise.complete(credentials);
                    }
                    else
                    {
                        promise.fail(execute.cause());
                    }
                });

        return promise.future();
    }

    //Query for fetching credential profile
    public Future<JsonObject> get(Long profileID)
    {
        Promise<JsonObject> promise = Promise.promise();

        client.preparedQuery("SELECT * FROM credentials WHERE profileID=$1")
                .execute(Tuple.of(profileID), execute ->{
                   if(execute.succeeded())
                   {
                        RowSet<Row> rows = execute.result();

                        if(rows.size()>0)
                        {
                            Row row = rows.iterator().next();

                            var response = new JsonObject()
                                    .put("credential.profile.id",row.getLong("profileID"))
                                    .put("credential.profile.name",row.getString("profile_name"))
                                    .put("credential.profile.protocol",row.getString("profile_protocol"))
                                    .put("user.name",row.getString("user_name"))
                                    .put("user.password",row.getString("user_password"))
                                    .put("community",row.getString("community"))
                                    .put("version",row.getString("version"));

                            promise.complete(response);
                        }
                        else
                        {
                            promise.fail("Credential profile not found for ID: "+ profileID);
                        }
                   }
                   else
                   {
                       promise.fail(execute.cause());
                   }
                });
        return promise.future();
    }

    //Query to delete credential profile
    public Future<Void> delete(Long profileID)
    {
        Promise<Void> promise = Promise.promise();

        client.preparedQuery("DELETE FROM credentials WHERE profile_id = $1")
                .execute(Tuple.of(profileID), execute ->{
                   if(execute.succeeded())
                   {
                       if(execute.result().rowCount()>0)
                       {
                           promise.complete();
                       }
                       else
                       {
                           promise.fail("Credential profile not found for ID: "+profileID);
                       }
                   }
                   else
                   {
                       promise.fail(execute.cause());
                   }
                });

        return promise.future();
    }

    //Query to update Credential profile
    public Future<Void> update(Long profileID, String profile_name, String profile_protocol, String user_name, String user_password, String community, String version)
    {
        Promise<Void> promise = Promise.promise();

        client.preparedQuery("UPDATE credentials " +
                        "SET profile_name = $1, " +
                        "    profile_protocol = $2, " +
                        "    user_name = $3, " +
                        "    user_password = $4, " +
                        "    community = $5, " +
                        "    version = $6 " +
                        "WHERE profile_id = $7")
                .execute(Tuple.of(
                        profile_name,
                        profile_protocol,
                        user_name,
                        user_password,
                        community,
                        version,
                        profileID
                ), execute -> {
                    if (execute.succeeded())
                    {
                        if (execute.result().rowCount() > 0)
                        {
                            promise.complete(); // Update succeeded
                        }
                        else
                        {
                            promise.fail("Credential profile not found");
                        }
                    }
                    else
                    {
                        var error = execute.cause().getMessage();

                        if(error.contains("credentials_profile_name_key"))
                        {
                            promise.fail("Credential profile name must be unique");
                        }
                        else if(error.contains("check_protocol_fields"))
                        {
                            promise.fail("Please enter necessary details according to your required protocol");
                        }
                        else
                        {
                            promise.fail(error); //Database error
                        }
                    }
                });

        return promise.future();
    }
}
