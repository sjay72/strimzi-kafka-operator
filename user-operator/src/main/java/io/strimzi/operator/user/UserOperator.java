/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.user;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.strimzi.operator.cluster.operator.assembly.AbstractAssemblyOperator;
import io.strimzi.operator.cluster.operator.assembly.KafkaAssemblyOperator;
import io.strimzi.operator.cluster.operator.assembly.KafkaConnectAssemblyOperator;
import io.strimzi.operator.cluster.operator.assembly.KafkaConnectS2IAssemblyOperator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An "operator" for managing assemblies of various types <em>in a particular namespace</em>.
 */
public class UserOperator extends AbstractVerticle {

    private static final Logger log = LogManager.getLogger(UserOperator.class.getName());

    public static final String STRIMZI_CLUSTER_OPERATOR_DOMAIN = "user.operator.strimzi.io";

    private static final int HEALTH_SERVER_PORT = 8080;

    private final KubernetesClient client;
    private final String namespace;
    private final long reconciliationInterval;

    private final Map<String, Watch> watchByKind = new ConcurrentHashMap();

    private long reconcileTimer;
    private final KafkaUserOperator kafkaUSerOperator;

    public UserOperator(String namespace,
                        long reconciliationInterval,
                        KubernetesClient client,
                        KafkaUserOperator kafkaUserOperator) {
        log.info("Creating UserOperator for namespace {}", namespace);
        this.namespace = namespace;
        this.reconciliationInterval = reconciliationInterval;
        this.client = client;
        this.kafkaUSerOperator = kafkaUSerOperator;
    }

    Consumer<KubernetesClientException> recreateWatch(AbstractAssemblyOperator op) {
        Consumer<KubernetesClientException> cons = new Consumer<KubernetesClientException>() {
            @Override
            public void accept(KubernetesClientException e) {
                if (e != null) {
                    log.error("Watcher closed with exception in namespace {}", namespace, e);
                    op.createWatch(namespace, this);
                } else {
                    log.info("Watcher closed in namespace {}", namespace);
                }
            }
        };
        return cons;
    }


    @Override
    public void start(Future<Void> start) {
        log.info("Starting UserOperator for namespace {}", namespace);

        // Configure the executor here, but it is used only in other places
        getVertx().createSharedWorkerExecutor("kubernetes-ops-pool", 10, TimeUnit.SECONDS.toNanos(120));

        kafkaAssemblyOperator.createWatch(namespace, recreateWatch(kafkaAssemblyOperator))
            .compose(w -> {
                log.info("Started operator for {} kind", "Kafka");
                watchByKind.put("Kafka", w);
                return kafkaConnectAssemblyOperator.createWatch(namespace, recreateWatch(kafkaConnectAssemblyOperator));
            })
            .compose(w -> {
                log.info("Started operator for {} kind", "KafkaConnect");
                watchByKind.put("KafkaConnect", w);
                if (kafkaConnectS2IAssemblyOperator != null) {
                    // only on OS
                    return kafkaConnectS2IAssemblyOperator.createWatch(namespace, recreateWatch(kafkaConnectS2IAssemblyOperator));
                } else {
                    return Future.succeededFuture(null);
                }
            }).compose(w -> {
                if (w != null) {
                    log.info("Started operator for {} kind", "KafkaConnectS2I");
                    watchByKind.put("KafkaS2IConnect", w);
                }
                log.info("Setting up periodical reconciliation for namespace {}", namespace);
                this.reconcileTimer = vertx.setPeriodic(this.reconciliationInterval, res2 -> {
                    log.info("Triggering periodic reconciliation for namespace {}...", namespace);
                    reconcileAll("timer");
                });
                return startHealthServer().map((Void) null);
            }).compose(start::complete, start);
    }


    @Override
    public void stop(Future<Void> stop) {
        log.info("Stopping UserOperator for namespace {}", namespace);
        vertx.cancelTimer(reconcileTimer);
        for (Watch watch : watchByKind.values()) {
            if (watch != null) {
                watch.close();
            }
            // TODO remove the watch from the watchByKind
        }
        client.close();

        stop.complete();
    }

    /**
      Periodical reconciliation (in case we lost some event)
     */
    private void reconcileAll(String trigger) {
        kafkaAssemblyOperator.reconcileAll(trigger, namespace);
        kafkaConnectAssemblyOperator.reconcileAll(trigger, namespace);

        if (kafkaConnectS2IAssemblyOperator != null) {
            kafkaConnectS2IAssemblyOperator.reconcileAll(trigger, namespace);
        }
    }

    /**
     * Start an HTTP health server
     */
    private Future<HttpServer> startHealthServer() {
        Future<HttpServer> result = Future.future();
        this.vertx.createHttpServer()
                .requestHandler(request -> {

                    if (request.path().equals("/healthy")) {
                        request.response().setStatusCode(200).end();
                    } else if (request.path().equals("/ready")) {
                        request.response().setStatusCode(200).end();
                    }
                })
                .listen(HEALTH_SERVER_PORT, ar -> {
                    if (ar.succeeded()) {
                        log.info("UserOperator is now ready (health server listening on {})", HEALTH_SERVER_PORT);
                    } else {
                        log.error("Unable to bind health server on {}", HEALTH_SERVER_PORT, ar.cause());
                    }
                    result.handle(ar);
                });
        return result;
    }

}
