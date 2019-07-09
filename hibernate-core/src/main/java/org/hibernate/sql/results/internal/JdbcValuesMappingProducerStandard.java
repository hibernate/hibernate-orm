/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.JdbcValuesMapping;
import org.hibernate.sql.results.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.spi.JdbcValuesMetadata;

/**
 * Hibernate's standard ResultSetMappingDescriptor implementation for cases
 * where Hibernate itself creates the mappings.  Basically this covers all
 * scenarios *except* {@link org.hibernate.query.NativeQuery} processing -
 * an important distinction as it means we do not have to perform any
 * {@link java.sql.ResultSetMetaData} resolutions.
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingProducerStandard implements JdbcValuesMappingProducer {
	private final JdbcValuesMapping resolvedMapping;


	public JdbcValuesMappingProducerStandard(Set<SqlSelection> sqlSelections, List<DomainResult> domainResults) {
		resolvedMapping = new StandardJdbcValuesMapping( sqlSelections, domainResults );
	}

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		for ( SqlSelection sqlSelection : resolvedMapping.getSqlSelections() ) {
			sqlSelection.prepare( jdbcResultsMetadata, sessionFactory );
		}

		return resolvedMapping;
	}
}
