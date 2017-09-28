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
import org.hibernate.boot.model.query.spi.NamedCallableQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.internal.NamedCallableQueryDescriptorImpl;
import org.hibernate.query.named.spi.NamedCallableQueryDescriptor;

/**
 * @author Steve Ebersole
 */
public class NamedCallableQueryDefinitionImpl
		extends AbstractNamedQueryDefinition
		implements NamedCallableQueryDefinition {
	private final String callableName;
	private final Collection<String> querySpaces;

	public NamedCallableQueryDefinitionImpl(
			String name,
			String callableName,
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
		this.callableName = callableName;
		this.querySpaces = querySpaces;
	}

	@Override
	public NamedCallableQueryDescriptor resolve(SessionFactoryImplementor factory) {
		return new NamedCallableQueryDescriptorImpl(
				getName(),
				callableName,
				querySpaces,
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
		private String callableName;

		public Builder(String name) {
			super( name );
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public NamedCallableQueryDefinition build() {
			return new NamedCallableQueryDefinitionImpl(
					getName(),
					callableName,
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

		public Builder setCallableName(String callableName) {
			this.callableName = callableName;
			return this;
		}
	}
}
