/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.named.spi.NamedQueryMemento;
import org.hibernate.query.named.internal.AbstractQueryMemento;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.type.BindableType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of NamedCallableQueryMemento
 *
 * @author Steve Ebersole
 */
public class NamedCallableQueryMementoImpl extends AbstractQueryMemento<Object> implements NamedCallableQueryMemento {
	private final String callableName;

	private final ParameterStrategy parameterStrategy;
	private final List<NamedCallableQueryMemento.ParameterMemento> parameterMementos;

	private final @Nullable String[] resultSetMappingNames;
	private final @Nullable Class<?>[] resultSetMappingClasses;

	private final @Nullable Set<String> querySpaces;


	/**
	 * Constructs a ProcedureCallImpl
	 */
	public NamedCallableQueryMementoImpl(
			@Nonnull String name,
			@Nonnull String callableName,
			@Nonnull ParameterStrategy parameterStrategy,
			@Nonnull List<NamedCallableQueryMemento.ParameterMemento> parameterMementos,
			@Nullable String[] resultSetMappingNames,
			@Nullable Class<?>[] resultSetMappingClasses,
			@Nullable Set<String> querySpaces,
			@Nullable Boolean cacheable,
			@Nullable String cacheRegion,
			@Nullable CacheMode cacheMode,
			@Nullable FlushMode flushMode,
			@Nullable Boolean readOnly,
			@Nullable Timeout timeout,
			@Nullable Integer fetchSize,
			@Nullable String comment,
			@Nonnull Map<String, Object> hints) {
		super(
				name,
				Object.class,
				flushMode,
				timeout,
				comment,
				hints
		);
		this.callableName = callableName;
		this.parameterStrategy = parameterStrategy;
		this.parameterMementos = parameterMementos;
		this.resultSetMappingNames = resultSetMappingNames;
		this.resultSetMappingClasses = resultSetMappingClasses;
		this.querySpaces = querySpaces;
	}

	public NamedCallableQueryMementoImpl(@Nonnull String name, @Nonnull NamedCallableQueryMementoImpl original) {
		super( name, original );

		this.callableName = original.callableName;
		this.parameterStrategy = original.parameterStrategy;
		this.parameterMementos = original.parameterMementos;
		this.resultSetMappingNames = original.resultSetMappingNames;
		this.resultSetMappingClasses = original.resultSetMappingClasses;
		this.querySpaces = original.querySpaces;
	}

	@Nonnull
	@Override
	public String getCallableName() {
		return callableName;
	}

	@Override
	@Nonnull
	public List<NamedCallableQueryMemento.ParameterMemento> getParameterMementos() {
		return parameterMementos;
	}

	@Override
	@Nonnull
	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	@Override
	@Nullable
	public String[] getResultSetMappingNames() {
		return resultSetMappingNames;
	}

	@Override
	@Nullable
	public Class<?>[] getResultSetMappingClasses() {
		return resultSetMappingClasses;
	}

	@Nullable
	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Nonnull
	@Override
	public ProcedureCallImplementor makeProcedureCall(@Nonnull SharedSessionContractImplementor session) {
		return new ProcedureCallImpl<>( session, this );
	}

	@Nonnull
	@Override
	public ProcedureCallImplementor makeProcedureCall(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull String... resultSetMappingNames) {
		return new ProcedureCallImpl<>( session, this, resultSetMappingNames );
	}

	@Nonnull
	@Override
	public ProcedureCallImplementor toQuery(@Nonnull SharedSessionContractImplementor session) {
		return makeProcedureCall( session );
	}

	@Nonnull
	@Override
	public <T> ProcedureCallImplementor<T> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<T> javaType) {
		return makeProcedureCall( session );
	}

	@Nonnull
	@Override
	public MutationQueryImplementor<Object> toMutationQuery(@Nonnull SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "ProcedureCall cannot be treated as a mutation query" );
	}

	@Nonnull
	@Override
	public <T> MutationQueryImplementor<T> toMutationQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<T> targetType) {
		throw new UnsupportedOperationException( "ProcedureCall cannot be treated as a mutation query" );
	}

	@Nonnull
	@Override
	public SelectionQueryImplementor<Object> toSelectionQuery(@Nonnull SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "ProcedureCall cannot be treated as a selection query" );
	}

	@Nonnull
	@Override
	public <T> SelectionQueryImplementor<T> toSelectionQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<T> javaType) {
		throw new UnsupportedOperationException( "ProcedureCall cannot be treated as a selection query" );
	}

	@Nonnull
	@Override
	public NamedQueryMemento<Object> makeCopy(@Nonnull String name) {
		return new NamedCallableQueryMementoImpl( name, this );
	}

	@Override
	public void validate(@Nonnull QueryEngine queryEngine) {
		// anything to do?
	}

	/**
	 * A "disconnected" copy of the metadata for a parameter, that can be used in ProcedureCallMementoImpl.
	 */
	public static class ParameterMementoImpl<T> implements NamedCallableQueryMemento.ParameterMemento {
		private final @Nullable Integer position;
		private final @Nullable String name;
		private final ParameterMode mode;
		private final Class<T> type;
		private final @Nullable BindableType<T> hibernateType;

		/**
		 * Create the memento
		 */
		public ParameterMementoImpl(
				@Nullable Integer position,
				@Nullable String name,
				@Nonnull ParameterMode mode,
				@Nonnull Class<T> type,
				@Nullable BindableType<T> hibernateType) {
			this.position = position;
			this.name = name;
			this.mode = mode;
			this.type = type;
			this.hibernateType = hibernateType;
		}

		@Nullable
		public Integer getPosition() {
			return position;
		}

		@Nullable
		public String getName() {
			return name;
		}

		@Nonnull
		public ParameterMode getMode() {
			return mode;
		}

		@Nonnull
		public Class<T> getType() {
			return type;
		}

		@Nullable
		public BindableType<T> getHibernateType() {
			return hibernateType;
		}

		@Override
		@Nonnull
		public ProcedureParameterImplementor<T> resolve(@Nonnull SharedSessionContractImplementor session) {
			if ( getName() != null ) {
				return new ProcedureParameterImpl<>(
						getName(),
						getMode(),
						type,
						getHibernateType()
				);
			}
			else {
				return new ProcedureParameterImpl<>(
						getPosition(),
						getMode(),
						type,
						getHibernateType()
				);
			}

		}

		/**
		 * Build a ParameterMemento from the given parameter registration
		 *
		 * @param registration The parameter registration from a ProcedureCall
		 *
		 * @return The memento
		 */
		@Nonnull
		public static <U> ParameterMementoImpl<U> fromRegistration(
				@Nonnull ProcedureParameterImplementor<U> registration) {
			return new ParameterMementoImpl<>(
					registration.getPosition(),
					registration.getName(),
					registration.getMode(),
					registration.getParameterType(),
					registration.getHibernateType()
			);
		}

	}
}
