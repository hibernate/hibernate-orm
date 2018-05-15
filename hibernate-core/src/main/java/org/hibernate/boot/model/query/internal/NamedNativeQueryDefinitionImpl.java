/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.query.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.query.spi.NamedNativeQueryDefinition;
import org.hibernate.boot.model.query.spi.ParameterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryParameterPositionalImpl;
import org.hibernate.query.named.internal.NamedNativeQueryMementoImpl;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
import org.hibernate.query.named.spi.ParameterMemento;

/**
 * @author Steve Ebersole
 */
public class NamedNativeQueryDefinitionImpl extends AbstractNamedQueryDefinition implements NamedNativeQueryDefinition {
	private final String sqlString;
	private final String resultSetMappingName;
	private final Set<String> querySpaces;

	public NamedNativeQueryDefinitionImpl(
			String name,
			String sqlString,
			List<ParameterDefinition> parameterDescriptors,
			String resultSetMappingName,
			Set<String> querySpaces,
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
				parameterDescriptors,
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
	public NamedNativeQueryMemento resolve(SessionFactoryImplementor factory) {
		return new NamedNativeQueryMementoImpl(
				getName(),
				resolveParameterDescriptors( factory ),
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
				getComment(),
				getHints()
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
					getParameterDescriptors(),
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
					getComment(),
					getHints()
			);
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		@Override
		protected ParameterDefinition createPositionalParameter(int i, Class javaType, ParameterMode mode) {
			//noinspection Convert2Lambda
			return new ParameterDefinition() {
				@Override
				@SuppressWarnings("unchecked")
				public ParameterMemento resolve(SessionFactoryImplementor factory) {
					return session -> new QueryParameterPositionalImpl(
							i,
							false,
							factory.getTypeConfiguration().getBasicTypeRegistry().getBasicType( javaType )
					);
				}
			};
		}

		@Override
		protected ParameterDefinition createNamedParameter(String name, Class javaType, ParameterMode mode) {
			return null;
		}

		public Builder setResultSetMapping(String resultSetMapping) {
			this.resultSetMapping = resultSetMapping;
			return this;
		}
	}
}
