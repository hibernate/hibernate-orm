/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for defining result-set mappings used in {@link org.hibernate.query.NativeQuery}
 * and {@link org.hibernate.procedure.ProcedureCall} / {@link javax.persistence.StoredProcedureQuery}.
 * These result-set mappings are used to map the values in the JDBC {@link java.sql.ResultSet}
 * into "domain results" (see {@link org.hibernate.sql.results.graph.DomainResult}).
 *
 * @see org.hibernate.query.results.ResultSetMapping
 *
 * @author Steve Ebersole
 */
package org.hibernate.query.results;
