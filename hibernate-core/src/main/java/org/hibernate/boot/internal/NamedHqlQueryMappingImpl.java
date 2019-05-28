/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.internal;

import java.util.List;
import java.util.Map;
import javax.persistence.ParameterMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.spi.AbstractNamedQueryMapping;
import org.hibernate.boot.spi.NamedHqlQueryMapping;
import org.hibernate.boot.spi.NamedQueryParameterMapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;

/**
 * @author Steve Ebersole
 */
public class NamedHqlQueryMappingImpl extends AbstractNamedQueryMapping implements NamedHqlQueryMapping {
	private final String hqlString;
	private final Integer firstResult;
	private final Integer maxResults;

	public NamedHqlQueryMappingImpl(
			String name,
			String hqlString,
			List<NamedQueryParameterMapping> parameterMappings,
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
			String comment,
			Map<String,Object> hints) {
		super(
				name,
				parameterMappings,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment,
				hints
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
	public NamedHqlQueryMemento resolve(SessionFactoryImplementor factory) {
		return new NamedHqlQueryMementoImpl(
				getName(),
				hqlString,
				resolveParameterMappings( factory ),
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
				getComment(),
				getHints()
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

		@Override
		protected NamedQueryParameterMapping createPositionalParameter(int i, Class javaType, ParameterMode mode) {
			/// todo (6.0) : this really ought to just adjust the type, if one...
			throw new NotYetImplementedFor6Exception();
//			return new ParameterDefinition() {
//				@Override
//				public ParameterDescriptor resolve(SessionFactoryImplementor factory) {
//					return new ParameterDescriptor() {
//						@Override
//						public QueryParameter toQueryParameter(SharedSessionContractImplementor session) {
//							return new QueryParameterPositionalImpl( i,  );
//						}
//					};
//				}
//			};
		}

		@Override
		protected NamedQueryParameterMapping createNamedParameter(String name, Class javaType, ParameterMode mode) {
			throw new NotYetImplementedFor6Exception();
		}

		public NamedHqlQueryMappingImpl build() {
			return new NamedHqlQueryMappingImpl(
					getName(),
					hqlString,
					getParameterMappings(),
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
					getComment(),
					getHints()
			);
		}
	}
}
