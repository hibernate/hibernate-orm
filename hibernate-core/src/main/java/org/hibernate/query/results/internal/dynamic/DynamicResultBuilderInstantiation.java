/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.internal.Builders;
import org.hibernate.query.results.ResultBuilderInstantiationValued;
import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.instantiation.internal.ArgumentDomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiationResultImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.ArrayList;
import java.util.List;

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

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final var that = (InstantiationArgument) o;
			return argumentBuilder.equals( that.argumentBuilder )
				&&  resultAlias.equals( that.resultAlias );
		}

		@Override
		public int hashCode() {
			int result = argumentBuilder.hashCode();
			result = 31 * result + resultAlias.hashCode();
			return result;
		}
	}

	private final JavaType<J> javaType;
	private final List<InstantiationArgument> argumentResultBuilders;

	public DynamicResultBuilderInstantiation(JavaType<J> javaType) {
		this.javaType = javaType;
		this.argumentResultBuilders = new ArrayList<>();
	}

	private DynamicResultBuilderInstantiation(DynamicResultBuilderInstantiation<J> original) {
		this.javaType = original.javaType;
		final List<InstantiationArgument> arguments = new ArrayList<>( original.argumentResultBuilders.size() );
		for ( var arg : original.argumentResultBuilders ) {
			arguments.add( new InstantiationArgument( arg.argumentBuilder.cacheKeyInstance(), arg.resultAlias ) );
		}
		this.argumentResultBuilders = arguments;
	}

	@Override
	public Class<?> getJavaType() {
		return javaType.getJavaTypeClass();
	}

	@Override
	public NativeQuery.InstantiationResultNode<J> addBasicArgument(String columnAlias, String argumentAlias) {
		argumentResultBuilders.add( new InstantiationArgument( Builders.scalar( columnAlias ), argumentAlias ) );
		return this;
	}

	@Override
	public DynamicResultBuilderInstantiation<?> cacheKeyInstance() {
		return new DynamicResultBuilderInstantiation<>( this );
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		if ( argumentResultBuilders.isEmpty() ) {
			throw new IllegalStateException( "DynamicResultBuilderInstantiation defined no arguments" );
		}

		final List<ArgumentDomainResult<?>> argumentDomainResults = new ArrayList<>( argumentResultBuilders.size() );
		for ( int i = 0; i < argumentResultBuilders.size(); i++ ) {
			argumentDomainResults.add( new ArgumentDomainResult<>(
					argumentResultBuilders.get( i )
							.argumentBuilder.buildResult( jdbcResultsMetadata, i, domainResultCreationState )
			) );
		}

		return new DynamicInstantiationResultImpl<>(
				null,
				DynamicInstantiationNature.CLASS,
				javaType,
				argumentDomainResults
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

		final var that = (DynamicResultBuilderInstantiation<?>) o;
		return javaType.equals( that.javaType )
			&& argumentResultBuilders.equals( that.argumentResultBuilders );
	}

	@Override
	public int hashCode() {
		int result = javaType.hashCode();
		result = 31 * result + argumentResultBuilders.hashCode();
		return result;
	}
}
