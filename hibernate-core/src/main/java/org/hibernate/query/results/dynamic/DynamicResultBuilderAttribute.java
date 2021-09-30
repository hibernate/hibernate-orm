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
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * DynamicResultBuilder based on a named mapped attribute
 *
 * @author Steve Ebersole
 */
public class DynamicResultBuilderAttribute implements DynamicResultBuilder, NativeQuery.ReturnProperty {
	private final BasicAttributeMapping attributeMapping;
	private final String columnAlias;
	private final String entityName;
	private final String attributePath;

	public DynamicResultBuilderAttribute(
			SingularAttributeMapping attributeMapping,
			String columnAlias,
			String entityName,
			String attributePath) {
		final boolean allowable = attributeMapping instanceof BasicAttributeMapping;
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

		this.attributeMapping = (BasicAttributeMapping) attributeMapping;
		this.columnAlias = columnAlias;
		this.entityName = entityName;
		this.attributePath = attributePath;
	}

	@Override
	public Class<?> getJavaType() {
		return attributeMapping.getExpressableJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		// todo (6.0) : TableGroups + `attributeMapping#buldResult`

		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						columnAlias,
						state -> {
							final int resultSetPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
							final int valuesArrayPosition = resultSetPosition - 1;
							return new SqlSelectionImpl( valuesArrayPosition, attributeMapping );
						}
				),
				attributeMapping.getJavaTypeDescriptor(),
				domainResultCreationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnAlias,
				attributeMapping.getJavaTypeDescriptor(),
				attributeMapping.getValueConverter()
		);
	}
}
