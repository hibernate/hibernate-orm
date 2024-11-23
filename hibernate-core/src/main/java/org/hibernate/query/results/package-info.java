/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
