/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.spi;

import java.util.Collection;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNamedQueryDescriptor implements NamedQueryDescriptor {
	private final String name;

	private final Boolean cacheable;
	private final String cacheRegion;
	private final CacheMode cacheMode;

	private final FlushMode flushMode;
	private final Boolean readOnly;

	private final LockOptions lockOptions;

	private final Integer timeout;
	private final Integer fetchSize;

	private final String comment;

	public AbstractNamedQueryDescriptor(
			String name,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment) {
		this.name = name;
		this.cacheable = cacheable;
		this.cacheRegion = cacheRegion;
		this.cacheMode = cacheMode;
		this.flushMode = flushMode;
		this.readOnly = readOnly;
		this.lockOptions = lockOptions;
		this.timeout = timeout;
		this.fetchSize = fetchSize;
		this.comment = comment;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Boolean getCacheable() {
		return cacheable;
	}

	@Override
	public String getCacheRegion() {
		return cacheRegion;
	}

	@Override
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	public FlushMode getFlushMode() {
		return flushMode;
	}

	@Override
	public Boolean getReadOnly() {
		return readOnly;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public Integer getTimeout() {
		return timeout;
	}

	@Override
	public Integer getFetchSize() {
		return fetchSize;
	}

	@Override
	public String getComment() {
		return comment;
	}

	protected void applyBaseOptions(QueryImplementor query, SharedSessionContractImplementor session) {
		if ( cacheable != null ) {
			query.setCacheable( cacheable );
		}

		if ( cacheRegion != null ) {
			query.setCacheRegion( cacheRegion );
		}

		if ( cacheMode != null ) {
			query.setCacheMode( cacheMode );
		}

		if ( flushMode != null ) {
			query.setHibernateFlushMode( flushMode );
		}

		if ( readOnly != null ) {
			query.setReadOnly( readOnly );
		}

		if ( lockOptions != null ) {
			query.setLockOptions( lockOptions );
		}

		if ( timeout != null ) {
			query.setTimeout( timeout );
		}

		if ( fetchSize != null ) {
			query.setFetchSize( fetchSize );
		}

		if ( comment != null ) {
			query.setComment( comment );
		}
	}
}
