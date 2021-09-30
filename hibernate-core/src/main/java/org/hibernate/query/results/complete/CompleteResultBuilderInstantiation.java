/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.query.DynamicInstantiationNature;
import org.hibernate.query.results.ResultBuilderInstantiationValued;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.instantiation.internal.ArgumentDomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiationResultImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * ResultBuilder for dynamic instantiation results ({@link jakarta.persistence.ConstructorResult}
 *
 * @author Steve Ebersole
 */
public class CompleteResultBuilderInstantiation
		implements CompleteResultBuilder, ResultBuilderInstantiationValued {

	private final JavaTypeDescriptor<?> javaTypeDescriptor;
	private final List<ResultBuilder> argumentResultBuilders;

	public CompleteResultBuilderInstantiation(
			JavaTypeDescriptor<?> javaTypeDescriptor,
			List<ResultBuilder> argumentResultBuilders) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.argumentResultBuilders = argumentResultBuilders;
	}

	@Override
	public Class<?> getJavaType() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final List<ArgumentDomainResult<?>> argumentDomainResults = new ArrayList<>( argumentResultBuilders.size() );

		for ( int i = 0; i < argumentResultBuilders.size(); i++ ) {
			final ResultBuilder argumentResultBuilder = argumentResultBuilders.get( i );

			@SuppressWarnings({"unchecked", "rawtypes"}) final ArgumentDomainResult<?> argumentDomainResult = new ArgumentDomainResult(
					argumentResultBuilder.buildResult(
							jdbcResultsMetadata,
							i,
							legacyFetchResolver,
							domainResultCreationState
					)
			);

			argumentDomainResults.add( argumentDomainResult );
		}

		return new DynamicInstantiationResultImpl<>(
				null,
				DynamicInstantiationNature.CLASS,
				javaTypeDescriptor,
				argumentDomainResults
		);
	}
}
