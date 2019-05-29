/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.Collection;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.NamedQueryMemento;
import org.hibernate.query.sql.internal.NamedNativeQueryMementoImpl;

/**
 * Descriptor for a named native query in the run-time environment
 *
 * @author Steve Ebersole
 */
public interface NamedNativeQueryMemento extends NamedQueryMemento {
	String getSqlString();

	@Override
	NamedNativeQueryMemento makeCopy(String name);

	@Override
	<T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);


	/**
	 * Delegate used in creating named HQL query mementos.
	 *
	 * @see org.hibernate.boot.spi.NamedNativeQueryMapping
	 */
	class Builder {
		protected String name;
		protected String queryString;
		protected boolean cacheable;
		protected String cacheRegion;
		protected CacheMode cacheMode;
		protected Integer timeout;
		protected Integer fetchSize;
		protected FlushMode flushMode;
		protected boolean readOnly;
		protected String comment;
		protected Integer firstResult;
		protected Integer maxResults;

		private Collection<String> querySpaces;
		private String resultSetMappingName;
		private String resultSetMappingClassName;


		public Builder() {
		}

		public Builder(String name) {
			this.name = name;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setQuery(String queryString) {
			this.queryString = queryString;
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

		public Builder setCacheMode(CacheMode cacheMode) {
			this.cacheMode = cacheMode;
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

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setComment(String comment) {
			this.comment = comment;
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

		public void setQuerySpaces(Collection<String> querySpaces) {
			this.querySpaces = querySpaces;
		}

		public void setResultSetMappingName(String resultSetMappingName) {
			this.resultSetMappingName = resultSetMappingName;
		}

		public void setResultSetMappingClassName(String resultSetMappingClassName) {
			this.resultSetMappingClassName = resultSetMappingClassName;
		}

		public NamedNativeQueryMemento createNamedQueryDefinition() {
			return new NamedNativeQueryMementoImpl(
					name,
					queryString,
					cacheable,
					cacheRegion,
					timeout,
					fetchSize,
					flushMode,
					cacheMode,
					readOnly,
					comment,
					firstResult,
					maxResults
			);
		}
	}
}
