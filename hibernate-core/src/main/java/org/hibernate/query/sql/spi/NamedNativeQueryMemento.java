/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.Set;

import jakarta.persistence.SqlResultSetMapping;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.named.AbstractNamedQueryMemento;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.sql.internal.NamedNativeQueryMementoImpl;

/**
 * Descriptor for a named native query in the runtime environment
 *
 * @author Steve Ebersole
 */
public interface NamedNativeQueryMemento extends NamedQueryMemento {
	/**
	 * Informational access to the SQL query string
	 */
	String getSqlString();

	default String getOriginalSqlString(){
		return getSqlString();
	}

	/**
	 * The affected query spaces.
	 */
	Set<String> getQuerySpaces();

	/**
	 * An explicit ResultSet mapping by name
	 *
	 * @see SqlResultSetMapping#name
	 */
	String getResultMappingName();

	/**
	 * An implicit entity mapping by entity class
	 */
	Class<?> getResultMappingClass();

	@Nullable Class<?> getResultType();

	Integer getFirstResult();

	Integer getMaxResults();

	/**
	 * Convert the memento into an untyped executable query
	 */
	@Override
	<T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session);

	/**
	 * Convert the memento into a typed executable query
	 */
	@Override
	<T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);

	/**
	 * Convert the memento into a typed executable query
	 */
	<T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, String resultSetMapping);

	@Override
	NamedNativeQueryMemento makeCopy(String name);

	/**
	 * Delegate used in creating named HQL query mementos.
	 *
	 * @see NamedNativeQueryDefinition
	 */
	class Builder extends AbstractNamedQueryMemento.AbstractBuilder<Builder> {
		protected String queryString;

		protected Integer firstResult;
		protected Integer maxResults;

		protected Set<String> querySpaces;

		protected String resultSetMappingName;
		protected String resultSetMappingClassName;


		public Builder(String name) {
			super( name );
		}

		@Override
		protected Builder getThis() {
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

		public Builder setFirstResult(Integer firstResult) {
			this.firstResult = firstResult;
			return this;
		}

		public Builder setMaxResults(Integer maxResults) {
			this.maxResults = maxResults;
			return this;
		}

		public void setQuerySpaces(Set<String> querySpaces) {
			this.querySpaces = querySpaces;
		}

		public void setResultSetMappingName(String resultSetMappingName) {
			this.resultSetMappingName = resultSetMappingName;
		}

		public void setResultSetMappingClassName(String resultSetMappingClassName) {
			this.resultSetMappingClassName = resultSetMappingClassName;
		}

		public NamedNativeQueryMemento build(SessionFactoryImplementor sessionFactory) {
			return new NamedNativeQueryMementoImpl(
					name,
					null,
					queryString,
					queryString,
					resultSetMappingName,
					sessionFactory.getServiceRegistry()
							.requireService( ClassLoaderService.class )
							.classForName( resultSetMappingClassName ),
					querySpaces,
					cacheable,
					cacheRegion,
					cacheMode,
					flushMode,
					readOnly,
					timeout,
					fetchSize,
					comment,
					firstResult,
					maxResults,
					hints
			);
		}
	}
}
