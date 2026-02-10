/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.NativeQuery;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * Hibernate's standard ResultSetMappingDescriptor implementation for cases
 * where Hibernate itself creates the mappings.  Basically this covers all
 * scenarios *except* {@link NativeQuery} processing -
 * an important distinction as it means we do not have to perform any
 * {@link java.sql.ResultSetMetaData} resolutions.
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingProducerStandard implements JdbcValuesMappingProducer {

	private final StandardJdbcValuesMapping resolvedMapping;

	public JdbcValuesMappingProducerStandard(List<SqlSelection> sqlSelections, List<DomainResult<?>> domainResults) {
		this.resolvedMapping = new StandardJdbcValuesMapping( sqlSelections, domainResults );
	}

	@Override
	public void addAffectedTableNames(Set<String> affectedTableNames, SessionFactoryImplementor sessionFactory) {

	}

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {
		if ( !resolvedMapping.needsResolve() ) {
			return resolvedMapping;
		}
		final List<SqlSelection> sqlSelections = resolvedMapping.getSqlSelections();
		List<SqlSelection> resolvedSelections = null;
		for ( int i = 0; i < sqlSelections.size(); i++ ) {
			final var sqlSelection = sqlSelections.get( i );
			final var resolvedSelection = sqlSelection.resolve( jdbcResultsMetadata, sessionFactory );
			if ( resolvedSelection != sqlSelection ) {
				if ( resolvedSelections == null ) {
					resolvedSelections = new ArrayList<>( sqlSelections );
				}
				resolvedSelections.set( i, resolvedSelection );
			}
		}
		if ( resolvedSelections == null ) {
			return resolvedMapping;
		}
		return new StandardJdbcValuesMapping(
				resolvedSelections,
				resolvedMapping.getDomainResults()
		);
	}
}
