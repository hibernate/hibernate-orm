/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.Objects;
import java.util.function.BiFunction;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.type.descriptor.converter.internal.JpaAttributeConverterImpl;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.query.results.ResultsHelper.impl;

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
	private final JpaAttributeConverterImpl<O, R> valueConverter;

	public CompleteResultBuilderBasicValuedConverted(
			String explicitColumnName,
			ManagedBean<? extends AttributeConverter<O, R>> converterBean,
			JavaType<? extends AttributeConverter<O, R>> converterJtd,
			BasicJavaType<O> domainJavaType,
			BasicValuedMapping underlyingMapping) {
		this.explicitColumnName = explicitColumnName;
		this.underlyingMapping = underlyingMapping;
		this.valueConverter = new JpaAttributeConverterImpl<>(
				converterBean,
				converterJtd,
				domainJavaType,
				underlyingMapping.getJdbcMapping().getJavaTypeDescriptor()
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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );
		final SessionFactoryImplementor sessionFactory = creationStateImpl.getSessionFactory();

		final String columnName;
		if ( explicitColumnName != null ) {
			columnName = explicitColumnName;
		}
		else {
			columnName = jdbcResultsMetadata.resolveColumnName( creationStateImpl.getNumberOfProcessedSelections() + 1 );
		}

		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
				creationStateImpl.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( columnName ),
						processingState -> {
							final int jdbcPosition;
							if ( explicitColumnName != null ) {
								jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( explicitColumnName );
							}
							else {
								jdbcPosition = resultPosition + 1;
							}

							final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
							return new ResultSetMappingSqlSelection( valuesArrayPosition, underlyingMapping );
						}
				),
				valueConverter.getRelationalJavaType(),
				null,
				sessionFactory.getTypeConfiguration()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnName,
				valueConverter.getDomainJavaType(),
				valueConverter,
				null,
				false,
				false
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

		CompleteResultBuilderBasicValuedConverted<?, ?> that = (CompleteResultBuilderBasicValuedConverted<?, ?>) o;

		if ( !Objects.equals( explicitColumnName, that.explicitColumnName ) ) {
			return false;
		}
		if ( !underlyingMapping.equals( that.underlyingMapping ) ) {
			return false;
		}
		return valueConverter.equals( that.valueConverter );
	}

	@Override
	public int hashCode() {
		int result = explicitColumnName != null ? explicitColumnName.hashCode() : 0;
		result = 31 * result + underlyingMapping.hashCode();
		result = 31 * result + valueConverter.hashCode();
		return result;
	}
}
