/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.DynamicInstantiationNature;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.ArgumentDomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiationResultImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardInstantiationResultBuilder implements InstantiationResultBuilder {
	private final JavaTypeDescriptor<?> javaTypeDescriptor;
	private final List<ResultBuilder> argumentResultBuilders;

	public StandardInstantiationResultBuilder(
			JavaTypeDescriptor<?> javaTypeDescriptor,
			List<ResultBuilder> argumentResultBuilders) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.argumentResultBuilders = argumentResultBuilders;
	}

	@Override
	public DomainResult<?> buildReturn(
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, LegacyFetchBuilder> legacyFetchResolver,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SessionFactoryImplementor sessionFactory) {
		final List<ArgumentDomainResult<?>> argumentDomainResults = new ArrayList<>( argumentResultBuilders.size() );

		argumentResultBuilders.forEach(
				argumentResultBuilder -> argumentDomainResults.add(
						new ArgumentDomainResult<>(
								argumentResultBuilder.buildReturn( jdbcResultsMetadata, legacyFetchResolver, sqlSelectionConsumer, sessionFactory )
						)
				)
		);

		//noinspection unchecked
		return new DynamicInstantiationResultImpl(
				null,
				DynamicInstantiationNature.CLASS,
				javaTypeDescriptor,
				argumentDomainResults
		);
	}
}
