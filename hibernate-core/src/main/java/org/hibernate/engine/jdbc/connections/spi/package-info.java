/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines SPI contracts for obtaining JDBC {@link java.sql.Connection}s from a
 * provider implemented as a {@linkplain org.hibernate.service.Service service}.
 * <p>
 * Typically, the provider is responsible not just for connecting to the database,
 * but also for pooling connections and caching {@link java.sql.PreparedStatement}s.
 * <p>
 * There are two flavors of connection provider:
 * <ul>
 * <li>{@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} is
 *     for general use, and
 * <li>{@link org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider}
 *     for use in a multi-tenant environment.
 * </ul>
 *
 * @see org.hibernate.engine.jdbc.connections.spi.ConnectionProvider
 * @see org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
 */
package org.hibernate.engine.jdbc.connections.spi;
