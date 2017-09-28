/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.internal;

import java.util.Collection;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.named.spi.AbstractNamedQueryDescriptor;
import org.hibernate.query.named.spi.NamedNativeQueryDescriptor;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.sql.internal.NativeQueryImpl;

/**
 * @author Steve Ebersole
 */
public class NamedNativeQueryDescriptorImpl extends AbstractNamedQueryDescriptor implements NamedNativeQueryDescriptor {
	private final String sqlString;
	private final String resultSetMappingName;
	private final Collection<String> querySpaces;

	public NamedNativeQueryDescriptorImpl(
			String name,
			String sqlString,
			String resultSetMappingName,
			Collection<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment) {
		super(
				name,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment
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
	public Collection<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public NativeQueryImplementor toQuery(SharedSessionContractImplementor session) {
		final NativeQueryImpl query = new NativeQueryImpl( this, session );

		applyBaseOptions( query, session );

		return query;
	}

	@Override
	public NamedNativeQueryDescriptor makeCopy(String name) {
		return new NamedNativeQueryDescriptorImpl(
				name,
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
				getComment()
		);
	}
}
