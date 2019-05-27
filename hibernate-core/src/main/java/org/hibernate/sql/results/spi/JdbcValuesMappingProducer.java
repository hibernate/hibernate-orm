/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.ResultSetMapping;

/**
 * Producer for JdbcValuesMapping references.
 *
 * The split allows resolution of JDBC value metadata to be used in the
 * production of JdbcValuesMapping references.  Generally this feature is
 * used from {@link ResultSetMapping} instances from native-sql queries and
 * procedure-call queries where not all JDBC types are known and we need the
 * JDBC {@link java.sql.ResultSetMetaData} to determine the types
 *
 * @author Steve Ebersole
 */
public interface JdbcValuesMappingProducer {

	/**
	 * Resolve the selections (both at the JDBC and object level) for this
	 * mapping.  Acts as delayed access to this resolution process to support
	 * "auto discovery" as needed for "undefined scalar" results as defined by
	 * native-sql and procedure call queries.
	 */
	JdbcValuesMapping resolve(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory);
}
