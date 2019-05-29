/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.spi.AbstractNamedQueryMapping;
import org.hibernate.boot.spi.NamedNativeQueryMapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sql.internal.NamedNativeQueryMementoImpl;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

/**
 * @author Steve Ebersole
 */
public class NamedNativeQueryMappingImpl extends AbstractNamedQueryMapping implements NamedNativeQueryMapping {
	private final String sqlString;
	private final String resultSetMappingName;
	private final Set<String> querySpaces;

	public NamedNativeQueryMappingImpl(
			String name,
			String sqlString,
			List<NamedQueryParameterMapping> parameterMappings,
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
				sqlString,
				resultSetMappingName,
				resolveParameterMappings( factory ),
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

		private String resultSetMappingName;
		private Class resultSetMappingClass;

		public Builder(String name) {
			super( name );
		}

		public Builder setSqlString(String sqlString) {
			this.sqlString = sqlString;
			return getThis();
		}

		public NamedNativeQueryMappingImpl build() {
			return new NamedNativeQueryMappingImpl(
					getName(),
					sqlString,
					getParameterMappings(),
					resultSetMappingName,
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
		protected NamedQueryParameterMapping createPositionalParameter(int i, Class javaType, ParameterMode mode) {
			//noinspection Convert2Lambda
			return new NamedQueryParameterMapping() {
				@Override
				@SuppressWarnings("unchecked")
				public ParameterMemento resolve(SessionFactoryImplementor factory) {
					return session -> new QueryParameterPositionalImpl(
							i,
							false,
							factory.getMetamodel().getTypeConfiguration().standardBasicTypeForJavaType( javaType )
					);
				}
			};
		}

		@Override
		protected NamedQueryParameterMapping createNamedParameter(String name, Class javaType, ParameterMode mode) {
			return null;
		}

		public Builder setResultSetMappingName(String resultSetMappingName) {
			this.resultSetMappingName = resultSetMappingName;
			return this;
		}
	}
}
