/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for defining result-set mappings used in {@link org.hibernate.query.NativeQuery}
 * and {@link org.hibernate.procedure.ProcedureCall} / {@link jakarta.persistence.StoredProcedureQuery}.
 * These result-set mappings are used to map the values in the JDBC {@link java.sql.ResultSet}
 * into the query result graph.
 *
 * NOTE : Handling the different sources of results and fetches is split into multiple packages
 * and multiple impls for performance reasons.  The classes in {@link org.hibernate.query.results.complete}
 * represent result/fetch definitions that are completely known up-front and are faster to
 * resolve.  The definitions in {@link org.hibernate.query.results.dynamic} are built incrementally
 * via Hibernate's {@link org.hibernate.query.NativeQuery} contract need to resolve themselves
 * against other dynamic result/fetch definitions and therefore take more resources to resolve.  The
 * classes in {@link org.hibernate.query.results.implicit} represent results that are implied
 *
 * @see org.hibernate.query.results.ResultSetMapping
 *
 * @author Steve Ebersole
 */
package org.hibernate.query.results;
