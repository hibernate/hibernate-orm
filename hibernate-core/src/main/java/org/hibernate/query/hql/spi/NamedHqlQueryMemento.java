/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.boot.spi.NamedHqlQueryDefinition;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.named.AbstractNamedQueryMemento;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.query.named.NamedQueryMemento;

/**
 * NamedQueryMemento for HQL queries
 *
 * @author Steve Ebersole
 */
public interface NamedHqlQueryMemento extends NamedQueryMemento {
	/**
	 * Informational access to the HQL query string
	 */
	String getHqlString();

	/**
	 * Convert the memento into a typed executable query
	 */
	<T> HqlQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);

	/**
	 * Convert the memento into an untyped executable query
	 */
	HqlQueryImplementor<?> toQuery(SharedSessionContractImplementor session);

	Integer getFirstResult();

	Integer getMaxResults();

	LockOptions getLockOptions();

	Map<String, String> getParameterTypes();

	@Override
	NamedHqlQueryMemento makeCopy(String name);

	/**
	 * Delegate used in creating named HQL query mementos.
	 *
	 * @see NamedHqlQueryDefinition
	 * @see NameableQuery#toMemento
	 */
	class Builder extends AbstractNamedQueryMemento.AbstractBuilder<Builder> {
		protected String hqlString;
		protected LockOptions lockOptions;
		protected Integer firstResult;
		protected Integer maxResults;
		protected Map<String,String> parameterTypes;


		public Builder(String name) {
			super( name );
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder addParameterType(String name, String typeName) {
			if ( this.parameterTypes == null ) {
				this.parameterTypes = new HashMap<>();
			}
			this.parameterTypes.put( name, typeName );
			return this;
		}

		public Builder setParameterTypes(Map<String,String> parameterTypes) {
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
					hqlString,
					firstResult,
					maxResults,
					cacheable,
					cacheRegion,
					cacheMode,
					flushMode,
					readOnly,
					lockOptions,
					timeout,
					fetchSize,
					comment,
					parameterTypes,
					hints
			);
		}
	}
}
