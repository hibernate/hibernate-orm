/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.Locale;
import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedSingularAttributeMapping;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * DynamicResultBuilder based on a named mapped attribute
 *
 * @author Steve Ebersole
 */
public class DynamicResultBuilderAttribute implements DynamicResultBuilder {
	private final BasicValuedSingularAttributeMapping attributeMapping;
	private final String columnAlias;
	private final String entityName;
	private final String attributePath;

	public DynamicResultBuilderAttribute(
			SingularAttributeMapping attributeMapping,
			String columnAlias,
			String entityName,
			String attributePath) {
		final boolean allowable = attributeMapping instanceof BasicValuedSingularAttributeMapping;
		if ( !allowable ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Specified attribute [%s.%s] must be basic: %s",
							entityName,
							attributePath,
							attributeMapping
					)
			);
		}

		this.attributeMapping = (BasicValuedSingularAttributeMapping) attributeMapping;
		this.columnAlias = columnAlias;
		this.entityName = entityName;
		this.attributePath = attributePath;
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final int resultSetPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
		final int valuesArrayPosition = resultSetPosition - 1;

		// todo (6.0) : TableGroups + `attributeMapping#buldResult`

		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();
		sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						columnAlias,
						state -> new SqlSelectionImpl( valuesArrayPosition, attributeMapping )
				),
				attributeMapping.getJavaTypeDescriptor(),
				domainResultCreationState.getSqlAstCreationState().getCreationContext().getSessionFactory().getTypeConfiguration()
		);

		return new BasicResult<>(
				valuesArrayPosition,
				columnAlias,
				attributeMapping.getJavaTypeDescriptor(),
				attributeMapping.getValueConverter()
		);
	}
}
