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
import org.hibernate.boot.model.query.spi.NamedNativeQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.internal.NamedNativeQueryDescriptorImpl;
import org.hibernate.query.named.spi.NamedNativeQueryDescriptor;

/**
 * @author Steve Ebersole
 */
public class NamedNativeQueryDefinitionImpl extends AbstractNamedQueryDefinition implements NamedNativeQueryDefinition {
	private final String sqlString;
	private final String resultSetMappingName;
	private final Collection<String> querySpaces;

	public NamedNativeQueryDefinitionImpl(
			String name,
			String sqlString,
			String resultSetMappingName,
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
		this.sqlString = sqlString;
		this.resultSetMappingName = resultSetMappingName;
		this.querySpaces = querySpaces;
	}

	@Override
	public String getQueryString() {
		return sqlString;
	}

	@Override
	public String getResultSetMappingName() {
		return resultSetMappingName;
	}

	@Override
	public NamedNativeQueryDescriptor resolve(SessionFactoryImplementor factory) {
		return new NamedNativeQueryDescriptorImpl(
				getName(),
				sqlString,
				resultSetMappingName,
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

	public static class Builder extends AbstractBuilder<Builder> {
		private String sqlString;

		private String resultSetMapping;

		public Builder(String name) {
			super( name );
		}

		public Builder setSqlString(String sqlString) {
			this.sqlString = sqlString;
			return this;
		}

		public NamedNativeQueryDefinitionImpl build() {
			return new NamedNativeQueryDefinitionImpl(
					getName(),
					sqlString,
					resultSetMapping,
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
					// todo (6.0) : information about parameters
			);
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public Builder setResultSetMapping(String resultSetMapping) {
			this.resultSetMapping = resultSetMapping;
			return this;
		}
	}
}
