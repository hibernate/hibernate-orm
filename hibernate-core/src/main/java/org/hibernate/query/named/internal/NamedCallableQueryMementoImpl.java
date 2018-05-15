/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.named.spi.AbstractNamedQueryMemento;
import org.hibernate.query.named.spi.NamedCallableQueryMemento;
import org.hibernate.query.named.spi.ParameterMemento;
import org.hibernate.query.named.spi.RowReaderMemento;

/**
 * @author Steve Ebersole
 */
public class NamedCallableQueryMementoImpl
		extends AbstractNamedQueryMemento
		implements NamedCallableQueryMemento {
	private final String callableName;
	private final ParameterStrategy parameterStrategy;
	private final Class[] resultClasses;
	private final String[] resultSetMappingNames;
	private final Set<String> querySpaces;

	public NamedCallableQueryMementoImpl(
			String name,
			String callableName,
			ParameterStrategy parameterStrategy,
			List<ParameterMemento> parameterMementos,
			Class[] resultClasses,
			String[] resultSetMappingNames,
			Set<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String, Object> hints) {
		super(
				name,
				parameterMementos,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment,
				hints
		);
		this.callableName = callableName;
		this.parameterStrategy = parameterStrategy;
		this.resultClasses = resultClasses;
		this.resultSetMappingNames = resultSetMappingNames;
		this.querySpaces = querySpaces;
	}

	public NamedCallableQueryMementoImpl(
			String name,
			String procedureName,
			ParameterStrategy parameterStrategy,
			List<ParameterMemento> parameterMementos,
			RowReaderMemento rowReaderMemento,
			Set<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String, Object> hints) {
		this(
				name,
				procedureName,
				parameterStrategy,
				parameterMementos,
				rowReaderMemento.getResultClasses(),
				rowReaderMemento.getResultMappingNames(),
				querySpaces,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment,
				hints
		);
	}

	@Override
	public String getCallableName() {
		return callableName;
	}

	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public String getQueryString() {
		return callableName;
	}

	@Override
	public Class[] getResultClasses() {
		return resultClasses;
	}

	@Override
	public String[] getResultSetMappingNames() {
		return resultSetMappingNames;
	}

	@Override
	public NamedCallableQueryMemento makeCopy(String name) {
		return new NamedCallableQueryMementoImpl(
				name,
				getCallableName(),
				getParameterStrategy(),
				getParameterMementos(),
				resultClasses,
				resultSetMappingNames,
				getQuerySpaces(),
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getLockOptions(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				getHintsCopy()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ProcedureCallImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType) {
		return ProcedureCallImpl.fromMemento( this, session );
	}
}
