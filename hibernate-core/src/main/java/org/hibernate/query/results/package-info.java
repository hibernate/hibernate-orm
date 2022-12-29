/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for defining result set mappings used in {@link org.hibernate.query.NativeQuery},
 * {@link org.hibernate.procedure.ProcedureCall}, and {@link jakarta.persistence.StoredProcedureQuery}.
 * These result set mappings are used to map the values in the JDBC {@link java.sql.ResultSet} into
 * the query result graph.
 * <p>
 * Subpackages handle different sources of result set mappings:
 * <ul>
 * <li>{@link org.hibernate.query.results.complete} handles result set mappings which are completely
 *     known upfront and are faster to resolve.
 * <li>{@link org.hibernate.query.results.dynamic} handles result set mappings which are defined
 *     incrementally via the {@link org.hibernate.query.NativeQuery} interface and need to resolve
 *     themselves against other dynamic mappings. These take more resources to resolve.
 * <li>{@link org.hibernate.query.results.implicit} handles implicit result set mappings.
 * </ul>
 *
 * @see org.hibernate.query.results.ResultSetMapping
 *
 * @author Steve Ebersole
 */
package org.hibernate.query.results;
