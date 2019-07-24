/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.AbstractNamedQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

/**
 * Keeps details of a named native-sql query
 *
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class NamedNativeQueryMementoImpl extends AbstractNamedQueryMemento implements NamedNativeQueryMemento {
	private final String sqlString;

	private String resultSetMappingName;
	private Class resultSetMappingClass;

	private final Set<String> querySpaces;

	public NamedNativeQueryMementoImpl(
			String name,
			String sqlString,
			String resultSetMappingName,
			Class resultSetMappingClass,
			Set<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String,Object> hints) {
		super(
				name,
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
		this.sqlString = sqlString;
		this.resultSetMappingName = resultSetMappingName;
		this.resultSetMappingClass = resultSetMappingClass;
		this.querySpaces = querySpaces;
	}

	public String getResultSetMappingName() {
		return resultSetMappingName;
	}

	public Class<?> getResultSetMappingClass() {
		return resultSetMappingClass;
	}

	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public String getSqlString() {
		return sqlString;
	}

	@Override
	public NamedNativeQueryMemento makeCopy(String name) {
		return new NamedNativeQueryMementoImpl(
				name,
				sqlString,
				resultSetMappingName,
				resultSetMappingClass,
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
		// todo (6.0) : add any validation we want here
	}

	@Override
	public <T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session) {
		//noinspection unchecked
		return toQuery( session, (Class) null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType) {
		return new NativeQueryImpl( this, resultType, session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, String resultSetMappingName) {
		return new NativeQueryImpl( this, resultSetMappingName, session );
	}
}
