/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.internal.NamedHqlQueryDefinitionImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;

/**
 * Boot-time descriptor of a named HQL query, as defined in
 * annotations or xml
 *
 * @see javax.persistence.NamedQuery
 * @see org.hibernate.annotations.NamedQuery
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedHqlQueryDefinition extends NamedQueryDefinition {
	String getHqlString();

	@Override
	NamedHqlQueryMemento resolve(SessionFactoryImplementor factory);

	class Builder extends AbstractNamedQueryDefinition.AbstractBuilder<Builder> {
		private String hqlString;

		private Integer firstResult;
		private Integer maxResults;

		private Map<String,String> parameterTypes;

		public Builder(String name) {
			super( name );
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public String getHqlString() {
			return hqlString;
		}

		public Builder setHqlString(String hqlString) {
			this.hqlString = hqlString;
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
					getComment(),
					parameterTypes,
					getHints()
			);
		}

		public void addParameterTypeHint(String name, String type) {
			if ( parameterTypes == null ) {
				parameterTypes = new HashMap<>();
			}

			parameterTypes.put( name, type );
		}
	}
}
