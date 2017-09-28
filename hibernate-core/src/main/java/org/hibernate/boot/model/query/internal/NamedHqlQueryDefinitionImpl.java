/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.query.internal;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.query.spi.NamedHqlQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.internal.NamedHqlQueryDescriptorImpl;
import org.hibernate.query.named.spi.NamedHqlQueryDescriptor;

/**
 * @author Steve Ebersole
 */
public class NamedHqlQueryDefinitionImpl extends AbstractNamedQueryDefinition implements NamedHqlQueryDefinition {
	private final String hqlString;
	private final Integer firstResult;
	private final Integer maxResults;

	public NamedHqlQueryDefinitionImpl(
			String name,
			String hqlString,
			Integer firstResult,
			Integer maxResults,
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
		this.hqlString = hqlString;
		this.firstResult = firstResult;
		this.maxResults = maxResults;
	}

	@Override
	public String getQueryString() {
		return hqlString;
	}

	@Override
	public NamedHqlQueryDescriptor resolve(SessionFactoryImplementor factory) {
		return new NamedHqlQueryDescriptorImpl(
				getName(),
				hqlString,
				firstResult,
				maxResults,
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

	public  static class Builder extends AbstractBuilder<Builder> {
		private final String hqlString;

		private Integer firstResult;
		private Integer maxResults;

		public Builder(String name, String hqlString) {
			super( name );
			this.hqlString = hqlString;
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public Builder setFirstResult(Integer firstResult) {
			this.firstResult = firstResult;
			return getThis();
		}

		public Builder setMaxResults(Integer maxResults) {
			this.maxResults = maxResults;
			return getThis();
		}

		public NamedHqlQueryDefinitionImpl build() {
			return new NamedHqlQueryDefinitionImpl(
					getName(),
					hqlString,
					firstResult,
					maxResults,
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
}
