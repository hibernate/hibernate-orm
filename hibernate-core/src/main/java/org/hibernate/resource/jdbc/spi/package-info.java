/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * An SPI for managing JDBC connections and other heavyweight resources, based around the
 * idea of a {@linkplain org.hibernate.resource.jdbc.spi.JdbcSessionOwner "JDBC session"}.
 * <p>
 * The interface {@link org.hibernate.resource.jdbc.spi.StatementInspector} is especially
 * useful for monitoring/intercepting SQL statements as they are sent to the database.
 */
package org.hibernate.resource.jdbc.spi;
