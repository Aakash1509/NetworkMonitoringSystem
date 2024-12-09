package org.example.database;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.example.model.Credential;

import java.util.ArrayList;
import java.util.List;

import static org.example.Bootstrap.client;

public class CredentialQuery
{
    //Query for creating credential profile
    public Future<Long> insert(Credential credential)
    {
        Promise<Long> promise = Promise.promise();

        String sql = "INSERT INTO credentials (profile_name, profile_protocol, user_name, user_password, community, version) " +
                "VALUES ($1, $2, $3, $4, $5, $6) RETURNING profile_id";

        client.preparedQuery(sql)
                .execute(Tuple.of(
                        credential.profileName(),
                        credential.protocol(),
                        credential.userName(),
                        credential.password(),
                        credential.community(),
                        credential.version()
                ), ar -> {
                    if (ar.succeeded())
                    {
                        RowSet<Row> rows = ar.result();
                        if (rows.size() > 0)
                        {
                            Long profileId = rows.iterator().next().getLong("profile_id");
                            promise.complete(profileId);
                        }
                        else {
                            promise.fail("Some problem");
                        }
                    }
                    else {
                        promise.fail(ar.cause());
                    }
                });
        return promise.future();
    }

    //Query for fetching all credential profiles
    public Future<List<Credential>> getAll()
    {
        Promise<List<Credential>> promise = Promise.promise();

        client.query("SELECT * FROM credentials")
                .execute(result->{
                    if(result.succeeded())
                    {
                        RowSet<Row> rows = result.result();

                        List<Credential> credentials = new ArrayList<>();

                        for(Row row : rows)
                        {
                            Credential credential = new Credential(
                                    row.getLong("profile_id"),
                                    row.getString("profile_name"),
                                    row.getString("profile_protocol"),
                                    row.getString("user_name"),
                                    row.getString("user_password"),
                                    row.getString("community"),
                                    row.getString("version")
                            );
                            credentials.add(credential);
                        }
                        promise.complete(credentials);
                    }
                    else
                    {
                        promise.fail(result.cause());
                    }
                });

        return promise.future();
    }

    //Query for fetching credential profile
    public Future<Credential> get(Long profile_id)
    {
        Promise<Credential> promise = Promise.promise();

        String sql = "SELECT * FROM credentials WHERE profile_id=$1";

        client.preparedQuery(sql)
                .execute(Tuple.of(profile_id),result->{
                   if(result.succeeded())
                   {
                        RowSet<Row> rows = result.result();

                        if(rows.size()>0)
                        {
                            Row row = rows.iterator().next();

                            Credential credential = new Credential(
                                    row.getLong("profile_id"),
                                    row.getString("profile_name"),
                                    row.getString("profile_protocol"),
                                    row.getString("user_name"),
                                    row.getString("user_password"),
                                    row.getString("community"),
                                    row.getString("version")
                            );
                            promise.complete(credential);
                        }
                        else
                        {
                            promise.fail("Credential profile not found for ID: "+profile_id);
                        }
                   }
                   else
                   {
                       promise.fail(result.cause());
                   }
                });
        return promise.future();
    }

    //Query to delete credential profile
    public Future<Void> delete(Long profile_id)
    {
        Promise<Void> promise = Promise.promise();

        String sql = "DELETE FROM credentials WHERE profile_id = $1";

        client.preparedQuery(sql)
                .execute(Tuple.of(profile_id),result->{
                   if(result.succeeded())
                   {
                       if(result.result().rowCount()>0)
                       {
                           promise.complete();
                       }
                       else
                       {
                           promise.fail("Credential profile not found for ID: "+profile_id);
                       }
                   }
                   else
                   {
                       promise.fail(result.cause());
                   }
                });

        return promise.future();
    }

    //Query to update Credential profile
    public Future<Void> update(Credential credential)
    {
        Promise<Void> promise = Promise.promise();

        String sql = "UPDATE credentials " +
                "SET profile_name = $1, " +
                "    profile_protocol = $2, " +
                "    user_name = $3, " +
                "    user_password = $4, " +
                "    community = $5, " +
                "    version = $6 " +
                "WHERE profile_id = $7";

        client.preparedQuery(sql)
                .execute(Tuple.of(
                        credential.profileName(),
                        credential.protocol(),
                        credential.userName(),
                        credential.password(),
                        credential.community(),
                        credential.version(),
                        credential.profileId()
                ), ar -> {
                    if (ar.succeeded())
                    {
                        if (ar.result().rowCount() > 0)
                        {
                            promise.complete(); // Update succeeded
                        }
                        else
                        {
                            promise.fail("Credential profile not found for ID: " + credential.profileId());
                        }
                    }
                    else
                    {
                        promise.fail(ar.cause()); // Database error
                    }
                });

        return promise.future();
    }
}
