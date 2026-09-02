/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.spi;

import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.spi.ResultSetMapping;

/**
 * Producer for JdbcValuesMapping references.
 *
 * The split allows resolution of JDBC value metadata to be used in the
 * production of JdbcValuesMapping references.  Generally this feature is
 * used from {@link ResultSetMapping} instances from native-sql queries and
 * procedure-call queries where not all JDBC types are known and we need the
 * JDBC {@link java.sql.ResultSetMetaData} to determine the types
 *
 * @see JdbcValuesMappingProducerProvider#buildMappingProducer(org.hibernate.sql.ast.spi.query.select.SelectStatement, SessionFactoryImplementor)
 * @see JdbcValuesMappingProducerProvider#buildResultSetMapping(String, boolean, SessionFactoryImplementor)
 *
 * @author Steve Ebersole
 */
@Incubating
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface JdbcValuesMappingProducer {
	/**
	 * Resolve the JdbcValuesMapping.  This involves resolving the
	 * {@link org.hibernate.sql.results.graph.DomainResult} and
	 * {@link org.hibernate.sql.results.graph.Fetch}
	 *
	 * @see JdbcValuesMapping
	 */
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory);

	void addAffectedTableNames(Set<String> affectedTableNames, SessionFactoryImplementor sessionFactory);

	default JdbcValuesMappingProducer cacheKeyInstance() {
		return this;
	}
}
