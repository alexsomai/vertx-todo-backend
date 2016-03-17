package io.vertx.todo;

import io.vertx.core.AbstractVerticle;

/**
 * @author Alexandru Somai
 *         date 3/15/16
 */
public class TodoREST extends AbstractVerticle {

    @Override
    public void start() {
        Integer port = 8080;
        String ip = "localhost";

        // needed to be able to run the app on https://openshift.redhat.com
        final String openShiftVertxIP = System.getenv("OPENSHIFT_VERTX_IP");
        if (openShiftVertxIP != null) {
            port = Integer.getInteger("http.port");
            ip = System.getProperty("http.address");
        }
        vertx.createHttpServer().requestHandler(req -> req.response().end("Hello World!")).listen(port, ip);
    }
}