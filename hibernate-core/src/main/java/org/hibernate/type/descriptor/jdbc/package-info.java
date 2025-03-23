/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines handling of almost the full range of standard JDBC-defined {@linkplain
 * java.sql.Types SQL data types}. Each JDBC type is described by an implementation
 * of {@link org.hibernate.type.descriptor.jdbc.JdbcType}.
 * <p>
 * See {@linkplain org.hibernate.type this discussion} of the role {@code JdbcType}
 * plays in basic type mappings.
 * <p>
 * We omit certain JDBC types here solely because Hibernate does not use them itself,
 * not due to any inability to provide proper descriptors for them. There are no
 * descriptors for:
 * <ul>
 *     <li>{@link java.sql.Types#DATALINK DATALINK}</li>
 *     <li>{@link java.sql.Types#DISTINCT DISTINCT}</li>
 *     <li>{@link java.sql.Types#REF REF}</li>
 *     <li>{@link java.sql.Types#REF_CURSOR REF_CURSOR}</li>
 * </ul>
 * <p>
 * Nor is there a generic descriptor for {@link java.sql.Types#STRUCT STRUCT} defined
 * in this package, but dialect-specific implementations are provided elsewhere.
 * <p>
 * On the other hand, we actually <em>extend</em> the set of JDBC types by enumerating
 * additional types in {@link org.hibernate.type.SqlTypes}.
 *
 * @see org.hibernate.type.descriptor.jdbc.JdbcType
 * @see org.hibernate.type
 * @see java.sql.Types
 * @see org.hibernate.type.SqlTypes
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jdbc/getstart/mapping.html">Mapping SQL and Java Types</a>
 */
package org.hibernate.type.descriptor.jdbc;
