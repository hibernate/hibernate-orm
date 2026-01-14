/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import jakarta.persistence.AttributeConverter;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.converter.internal.AttributeConverterBean;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.Objects;

import static org.hibernate.query.results.internal.ResultsHelper.impl;
import static org.hibernate.query.results.internal.ResultsHelper.jdbcPositionToValuesArrayPosition;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * ResultBuilder for scalar results defined via:<ul>
 *     <li>JPA {@link jakarta.persistence.ColumnResult}</li>
 *     <li>`&lt;return-scalar/&gt;` as part of a `&lt;resultset/&gt;` stanza in `hbm.xml`</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class CompleteResultBuilderBasicValuedConverted<O,R> implements CompleteResultBuilderBasicValued {
	private final String explicitColumnName;
	private final BasicValuedMapping underlyingMapping;
	private final AttributeConverterBean<O, R> valueConverter;

	public CompleteResultBuilderBasicValuedConverted(
			String explicitColumnName,
			ManagedBean<? extends AttributeConverter<O, R>> converterBean,
			JavaType<? extends AttributeConverter<O, R>> converterJtd,
			BasicJavaType<O> domainJavaType,
			BasicValuedMapping underlyingMapping) {
		this.explicitColumnName = explicitColumnName;
		this.underlyingMapping = underlyingMapping;
		final var relationalType =
				underlyingMapping.getJdbcMapping()
						.getJavaTypeDescriptor();
		this.valueConverter = new AttributeConverterBean<>(
				converterBean,
				converterJtd,
				domainJavaType,
				(JavaType<R>) relationalType
		);
	}

	@Override
	public Class<?> getJavaType() {
		return valueConverter.getDomainJavaType().getJavaTypeClass();
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final var creationStateImpl = impl( domainResultCreationState );
		final String columnName =
				explicitColumnName != null
						? explicitColumnName
						: jdbcResultsMetadata.resolveColumnName(
								creationStateImpl.getNumberOfProcessedSelections() + 1 );
		return new BasicResult<>(
				sqlSelection( jdbcResultsMetadata, resultPosition, creationStateImpl, columnName )
						.getValuesArrayPosition(),
				columnName,
				valueConverter.getDomainJavaType(),
				valueConverter,
				null,
				false,
				false
		);
	}

	private SqlSelection sqlSelection(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationStateImpl creationStateImpl,
			String columnName) {
		return creationStateImpl.resolveSqlSelection(
				creationStateImpl.resolveSqlExpression(
						createColumnReferenceKey( columnName ),
						processingState -> {
							final int jdbcPosition =
									explicitColumnName != null
											? jdbcResultsMetadata.resolveColumnPosition( explicitColumnName )
											: resultPosition + 1;
							final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );
							return new ResultSetMappingSqlSelection( valuesArrayPosition, underlyingMapping );
						}
				),
				valueConverter.getRelationalJavaType(),
				null,
				creationStateImpl.getSessionFactory()
						.getTypeConfiguration()
		);
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof CompleteResultBuilderBasicValuedConverted<?, ?> that ) ) {
			return false;
		}
		else {
			return Objects.equals( explicitColumnName, that.explicitColumnName )
				&& underlyingMapping.equals( that.underlyingMapping )
				&& valueConverter.equals( that.valueConverter );
		}
	}

	@Override
	public int hashCode() {
		int result = explicitColumnName != null ? explicitColumnName.hashCode() : 0;
		result = 31 * result + underlyingMapping.hashCode();
		result = 31 * result + valueConverter.hashCode();
		return result;
	}
}
