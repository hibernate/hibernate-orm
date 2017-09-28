/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.query.internal;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.query.spi.NamedQueryDefinition;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNamedQueryDefinition implements NamedQueryDefinition {
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

	public AbstractNamedQueryDefinition(
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

	protected static abstract class AbstractBuilder<T extends AbstractBuilder> {
		private final String name;

		private Collection<String> querySpaces;
		private Boolean cacheable;
		private String cacheRegion;
		private CacheMode cacheMode;

		private FlushMode flushMode;
		private Boolean readOnly;

		private LockOptions lockOptions;

		private Integer timeout;
		private Integer fetchSize;

		private String comment;

		public AbstractBuilder(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		protected abstract T getThis();

		public T addQuerySpaces(Collection<String> querySpaces) {
			if ( querySpaces == null || querySpaces.isEmpty() ) {
				return getThis();
			}

			if ( this.querySpaces == null ) {
				this.querySpaces = new ArrayList<>();
			}
			this.querySpaces.addAll( querySpaces );
			return getThis();
		}

		public T addQuerySpace(String space) {
			if ( this.querySpaces == null ) {
				this.querySpaces = new ArrayList<>();
			}
			this.querySpaces.add( space );
			return getThis();
		}

		public T setCacheable(Boolean cacheable) {
			this.cacheable = cacheable;
			return getThis();
		}

		public T setCacheRegion(String cacheRegion) {
			this.cacheRegion = cacheRegion;
			return getThis();
		}

		public T setCacheMode(CacheMode cacheMode) {
			this.cacheMode = cacheMode;
			return getThis();
		}

		public T setLockOptions(LockOptions lockOptions) {
			this.lockOptions = lockOptions;
			return getThis();
		}

		public T setTimeout(Integer timeout) {
			this.timeout = timeout;
			return getThis();
		}

		public T setFlushMode(FlushMode flushMode) {
			this.flushMode = flushMode;
			return getThis();
		}

		public T setReadOnly(Boolean readOnly) {
			this.readOnly = readOnly;
			return getThis();
		}

		public T setFetchSize(Integer fetchSize) {
			this.fetchSize = fetchSize;
			return getThis();
		}

		public T setComment(String comment) {
			this.comment = comment;
			return getThis();
		}


		public Collection<String> getQuerySpaces() {
			return querySpaces;
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
	}
}
