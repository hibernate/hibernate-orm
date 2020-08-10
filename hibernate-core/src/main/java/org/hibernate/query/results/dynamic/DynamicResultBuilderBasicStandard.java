/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.function.BiFunction;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Standard DynamicResultBuilder for basic values.
 *
 * @see org.hibernate.query.NativeQuery#addScalar
 *
 * @author Steve Ebersole
 */
public class DynamicResultBuilderBasicStandard implements DynamicResultBuilderBasic {
	private final String columnName;
	private final String resultAlias;

	private final BasicType<?> explicitType;
	private final JavaTypeDescriptor<?> explicitJavaTypeDescriptor;

	public DynamicResultBuilderBasicStandard(String columnName, String resultAlias) {
		assert columnName != null;
		this.columnName = columnName;
		this.resultAlias = resultAlias != null ? resultAlias : columnName;

		this.explicitType = null;
		this.explicitJavaTypeDescriptor = null;
	}

	public DynamicResultBuilderBasicStandard(String columnName, String resultAlias, JavaTypeDescriptor<?> explicitJavaTypeDescriptor) {
		assert columnName != null;
		this.columnName = columnName;
		this.resultAlias = resultAlias != null ? resultAlias : columnName;

		assert explicitJavaTypeDescriptor != null;
		this.explicitJavaTypeDescriptor = explicitJavaTypeDescriptor;

		this.explicitType = null;
	}

	public DynamicResultBuilderBasicStandard(String columnName, String resultAlias, BasicType<?> explicitType) {
		assert columnName != null;
		this.columnName = columnName;
		this.resultAlias = resultAlias != null ? resultAlias : columnName;

		assert explicitType != null;
		this.explicitType = explicitType;

		this.explicitJavaTypeDescriptor = null;
	}

	public String getColumnName() {
		return columnName;
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final SessionFactoryImplementor sessionFactory = domainResultCreationState.getSqlAstCreationState()
				.getCreationContext()
				.getSessionFactory();

		final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnName );
		final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );

		final BasicType<?> basicType;

		if ( explicitType != null ) {
			basicType = explicitType;
		}
		else if ( explicitJavaTypeDescriptor != null ) {
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

			final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( jdbcPosition );

			basicType = typeConfiguration.getBasicTypeRegistry().resolve( explicitJavaTypeDescriptor, sqlTypeDescriptor );
		}
		else {
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

			final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( jdbcPosition );
			final JavaTypeDescriptor<?> javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( typeConfiguration );

			basicType = typeConfiguration.getBasicTypeRegistry().resolve( javaTypeDescriptor, sqlTypeDescriptor );
		}

		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();
		sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						columnName,
						state -> new SqlSelectionImpl( valuesArrayPosition, (BasicValuedMapping) basicType )
				),
				basicType.getJavaTypeDescriptor(),
				sessionFactory.getTypeConfiguration()
		);

		return new BasicResult<>( valuesArrayPosition, resultAlias, explicitJavaTypeDescriptor );
	}

}
