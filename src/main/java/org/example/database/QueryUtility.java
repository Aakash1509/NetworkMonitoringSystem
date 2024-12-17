package org.example.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

import static org.example.Bootstrap.client;

public class QueryUtility
{

    public Future<Long> insert(String tableName, JsonObject data)
    {
        Promise<Long> promise = Promise.promise();

        var columns = new StringBuilder();

        var placeholders = new StringBuilder();

        List<Object> values = new ArrayList<>();

        data.forEach(entry -> {
            columns.append(entry.getKey()).append(", ");

            placeholders.append("$").append(values.size() + 1).append(", ");

            values.add(entry.getValue());
        });

        columns.setLength(columns.length() - 2);

        placeholders.setLength(placeholders.length() - 2);

        client.preparedQuery("INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ") RETURNING *") //* means every column
                .execute(Tuple.from(values), execute -> {
                    if (execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();
                        if (rows.size() > 0)
                        {
                            var id = rows.iterator().next().getLong(0);

                            promise.complete(id);
                        }
                        else
                        {
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
                        else if(error.contains("unique_name"))
                        {
                            promise.fail("Discovery name must be unique");
                        }
                        else
                        {
                            promise.fail(error);
                        }
                    }
                });
        return promise.future();
    }

    public Future<JsonObject> get(String tableName, String column, Long id)
    {
        Promise<JsonObject> promise = Promise.promise();

        client.preparedQuery("SELECT * FROM " + tableName + " WHERE " + column + " = $1")
                .execute(Tuple.of(id),execute->{
                    if(execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();

                        if (rows.iterator().hasNext()) {
                            Row row = rows.iterator().next();

                            // Convert the Row into a JsonObject
                            var response = new JsonObject();

                            for (int i = 0; i < row.size(); i++)
                            {
                                var fieldName = row.getColumnName(i);

                                var value = row.getValue(i);

                                response.put(fieldName, value);
                            }
                            promise.complete(response);
                        }
                        else
                        {
                            promise.fail("Information not found");
                        }
                    }
                    else
                    {
                        promise.fail(execute.cause());
                    }
                });

        return promise.future();
    }

    public Future<Void> delete(String tableName, String column, Long id)
    {
        Promise<Void> promise = Promise.promise();

        client.preparedQuery("DELETE FROM "+ tableName + " WHERE " + column + " = $1")
                .execute(Tuple.of(id), execute ->{
                    if(execute.succeeded())
                    {
                        if(execute.result().rowCount()>0)
                        {
                            promise.complete();
                        }
                        else
                        {
                            promise.fail("Information not found");
                        }
                    }
                    else
                    {
                        promise.fail(execute.cause());
                    }
                });

        return promise.future();
    }

    public Future<JsonArray> getAll(String tableName)
    {
        Promise<JsonArray> promise = Promise.promise();


        client.preparedQuery("SELECT * FROM "+ tableName)
                .execute(execute->{
                    if(execute.succeeded())
                    {
                        RowSet<Row> rows = execute.result();

                        var response = new JsonArray();

                        for(Row row : rows)
                        {
                            var json = new JsonObject();

                            for(int i=0;i<row.size();i++)
                            {
                                var columnName = row.getColumnName(i);

                                var value = row.getValue(i);

                                json.put(columnName, value);
                            }
                            response.add(json);
                        }
                        promise.complete(response);

                    }
                    else
                    {
                        promise.fail(execute.cause());
                    }
                });
        return promise.future();
    }

    public Future<Void> update(String tableName, JsonObject data)
    {
        Promise<Void> promise= Promise.promise();

        var setClause = new StringBuilder();

        var whereClause = new StringBuilder();

        var values = new ArrayList<>();

        var keys = new ArrayList<>(data.fieldNames()); //To get last key for where clause

        String lastKey = keys.get(keys.size() - 1);

        var value = data.getValue(lastKey);

        data.forEach(entry -> {
            if (!setClause.isEmpty())
            {
                setClause.append(", ");
            }

            setClause.append(entry.getKey()).append(" = $").append(values.size() + 1);

            values.add(entry.getValue());

        });

        whereClause.append(lastKey).append(" = $").append(values.size() + 1);

        values.add(value);

        client.preparedQuery("UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause)
                .execute(Tuple.from(values),execute->{
                   if(execute.succeeded())
                   {
                       if (execute.result().rowCount() > 0)
                       {
                           promise.complete(); // Update succeeded
                       }
                       else
                       {
                           promise.fail("Information not found");
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
                       else if(error.contains("unique_name"))
                       {
                           promise.fail("Discovery name must be unique");
                       }
                       else
                       {
                           promise.fail(error);
                       }
                   }
                });

        return promise.future();
    }
}
