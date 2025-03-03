/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Objects;

import static org.hibernate.query.results.internal.ResultsHelper.impl;

/**
 * ResultBuilder for scalar results defined via:<ul>
 *     <li>JPA {@link jakarta.persistence.ColumnResult}</li>
 *     <li>`&lt;return-scalar/&gt;` as part of a `&lt;resultset/&gt;` stanza in `hbm.xml`</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class CompleteResultBuilderBasicValuedStandard implements CompleteResultBuilderBasicValued {

	private final String explicitColumnName;

	private final BasicValuedMapping explicitType;
	private final JavaType<?> explicitJavaType;

	public CompleteResultBuilderBasicValuedStandard(
			String explicitColumnName,
			BasicValuedMapping explicitType,
			JavaType<?> explicitJavaType) {
		//noinspection unchecked
		assert explicitType == null
			|| explicitType.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass()
				.isAssignableFrom( explicitJavaType.getJavaTypeClass() );

		this.explicitColumnName = explicitColumnName;
		this.explicitType = explicitType;
		this.explicitJavaType = explicitJavaType;
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public Class<?> getJavaType() {
		return explicitJavaType == null ? null : explicitJavaType.getJavaTypeClass();
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );
		final int jdbcPosition =
				explicitColumnName != null
						? jdbcResultsMetadata.resolveColumnPosition( explicitColumnName )
						: creationStateImpl.getNumberOfProcessedSelections() + 1;
		final String columnName =
				explicitColumnName != null
						? explicitColumnName
						: jdbcResultsMetadata.resolveColumnName( jdbcPosition );
		final SqlSelection sqlSelection =
				sqlSelection( jdbcResultsMetadata, creationStateImpl, columnName, jdbcPosition );
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnName,
				sqlSelection.getExpressionType().getSingleJdbcMapping(),
				null,
				false,
				false
		);
	}

	private SqlSelection sqlSelection(
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationStateImpl creationStateImpl,
			String columnName, int jdbcPosition) {
		final TypeConfiguration typeConfiguration = creationStateImpl.getCreationContext().getTypeConfiguration();
		return creationStateImpl.resolveSqlSelection(
				creationStateImpl.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( columnName ),
						processingState -> {
							final BasicValuedMapping basicType;
							if ( explicitType != null ) {
								basicType = explicitType;
							}
							else {
								basicType = jdbcResultsMetadata.resolveType(
										jdbcPosition,
										explicitJavaType,
										typeConfiguration
								);
							}
							final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
							return new ResultSetMappingSqlSelection( valuesArrayPosition, basicType );
						}
				),
				explicitJavaType,
				null,
				typeConfiguration
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final CompleteResultBuilderBasicValuedStandard that = (CompleteResultBuilderBasicValuedStandard) o;
		return Objects.equals( explicitColumnName, that.explicitColumnName )
			&& Objects.equals( explicitType, that.explicitType )
			&& Objects.equals( explicitJavaType, that.explicitJavaType );
}

	@Override
	public int hashCode() {
		return Objects.hash( explicitColumnName, explicitType, explicitJavaType );
	}
}
