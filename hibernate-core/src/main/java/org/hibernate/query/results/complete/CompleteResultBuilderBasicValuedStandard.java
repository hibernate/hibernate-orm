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
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.query.results.ResultsHelper.impl;

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
		assert explicitType == null || explicitType.getJdbcMapping()
				.getJavaTypeDescriptor()
				.getJavaTypeClass()
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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );
		final SessionFactoryImplementor sessionFactory = creationStateImpl.getSessionFactory();
		final int jdbcPosition;
		final String columnName;
		if ( explicitColumnName != null ) {
			jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( explicitColumnName );
			columnName = explicitColumnName;
		}
		else {
			jdbcPosition = creationStateImpl.getNumberOfProcessedSelections() + 1;
			columnName = jdbcResultsMetadata.resolveColumnName( jdbcPosition );
		}
		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
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
										sessionFactory
								);
							}

							final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
							return new ResultSetMappingSqlSelection( valuesArrayPosition, basicType );
						}
				),
				explicitJavaType,
				null,
				sessionFactory.getTypeConfiguration()
		);

		final JdbcMapping jdbcMapping = sqlSelection.getExpressionType().getSingleJdbcMapping();
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnName,
				jdbcMapping,
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
		CompleteResultBuilderBasicValuedStandard that = (CompleteResultBuilderBasicValuedStandard) o;
		return Objects.equals( explicitColumnName, that.explicitColumnName )
				&& Objects.equals( explicitType, that.explicitType )
				&& Objects.equals( explicitJavaType, that.explicitJavaType );
	}

	@Override
	public int hashCode() {
		return Objects.hash( explicitColumnName, explicitType, explicitJavaType );
	}
}
