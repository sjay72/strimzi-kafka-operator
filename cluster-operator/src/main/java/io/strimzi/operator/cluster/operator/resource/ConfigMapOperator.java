/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.Map;
import java.util.Objects;

/**
 * Operations for {@code ConfigMap}s.
 */
public class ConfigMapOperator extends AbstractResourceOperator<KubernetesClient, ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>> {
    /**
     * Constructor
     * @param vertx The Vertx instance
     * @param client The Kubernetes client
     */
    public ConfigMapOperator(Vertx vertx, KubernetesClient client) {
        super(vertx, client, "ConfigMap");
    }

    @Override
    protected MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>> operation() {
        return client.configMaps();
    }

    @Override
    protected Future<ReconcileResult<ConfigMap>> internalPatch(String namespace, String name, ConfigMap current, ConfigMap desired) {
        try {
            if (compareObjects(current.getData(), desired.getData())
                    && compareObjects(current.getMetadata().getName(), desired.getMetadata().getName())
                    && compareObjects(current.getMetadata().getNamespace(), desired.getMetadata().getNamespace())
                    && compareObjects(current.getMetadata().getAnnotations(), desired.getMetadata().getAnnotations())
                    && compareObjects(current.getMetadata().getLabels(), desired.getMetadata().getLabels())) {
                // Checking some metadata. We cannot check entire metadata object because it contains
                // timestamps which would cause restarting loop
                log.debug("{} {} in namespace {} has not been patched because resources are equal", resourceKind, name, namespace);
                return Future.succeededFuture(ReconcileResult.noop());
            } else {
                return super.internalPatch(namespace, name, current, desired);
            }
        } catch (Exception e) {
            log.error("Caught exception while patching {} {} in namespace {}", resourceKind, name, namespace, e);
            return Future.failedFuture(e);
        }
    }

    private boolean compareObjects(Object a, Object b) {
        /*if (a instanceof Map || b instanceof Map) {
            if (a == null && b != null) {
                if (((HashMap) b).size() == 0) {
                    return true;
                }
            }
            if (b == null && a != null) {
                if (((HashMap) a).size() == 0) {
                    return true;
                }
            }
        }
        return a == null ? b == null : a.equals(b);*/
        if (a == null && b instanceof Map && ((Map) b).size() == 0)
            return true;
        return !(a instanceof Map ^ b instanceof Map) && Objects.equals(a, b);
    }
}
