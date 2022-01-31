/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.sqm.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.named.AbstractNamedQueryMemento;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.sqm.internal.NamedSqmQueryMementoImpl;
import org.hibernate.query.sqm.tree.SqmStatement;

public interface NamedSqmQueryMemento extends NamedQueryMemento {
	<T> SqmQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);

	/**
	 * Convert the memento into an untyped executable query
	 */
	<T> SqmQueryImplementor<T> toQuery(SharedSessionContractImplementor session);

	SqmStatement getSqmStatement();

	Integer getFirstResult();

	Integer getMaxResults();

	LockOptions getLockOptions();

	Map<String, String> getParameterTypes();

	@Override
	NamedSqmQueryMemento makeCopy(String name);

	/**
	 * Delegate used in creating named HQL query mementos.
	 *
	 * @see NamedHqlQueryDefinition
	 * @see NameableQuery#toMemento
	 */
	class Builder extends AbstractNamedQueryMemento.AbstractBuilder<NamedSqmQueryMemento.Builder> {
		protected SqmStatement sqmSelectStatement;
		protected LockOptions lockOptions;
		protected Integer firstResult;
		protected Integer maxResults;
		protected Map<String, String> parameterTypes;


		public Builder(String name) {
			super( name );
		}

		@Override
		protected NamedSqmQueryMemento.Builder getThis() {
			return this;
		}

		public NamedSqmQueryMemento.Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public NamedSqmQueryMemento.Builder addParameterType(String name, String typeName) {
			if ( this.parameterTypes == null ) {
				this.parameterTypes = new HashMap<>();
			}
			this.parameterTypes.put( name, typeName );
			return this;
		}

		public NamedSqmQueryMemento.Builder setParameterTypes(Map<String, String> parameterTypes) {
			this.parameterTypes = parameterTypes;
			return this;
		}

		public NamedSqmQueryMemento.Builder setLockOptions(LockOptions lockOptions) {
			this.lockOptions = lockOptions;
			return this;
		}

		public NamedSqmQueryMemento.Builder setFirstResult(Integer firstResult) {
			this.firstResult = firstResult;
			return this;
		}

		public NamedSqmQueryMemento.Builder setMaxResults(Integer maxResults) {
			this.maxResults = maxResults;
			return this;
		}

		public NamedSqmQueryMemento createNamedQueryDefinition() {
			return new NamedSqmQueryMementoImpl(
					name,
					sqmSelectStatement,
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
