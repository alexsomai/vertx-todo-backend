package io.vertx.todo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/*
 * @author <a href="mailto:somai.alexandru@gmail.com">Alexandru Somai</a>
 */
public class TodoREST extends AbstractVerticle {

    private static final String TODOS = "todos";

    private MongoClient mongo;

    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Launcher.main(new String[]{"run", TodoREST.class.getName()});
    }

    @Override
    public void start(Future<Void> future) {
        mongo = MongoClient.createShared(vertx, config());

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.route().handler((routingContext) -> {
            routingContext.response()
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH")
                    .putHeader("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept")
                    .putHeader("Access-Control-Max-Age", "3600");
            routingContext.next();
        });

        router.get("/").handler((context) -> context.reroute("/todos"));
        router.get("/todos").handler(this::getAllTodos);
        router.get("/todos/:todoId").handler(this::getTodo);
        router.post("/todos").handler(this::addTodo);
        router.patch("/todos/:todoId").handler(this::updateTodo);
        router.delete("/todos/:todoId").handler(this::deleteTodo);
        router.delete("/todos/").handler(this::deleteAllTodos);
        router.options("/todos").handler((handler) -> handler.response().end());
        router.options("/todos/:todoId").handler((handler) -> handler.response().end());

        Integer port = 8080;
        String host = "localhost";

        // needed to be able to run the app on https://openshift.redhat.com
        final String openShiftVertxIP = System.getenv("OPENSHIFT_VERTX_IP");
        if (openShiftVertxIP != null) {
            port = Integer.getInteger("http.port");
            host = System.getProperty("http.address");
        }

        vertx.createHttpServer().requestHandler(router::accept).listen(port, host, result -> {
            if (result.succeeded()) {
                future.complete();
            } else {
                future.fail(result.cause());
            }
        });
    }

    private void getAllTodos(RoutingContext routingContext) {
        mongo.find(TODOS, new JsonObject(), results -> {
            JsonArray arr = new JsonArray();
            results.result().forEach(arr::add);
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(arr.encodePrettily());
        });
    }

    private void getTodo(RoutingContext routingContext) {
        String id = routingContext.request().getParam("todoId");
        if (id == null) {
            sendError(400, routingContext.response());
        } else {
            mongo.findOne(TODOS, new JsonObject().put("_id", id), new JsonObject(), result -> {
                if (result.succeeded()) {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(result.result().encodePrettily());
                }
                if (result.failed()) {
                    sendError(500, routingContext.response());
                }
            });
        }
    }

    private void addTodo(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        JsonObject todo = routingContext.getBodyAsJson();
        if (todo == null) {
            sendError(400, response);
        } else {
            mongo.insert(TODOS, todo, insertResult -> {
                if (insertResult.succeeded()) {
                    String id = insertResult.result();
                    todo.put("_id", id);
                    todo.put("url", routingContext.request().absoluteURI() + "/" + id);
                    todo.put("completed", false);

                    mongo.update(TODOS, new JsonObject().put("_id", id), new JsonObject().put("$set", todo), updateResult -> {
                        if (updateResult.succeeded()) {
                            response.setStatusCode(201)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(todo.encodePrettily());
                        }
                        if (updateResult.failed()) {
                            sendError(500, routingContext.response());
                        }
                    });
                }
                if (insertResult.failed()) {
                    sendError(500, routingContext.response());
                }
            });
        }
    }

    private void updateTodo(RoutingContext routingContext) {
        String id = routingContext.request().getParam("todoId");
        JsonObject jsonObject = routingContext.getBodyAsJson();
        if (id == null || jsonObject == null) {
            sendError(400, routingContext.response());
        } else {
            mongo.update(TODOS,
                    new JsonObject().put("_id", id),
                    new JsonObject().put("$set", jsonObject)
                    , result -> {
                        if (result.succeeded()) {
                            getTodo(routingContext);
                        }
                        if (result.failed()) {
                            sendError(500, routingContext.response());
                        }
                    });
        }
    }

    private void deleteTodo(RoutingContext routingContext) {
        String id = routingContext.request().getParam("todoId");
        if (id == null) {
            sendError(400, routingContext.response());
        } else {
            mongo.removeOne(TODOS, new JsonObject().put("_id", id), result -> {
                if (result.succeeded()) {
                    routingContext.response().end();
                }
                if (result.failed()) {
                    sendError(500, routingContext.response());
                }
            });
        }
    }

    private void deleteAllTodos(RoutingContext routingContext) {
        mongo.dropCollection(TODOS, result -> {
            if (result.succeeded()) {
                routingContext.response().end();
            }
            if (result.failed()) {
                sendError(500, routingContext.response());
            }
        });
    }
    private static void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }
}