/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * An SPI for managing JDBC connections and other heavyweight resources, based around the
 * idea of a {@linkplain org.hibernate.resource.jdbc.spi.JdbcSessionOwner "JDBC session"}.
 * <p>
 * The interface {@link org.hibernate.resource.jdbc.spi.StatementInspector} is especially
 * useful for monitoring/intercepting SQL statements as they are sent to the database.
 */
package org.hibernate.resource.jdbc.spi;
