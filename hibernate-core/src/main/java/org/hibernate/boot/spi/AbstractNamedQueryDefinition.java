/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.query.NamedQueryDefinition;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNamedQueryDefinition<R> implements NamedQueryDefinition<R> {
	private final String name;
	private final @Nullable Class<R> resultType;

	private final Boolean cacheable;
	private final String cacheRegion;
	private final CacheMode cacheMode;

	private final FlushMode flushMode;
	private final Boolean readOnly;

	private final LockOptions lockOptions;

	private final Integer timeout;
	private final Integer fetchSize;

	private final String comment;

	private final Map<String,Object> hints;

	public AbstractNamedQueryDefinition(
			String name,
			@Nullable Class<R> resultType,
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
		this.name = name;
		this.resultType = resultType;
		this.cacheable = cacheable;
		this.cacheRegion = cacheRegion;
		this.cacheMode = cacheMode;
		this.flushMode = flushMode;
		this.readOnly = readOnly;
		this.lockOptions = lockOptions;
		this.timeout = timeout;
		this.fetchSize = fetchSize;
		this.comment = comment;
		this.hints = hints == null ? new HashMap<>() : new HashMap<>( hints );
	}

	@Override
	public String getRegistrationName() {
		return name;
	}

	@Override
	public @Nullable Class<R> getResultType() {
		return resultType;
	}

	public Boolean getCacheable() {
		return cacheable;
	}

	public String getCacheRegion() {
		return cacheRegion;
	}

	public CacheMode getCacheMode() {
		return cacheMode;
	}

	public FlushMode getFlushMode() {
		return flushMode;
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public Integer getFetchSize() {
		return fetchSize;
	}

	public String getComment() {
		return comment;
	}

	public Map<String, Object> getHints() {
		return hints;
	}

}
