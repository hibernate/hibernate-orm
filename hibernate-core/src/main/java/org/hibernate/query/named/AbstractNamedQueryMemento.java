/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public abstract class AbstractNamedQueryMemento implements NamedQueryMemento {
	private final String name;
	private final @Nullable Class<?> resultType;

	private final Boolean cacheable;
	private final String cacheRegion;
	private final CacheMode cacheMode;

	private final FlushMode flushMode;
	private final Boolean readOnly;

	private final Integer timeout;
	private final Integer fetchSize;

	private final String comment;

	private final Map<String, Object> hints;

	protected AbstractNamedQueryMemento(
			String name,
			@Nullable Class<?> resultType,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String, Object> hints) {
		this.name = name;
		this.resultType = resultType;
		this.cacheable = cacheable;
		this.cacheRegion = cacheRegion;
		this.cacheMode = cacheMode;
		this.flushMode = flushMode;
		this.readOnly = readOnly;
		this.timeout = timeout;
		this.fetchSize = fetchSize;
		this.comment = comment;
		this.hints = hints;
	}

	@Override
	public String getRegistrationName() {
		return name;
	}

	public @Nullable Class<?> getResultType() {
		return resultType;
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

	@Override
	public Map<String, Object> getHints() {
		return hints;
	}

	public static abstract class AbstractBuilder<T extends AbstractBuilder> {
		protected final String name;

		protected Set<String> querySpaces;
		protected Boolean cacheable;
		protected String cacheRegion;
		protected CacheMode cacheMode;

		protected FlushMode flushMode;
		protected Boolean readOnly;

		protected Integer timeout;
		protected Integer fetchSize;

		protected String comment;

		protected Map<String,Object> hints;

		public AbstractBuilder(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		protected abstract T getThis();


		public T addQuerySpaces(Set<String> querySpaces) {
			if ( querySpaces == null || querySpaces.isEmpty() ) {
				return getThis();
			}

			if ( this.querySpaces == null ) {
				this.querySpaces = new HashSet<>();
			}
			this.querySpaces.addAll( querySpaces );
			return getThis();
		}

		public T addQuerySpace(String space) {
			if ( this.querySpaces == null ) {
				this.querySpaces = new HashSet<>();
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

		public T setReadOnly(boolean readOnly) {
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

		public Set<String> getQuerySpaces() {
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

		public Integer getTimeout() {
			return timeout;
		}

		public Integer getFetchSize() {
			return fetchSize;
		}

		public String getComment() {
			return comment;
		}

		public void addHint(String name, Object value) {
			if ( hints == null ) {
				hints = new HashMap<>();
			}
			hints.put( name, value );
		}
	}
}
