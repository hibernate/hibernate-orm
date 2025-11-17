/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Support for defining {@linkplain org.hibernate.query.results.ResultSetMapping result set mappings}
 * used in {@link org.hibernate.query.NativeQuery}, {@link org.hibernate.procedure.ProcedureCall},
 * and {@link jakarta.persistence.StoredProcedureQuery}.  These result set mappings are used to map
 * the values in the JDBC {@link java.sql.ResultSet} into the query result graph.
 *
 * @see org.hibernate.query.results.ResultSetMapping
 *
 * @author Steve Ebersole
 */
package org.hibernate.query.results;
