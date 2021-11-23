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
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Standard DynamicResultBuilder for basic values.
 *
 * @see org.hibernate.query.NativeQuery#addScalar
 *
 * @author Steve Ebersole
 */
public class DynamicResultBuilderBasicStandard implements DynamicResultBuilderBasic {
	private final String columnName;
	private final int columnPosition;
	private final String resultAlias;

	private final BasicType<?> explicitType;
	private final JavaType<?> explicitJavaTypeDescriptor;

	public DynamicResultBuilderBasicStandard(String columnName, String resultAlias) {
		assert columnName != null;
		this.columnName = columnName;
		this.columnPosition = 0;
		this.resultAlias = resultAlias != null ? resultAlias : columnName;

		this.explicitType = null;
		this.explicitJavaTypeDescriptor = null;
	}

	public DynamicResultBuilderBasicStandard(int columnPosition) {
		assert columnPosition > 0;
		this.columnName = "c" + columnPosition;
		this.columnPosition = columnPosition;
		this.resultAlias = columnName;

		this.explicitType = null;
		this.explicitJavaTypeDescriptor = null;
	}

	public DynamicResultBuilderBasicStandard(String columnName, String resultAlias, JavaType<?> explicitJavaTypeDescriptor) {
		assert columnName != null;
		this.columnName = columnName;
		this.columnPosition = 0;
		this.resultAlias = resultAlias != null ? resultAlias : columnName;

		assert explicitJavaTypeDescriptor != null;
		this.explicitJavaTypeDescriptor = explicitJavaTypeDescriptor;

		this.explicitType = null;
	}

	public DynamicResultBuilderBasicStandard(String columnName, String resultAlias, BasicType<?> explicitType) {
		assert columnName != null;
		this.columnName = columnName;
		this.columnPosition = 0;
		this.resultAlias = resultAlias != null ? resultAlias : columnName;

		assert explicitType != null;
		this.explicitType = explicitType;

		this.explicitJavaTypeDescriptor = null;
	}

	public DynamicResultBuilderBasicStandard(int columnPosition, BasicType<?> explicitType) {
		assert columnPosition > 0;
		this.columnName = "c" + columnPosition;
		this.columnPosition = columnPosition;
		this.resultAlias = columnName;

		assert explicitType != null;
		this.explicitType = explicitType;

		this.explicitJavaTypeDescriptor = null;
	}

	@Override
	public Class<?> getJavaType() {
		if ( explicitJavaTypeDescriptor != null ) {
			return explicitJavaTypeDescriptor.getJavaTypeClass();
		}
		if ( explicitType != null ) {
			return explicitType.getJavaType();
		}
		return null;
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

		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();
		final Expression expression = sqlExpressionResolver.resolveSqlExpression(
				columnName,
				state -> {
					final int jdbcPosition;
					if ( columnPosition > 0 ) {
						jdbcPosition = columnPosition;
					}
					else {
						jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnName );
					}
					final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );

					final BasicType<?> basicType;

					if ( explicitType != null ) {
						basicType = explicitType;
					}
					else {
						basicType = jdbcResultsMetadata.resolveType(
								jdbcPosition,
								explicitJavaTypeDescriptor,
								sessionFactory
						);
					}
					return new SqlSelectionImpl( valuesArrayPosition, (BasicValuedMapping) basicType );
				}
		);

		final JavaType<?> javaTypeDescriptor;

		if ( explicitJavaTypeDescriptor != null ) {
			javaTypeDescriptor = explicitJavaTypeDescriptor;
		}
		else {
			javaTypeDescriptor = expression.getExpressionType().getJdbcMappings().get( 0 ).getMappedJavaTypeDescriptor();
		}
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				expression,
				javaTypeDescriptor,
				sessionFactory.getTypeConfiguration()
		);

		// StandardRowReader expects there to be a JavaTypeDescriptor as part of the ResultAssembler.
		assert javaTypeDescriptor != null;

		return new BasicResult<>( sqlSelection.getValuesArrayPosition(), resultAlias, javaTypeDescriptor );
	}

}
