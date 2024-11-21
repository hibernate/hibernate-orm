/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.ParameterMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.BindableType;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.named.AbstractNamedQueryMemento;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;

/**
 * Implementation of NamedCallableQueryMemento
 *
 * @author Steve Ebersole
 */
public class NamedCallableQueryMementoImpl extends AbstractNamedQueryMemento implements NamedCallableQueryMemento {
	private final String callableName;

	private final ParameterStrategy parameterStrategy;
	private final List<NamedCallableQueryMemento.ParameterMemento> parameterMementos;

	private final String[] resultSetMappingNames;
	private final Class<?>[] resultSetMappingClasses;

	private final Set<String> querySpaces;


	/**
	 * Constructs a ProcedureCallImpl
	 */
	public NamedCallableQueryMementoImpl(
			String name,
			String callableName,
			ParameterStrategy parameterStrategy,
			List<NamedCallableQueryMemento.ParameterMemento> parameterMementos,
			String[] resultSetMappingNames,
			Class<?>[] resultSetMappingClasses,
			Set<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String, Object> hints) {
		super(
				name,
				Object.class,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				timeout,
				fetchSize,
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

	@Override
	public String getCallableName() {
		return callableName;
	}

	@Override
	public List<NamedCallableQueryMemento.ParameterMemento> getParameterMementos() {
		return parameterMementos;
	}

	@Override
	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	@Override
	public String[] getResultSetMappingNames() {
		return resultSetMappingNames;
	}

	@Override
	public Class<?>[] getResultSetMappingClasses() {
		return resultSetMappingClasses;
	}

	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public ProcedureCallImplementor<?> makeProcedureCall(SharedSessionContractImplementor session) {
		return new ProcedureCallImpl<>( session, this );
	}

	@Override
	public ProcedureCall makeProcedureCall(
			SharedSessionContractImplementor session,
			String... resultSetMappingNames) {
		return new ProcedureCallImpl<>( session, this, resultSetMappingNames );
	}

	@Override
	public ProcedureCall makeProcedureCall(
			SharedSessionContractImplementor session,
			Class<?>... resultSetJavaTypes) {
		return null;
	}

	@Override
	public <T> QueryImplementor<T> toQuery(SharedSessionContractImplementor session) {
		return new ProcedureCallImpl<>( session, this );
	}

	@Override
	public <T> ProcedureCallImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> javaType) {
		return new ProcedureCallImpl<>( session, this, javaType );
	}

	@Override
	public NamedQueryMemento makeCopy(String name) {
		return new NamedCallableQueryMementoImpl(
				name,
				callableName,
				parameterStrategy,
				parameterMementos,
				resultSetMappingNames,
				resultSetMappingClasses,
				querySpaces,
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				getHints()
		);
	}

	@Override
	public void validate(QueryEngine queryEngine) {
		// anything to do?
	}

	/**
	 * A "disconnected" copy of the metadata for a parameter, that can be used in ProcedureCallMementoImpl.
	 */
	public static class ParameterMementoImpl<T> implements NamedCallableQueryMemento.ParameterMemento {
		private final Integer position;
		private final String name;
		private final ParameterMode mode;
		private final Class<T> type;
		private final BindableType<T> hibernateType;

		/**
		 * Create the memento
		 */
		public ParameterMementoImpl(
				int position,
				String name,
				ParameterMode mode,
				Class<T> type,
				BindableType<T> hibernateType) {
			this.position = position;
			this.name = name;
			this.mode = mode;
			this.type = type;
			this.hibernateType = hibernateType;
		}

		public Integer getPosition() {
			return position;
		}

		public String getName() {
			return name;
		}

		public ParameterMode getMode() {
			return mode;
		}

		public Class<T> getType() {
			return type;
		}

		public BindableType<T> getHibernateType() {
			return hibernateType;
		}

		@Override
		public ProcedureParameterImplementor<T> resolve(SharedSessionContractImplementor session) {
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
		public static <U> ParameterMementoImpl<U> fromRegistration(ProcedureParameterImplementor<U> registration) {
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
