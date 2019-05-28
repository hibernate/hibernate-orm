/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.spi.NamedQueryMemento;

/**
 * @author Steve Ebersole
 */
public interface NamedHqlQueryMemento extends NamedQueryMemento {
	String getHqlString();

	@Override
	default String getQueryString() {
		return getHqlString();
	}

	@Override
	NamedHqlQueryMemento makeCopy(String name);

	@Override
	<T> HqlQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);

	/**
	 * Delegate used in creating named HQL query mementos for queries defined in
	 * annotations, hbm.xml or orm.xml
	 */
	class Builder {
		protected String name;
		protected String query;
		protected boolean cacheable;
		protected String cacheRegion;
		protected Integer timeout;
		protected Integer fetchSize;
		protected FlushMode flushMode;
		protected CacheMode cacheMode;
		protected boolean readOnly;
		protected String comment;
		protected Map parameterTypes;
		protected LockOptions lockOptions;
		protected Integer firstResult;
		protected Integer maxResults;

		public Builder() {
		}

		public Builder(String name) {
			this.name = name;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setQuery(String query) {
			this.query = query;
			return this;
		}

		public Builder setCacheable(boolean cacheable) {
			this.cacheable = cacheable;
			return this;
		}

		public Builder setCacheRegion(String cacheRegion) {
			this.cacheRegion = cacheRegion;
			return this;
		}

		public Builder setTimeout(Integer timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder setFetchSize(Integer fetchSize) {
			this.fetchSize = fetchSize;
			return this;
		}

		public Builder setFlushMode(FlushMode flushMode) {
			this.flushMode = flushMode;
			return this;
		}

		public Builder setCacheMode(CacheMode cacheMode) {
			this.cacheMode = cacheMode;
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setComment(String comment) {
			this.comment = comment;
			return this;
		}

		public Builder addParameterType(String name, String typeName) {
			if ( this.parameterTypes == null ) {
				this.parameterTypes = new HashMap();
			}
			this.parameterTypes.put( name, typeName );
			return this;
		}

		public Builder setParameterTypes(Map parameterTypes) {
			this.parameterTypes = parameterTypes;
			return this;
		}

		public Builder setLockOptions(LockOptions lockOptions) {
			this.lockOptions = lockOptions;
			return this;
		}

		public Builder setFirstResult(Integer firstResult) {
			this.firstResult = firstResult;
			return this;
		}

		public Builder setMaxResults(Integer maxResults) {
			this.maxResults = maxResults;
			return this;
		}

		public NamedHqlQueryMemento createNamedQueryDefinition() {
			return new NamedHqlQueryMementoImpl(
					name,
					query,
					cacheable,
					cacheRegion,
					timeout,
					lockOptions,
					fetchSize,
					flushMode,
					cacheMode,
					readOnly,
					comment,
					parameterTypes,
					firstResult,
					maxResults
			);
		}
	}
}
