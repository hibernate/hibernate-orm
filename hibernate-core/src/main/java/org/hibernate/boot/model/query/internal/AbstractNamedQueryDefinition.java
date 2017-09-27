/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.query.internal;

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

	public AbstractNamedQueryDefinition(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
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

		public T setQuerySpaces(Collection<String> querySpaces) {
			this.querySpaces = querySpaces;
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
	}
}
