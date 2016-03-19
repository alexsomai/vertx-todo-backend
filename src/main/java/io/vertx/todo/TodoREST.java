package io.vertx.todo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.*;

/*
 * @author <a href="mailto:somai.alexandru@gmail.com">Alexandru Somai</a>
 */
public class TodoREST extends AbstractVerticle {

    private Map<String, JsonObject> todos = new HashMap<>();

    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        args = Arrays.copyOf(args, args.length + 2);
        args[args.length - 2] = "run";
        args[args.length - 1] = "io.vertx.todo.TodoREST";
        Launcher.main(args);
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        // CORS filters needed by todo-backend specs
        router.route().handler((routingContext) -> {
            routingContext.response()
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH")
                    .putHeader("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept")
                    .putHeader("Access-Control-Max-Age", "3600");
            routingContext.next();
        });

        // URI path mappings
        router.get("/").handler((context) -> context.reroute("/todos"));
        router.get("/todos").handler(this::handleListTodos);
        router.get("/todos/:todoID").handler(this::handleGetTodo);
        router.post("/todos").handler(this::handleAddTodo);
        router.patch("/todos/:todoID").handler(this::handleEditTodo);
        router.delete("/todos/:todoID").handler(this::handleDeleteTodo);
        router.delete("/todos/").handler(this::handleDeleteAllTodos);
        router.options("/todos").handler((handler) -> handler.response().end());
        router.options("/todos/:todoID").handler((handler) -> handler.response().end());

        Integer port = 8080;
        String ip = "localhost";

        // needed to be able to run the app on https://openshift.redhat.com
        final String openShiftVertxIP = System.getenv("OPENSHIFT_VERTX_IP");
        if (openShiftVertxIP != null) {
            port = Integer.getInteger("http.port");
            ip = System.getProperty("http.address");
        }

        vertx.createHttpServer().requestHandler(router::accept).listen(port, ip);
    }

    private void handleListTodos(RoutingContext routingContext) {
        JsonArray arr = new JsonArray();
        todos.forEach((k, v) -> arr.add(v));
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(arr.encodePrettily());
    }

    private void handleGetTodo(RoutingContext rc) {
        Optional<JsonObject> todoOpt = findOne(rc);

        todoOpt.ifPresent(todo -> rc.response()
                .putHeader("content-type", "application/json")
                .end(todo.encodePrettily()));
    }

    private void handleAddTodo(RoutingContext rc) {
        HttpServerResponse response = rc.response();
        JsonObject todo = rc.getBodyAsJson();
        if (todo == null) {
            sendError(400, response);
        } else {
            String todoID = UUID.randomUUID().toString();
            todo.put("completed", false);
            todo.put("url", rc.request().absoluteURI() + "/" + todoID);
            todos.put(todoID, todo);

            response.setStatusCode(201).end(todo.encodePrettily());
        }
    }

    private void handleEditTodo(RoutingContext rc) {
        Optional<JsonObject> todoOpt = findOne(rc);
        JsonObject requestedTodoObject = rc.getBodyAsJson();

        todoOpt.ifPresent(todo -> {
            todo = todo.mergeIn(requestedTodoObject);
            rc.response()
                    .putHeader("content-type", "application/json")
                    .end(todo.encodePrettily());
        });
    }

    private void handleDeleteTodo(RoutingContext rc) {
        String todoID = rc.request().getParam("todoID");
        Optional<JsonObject> todoOpt = findOne(todoID, rc);

        todoOpt.ifPresent(todo -> {
            todos.remove(todoID);
            rc.response().end();
        });
    }

    private void handleDeleteAllTodos(RoutingContext rc) {
        todos.clear();
        rc.response().end();
    }

    private Optional<JsonObject> findOne(RoutingContext rc) {
        return findOne(rc.request().getParam("todoID"), rc);
    }

    private Optional<JsonObject> findOne(String todoID, RoutingContext rc) {
        HttpServerResponse response = rc.response();
        if (todoID == null) {
            sendError(400, response);
        } else {
            JsonObject todo = todos.get(todoID);
            if (todo == null) {
                sendError(404, response);
            } else {
                return Optional.of(todo);
            }
        }
        return Optional.empty();
    }

    private static void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }
}