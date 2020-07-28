/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedSingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class AttributeResultBuilder implements ResultBuilder {
	private final BasicValuedSingularAttributeMapping attributeMapping;
	private final String columnAlias;
	private final String entityName;
	private final String attributePath;

	public AttributeResultBuilder(
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
	public DomainResult<?> buildReturn(
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, LegacyFetchBuilder> legacyFetchResolver,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SessionFactoryImplementor sessionFactory) {
		final int resultSetPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
		final int valuesArrayPosition = resultSetPosition - 1;

		final SqlSelectionImpl sqlSelection = new SqlSelectionImpl( valuesArrayPosition, attributeMapping );
		sqlSelectionConsumer.accept( sqlSelection );

		return new BasicResult<>(
				valuesArrayPosition,
				columnAlias,
				attributeMapping.getJavaTypeDescriptor(),
				attributeMapping.getValueConverter()
		);
	}
}
