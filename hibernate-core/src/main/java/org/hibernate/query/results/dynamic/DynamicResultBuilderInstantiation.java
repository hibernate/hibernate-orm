/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.query.DynamicInstantiationNature;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.Builders;
import org.hibernate.query.results.ResultBuilderInstantiationValued;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.instantiation.internal.ArgumentDomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiationResultImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DynamicResultBuilderInstantiation<J>
		implements DynamicResultBuilder, ResultBuilderInstantiationValued, NativeQuery.InstantiationResultNode<J> {

	private static class InstantiationArgument {
		private final DynamicResultBuilder argumentBuilder;
		private final String resultAlias;

		public InstantiationArgument(DynamicResultBuilder argumentBuilder, String resultAlias) {
			this.argumentBuilder = argumentBuilder;
			this.resultAlias = resultAlias;
		}
	}

	private final JavaTypeDescriptor<J> javaTypeDescriptor;
	private final List<InstantiationArgument> argumentResultBuilders = new ArrayList<>();

	public DynamicResultBuilderInstantiation(JavaTypeDescriptor<J> javaTypeDescriptor) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public Class<?> getJavaType() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	@Override
	public NativeQuery.InstantiationResultNode<J> addBasicArgument(String columnAlias, String argumentAlias) {
		argumentResultBuilders.add(
				new InstantiationArgument( Builders.scalar( columnAlias ), argumentAlias )
		);
		return this;
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		if ( argumentResultBuilders.isEmpty() ) {
			throw new IllegalStateException( "DynamicResultBuilderInstantiation defined no arguments" );
		}

		final List<ArgumentDomainResult<?>> argumentDomainResults = new ArrayList<>( argumentResultBuilders.size() );

		for ( int i = 0; i < argumentResultBuilders.size(); i++ ) {
			final InstantiationArgument argument = argumentResultBuilders.get( i );

			final ArgumentDomainResult<?> argumentDomainResult = new ArgumentDomainResult(
					argument.argumentBuilder.buildResult(
							jdbcResultsMetadata,
							i,
							legacyFetchResolver,
							domainResultCreationState
					)
			);

			argumentDomainResults.add( argumentDomainResult );
		}

		return new DynamicInstantiationResultImpl(
				null,
				DynamicInstantiationNature.CLASS,
				javaTypeDescriptor,
				argumentDomainResults
		);
	}
}
