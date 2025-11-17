/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.internal.NamedHqlQueryDefinitionImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

/**
 * Boot-time descriptor of a named HQL query, as defined in
 * annotations or xml
 *
 * @see jakarta.persistence.NamedQuery
 * @see org.hibernate.annotations.NamedQuery
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedHqlQueryDefinition<E> extends NamedQueryDefinition<E> {
	String getHqlString();

	@Override
	NamedSqmQueryMemento<E> resolve(SessionFactoryImplementor factory);

	class Builder<E> extends AbstractNamedQueryBuilder<E, Builder<E>> {
		private String hqlString;

		private Integer firstResult;
		private Integer maxResults;

		private Map<String,String> parameterTypes;

		public Builder(String name, AnnotationTarget location) {
			super( name, location );
		}

		public Builder(String name) {
			super( name, null );
		}

		@Override
		protected Builder<E> getThis() {
			return this;
		}

		public String getHqlString() {
			return hqlString;
		}

		public Builder<E> setHqlString(String hqlString) {
			this.hqlString = hqlString;
			return this;
		}

		public Builder<E> setFirstResult(Integer firstResult) {
			this.firstResult = firstResult;
			return getThis();
		}

		public Builder<E> setMaxResults(Integer maxResults) {
			this.maxResults = maxResults;
			return getThis();
		}

		public NamedHqlQueryDefinitionImpl<E> build() {
			return new NamedHqlQueryDefinitionImpl<>(
					getName(),
					getResultClass(),
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
					getHints(),
					//TODO: should this be location.asClassDetails().getClassName() ?
					getLocation() == null ? null : getLocation().getName()
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
