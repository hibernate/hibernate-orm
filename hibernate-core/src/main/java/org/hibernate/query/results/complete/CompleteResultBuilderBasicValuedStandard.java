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
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.query.results.ResultsHelper.impl;

/**
 * ResultBuilder for scalar results defined via:<ul>
 *     <li>JPA {@link jakarta.persistence.ColumnResult}</li>
 *     <li>`<return-scalar/>` as part of a `<resultset/>` stanza in `hbm.xml`</li>
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
		return explicitJavaType.getJavaTypeClass();
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
			columnName = jdbcResultsMetadata.resolveColumnName( resultPosition + 1 );
		}

//		final int jdbcPosition;
//		if ( explicitColumnName != null ) {
//			jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( explicitColumnName );
//		}
//		else {
//			jdbcPosition = resultPosition + 1;
//		}
//
//		final BasicValuedMapping basicType;
//		if ( explicitType != null ) {
//			basicType = explicitType;
//		}
//		else {
//			basicType = jdbcResultsMetadata.resolveType( jdbcPosition, explicitJavaType );
//		}
//
//		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
//				creationStateImpl.resolveSqlExpression(
//						columnName,
//						processingState -> {
//							final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
//							return new SqlSelectionImpl( valuesArrayPosition, basicType );
//						}
//				),
//				basicType.getExpressibleJavaType(),
//				sessionFactory.getTypeConfiguration()
//		);


		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
				creationStateImpl.resolveSqlExpression(
						columnName,
						processingState -> {
							final int jdbcPosition;
							if ( explicitColumnName != null ) {
								jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( explicitColumnName );
							}
							else {
								jdbcPosition = resultPosition + 1;
							}

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
				sessionFactory.getTypeConfiguration()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnName,
				sqlSelection.getExpressionType().getJdbcMappings().get( 0 ).getMappedJavaType()
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

		if ( !Objects.equals( explicitColumnName, that.explicitColumnName ) ) {
			return false;
		}
		if ( !Objects.equals( explicitType, that.explicitType ) ) {
			return false;
		}
		return explicitJavaType.equals( that.explicitJavaType );
	}

	@Override
	public int hashCode() {
		int result = explicitColumnName != null ? explicitColumnName.hashCode() : 0;
		result = 31 * result + ( explicitType != null ? explicitType.hashCode() : 0 );
		result = 31 * result + explicitJavaType.hashCode();
		return result;
	}
}
