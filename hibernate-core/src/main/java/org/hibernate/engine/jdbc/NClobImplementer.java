/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

/**
 * Marker interface for non-contextually created {@link java.sql.NClob} instances.
 * <p>
 * {@link java.sql.NClob} is a new type introduced in JDK 1.6 (JDBC 4)
 *
 * @author Steve Ebersole
 */
public interface NClobImplementer extends ClobImplementer {
}
