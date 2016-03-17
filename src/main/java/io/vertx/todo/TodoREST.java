package io.vertx.todo;

import io.vertx.core.AbstractVerticle;

/**
 * @author Alexandru Somai
 *         date 3/15/16
 */
public class TodoREST extends AbstractVerticle {

    @Override
    public void start() {
        vertx.createHttpServer().requestHandler(req -> req.response().end("Hello World!")).listen(8080);
    }
}