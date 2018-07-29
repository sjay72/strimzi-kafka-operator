/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.user;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

/**
 * Cluster Operator configuration
 */
public class UserOperatorConfig {

    public static final String STRIMZI_NAMESPACE = "STRIMZI_NAMESPACE";
    public static final String STRIMZI_FULL_RECONCILIATION_INTERVAL_MS = "STRIMZI_FULL_RECONCILIATION_INTERVAL_MS";
    public static final String STRIMZI_OPERATION_TIMEOUT_MS = "STRIMZI_OPERATION_TIMEOUT_MS";

    public static final long DEFAULT_FULL_RECONCILIATION_INTERVAL_MS = 120_000;
    public static final long DEFAULT_OPERATION_TIMEOUT_MS = 300_000;

    private final String namespace;
    private final long reconciliationIntervalMs;
    private final long operationTimeoutMs;

    /**
     * Constructor
     *
     * @param namespace namespace in which the operator will run and create resources
     * @param reconciliationIntervalMs    specify every how many milliseconds the reconciliation runs
     * @param operationTimeoutMs    timeout for internal operations specified in milliseconds
     */
    public UserOperatorConfig(String namespace, long reconciliationIntervalMs, long operationTimeoutMs) {
        this.namespace = namespace;
        this.reconciliationIntervalMs = reconciliationIntervalMs;
        this.operationTimeoutMs = operationTimeoutMs;
    }

    /**
     * Loads configuration parameters from a related map
     *
     * @param map   map from which loading configuration parameters
     * @return  Cluster Operator configuration instance
     */
    public static UserOperatorConfig fromMap(Map<String, String> map) {

        String namespace = map.get(UserOperatorConfig.STRIMZI_NAMESPACE);
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException(UserOperatorConfig.STRIMZI_NAMESPACE + " cannot be null");
        }

        long reconciliationInterval = DEFAULT_FULL_RECONCILIATION_INTERVAL_MS;
        String reconciliationIntervalEnvVar = map.get(UserOperatorConfig.STRIMZI_FULL_RECONCILIATION_INTERVAL_MS);
        if (reconciliationIntervalEnvVar != null) {
            reconciliationInterval = Long.parseLong(reconciliationIntervalEnvVar);
        }

        long operationTimeout = DEFAULT_OPERATION_TIMEOUT_MS;
        String operationTimeoutEnvVar = map.get(UserOperatorConfig.STRIMZI_OPERATION_TIMEOUT_MS);
        if (operationTimeoutEnvVar != null) {
            operationTimeout = Long.parseLong(operationTimeoutEnvVar);
        }

        return new UserOperatorConfig(namespace, reconciliationInterval, operationTimeout);
    }


    /**
     * @return  namespace in which the operator runs and creates resources
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @return  how many milliseconds the reconciliation runs
     */
    public long getReconciliationIntervalMs() {
        return reconciliationIntervalMs;
    }

    /**
     * @return  how many milliseconds should we wait for Kubernetes operations
     */
    public long getOperationTimeoutMs() {
        return operationTimeoutMs;
    }

    @Override
    public String toString() {
        return "ClusterOperatorConfig(" +
                "namespace=" + namespace +
                ",reconciliationIntervalMs=" + reconciliationIntervalMs +
                ",operationTimeoutMs=" + operationTimeoutMs +
                ")";
    }
}
