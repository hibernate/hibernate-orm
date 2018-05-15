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
import org.hibernate.query.named.spi.AbstractNamedQueryMemento;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
import org.hibernate.query.named.spi.ParameterMemento;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.sql.internal.NativeQueryImpl;

/**
 * @author Steve Ebersole
 */
public class NamedNativeQueryMementoImpl extends AbstractNamedQueryMemento implements NamedNativeQueryMemento {
	private final String sqlString;
	private final String resultSetMappingName;
	private final Set<String> querySpaces;

	public NamedNativeQueryMementoImpl(
			String name,
			List<ParameterMemento> parameterMementos,
			String sqlString,
			String resultSetMappingName,
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
			Map<String,Object> hints) {
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
		this.sqlString = sqlString;
		this.resultSetMappingName = resultSetMappingName;
		this.querySpaces = querySpaces;
	}

	@Override
	public String getSqlString() {
		return sqlString;
	}

	@Override
	public String getQueryString() {
		return sqlString;
	}

	@Override
	public String getResultSetMappingName() {
		return resultSetMappingName;
	}

	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public NamedNativeQueryMemento makeCopy(String name) {
		return new NamedNativeQueryMementoImpl(
				name,
				getParameterMementos(),
				sqlString,
				resultSetMappingName,
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
				getHints()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeQueryImplementor toQuery(SharedSessionContractImplementor session, Class resultType) {
		final NativeQueryImpl query = new NativeQueryImpl( this, resultType, session );

		applyBaseOptions( query, session );

		return query;
	}
}
