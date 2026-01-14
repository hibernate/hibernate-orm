/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import jakarta.persistence.sql.ColumnMapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.spi.ResultBuilderBasicValued;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.internal.ResultsHelper.jdbcPositionToValuesArrayPosition;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/// Support for [jakarta.persistence.sql.ColumnMapping].
///
/// @author Steve Ebersole
public class ColumnBuilder<T> extends AbstractMappingElementBuilder<T> implements ResultBuilderBasicValued {
	private final String columnName;

	public ColumnBuilder(ColumnMapping<T> columnMapping, SessionFactoryImplementor sessionFactory) {
		super( columnMapping.getAlias(), columnMapping.getJavaType(), sessionFactory );
		columnName = columnMapping.columnName();
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public BasicResult<T> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState creationState) {
		final var typeConfiguration = sessionFactory.getTypeConfiguration();
		final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnName );
		final var sqlExprResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		var sqlSelection = sqlExprResolver.resolveSqlSelection(
				sqlExprResolver.resolveSqlExpression(
						createColumnReferenceKey( columnName ),
						processingState -> new ResultSetMappingSqlSelection(
								jdbcPositionToValuesArrayPosition( jdbcPosition ),
								(BasicValuedMapping) jdbcResultsMetadata.resolveType( jdbcPosition, javaType, typeConfiguration )
						)
				),
				javaType,
				null,
				typeConfiguration
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnName,
				sqlSelection.getExpressionType().getSingleJdbcMapping(),
				null,
				false,
				false
		);
	}
}
