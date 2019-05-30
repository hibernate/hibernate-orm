/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.internal.NamedNativeQueryDefinitionImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

/**
 * Boot-time descriptor of a named native query, as defined in
 * annotations or xml
 *
 * @see javax.persistence.NamedNativeQuery
 * @see org.hibernate.annotations.NamedNativeQuery
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedNativeQueryDefinition extends NamedQueryDefinition {
	String getSqlQueryString();

	String getResultSetMappingName();
	String getResultSetMappingClassName();

	@Override
	NamedNativeQueryMemento resolve(SessionFactoryImplementor factory);

	class Builder extends AbstractNamedQueryDefinition.AbstractBuilder<Builder> {
		private String sqlString;

		private String resultSetMappingName;
		private String resultSetMappingClassName;

		private Set<String> querySpaces;

		private Map<String,String> parameterTypes;

		public Builder(String name) {
			super( name );
		}

		public Builder setSqlString(String sqlString) {
			this.sqlString = sqlString;
			return getThis();
		}

		public NamedNativeQueryDefinition build() {
			return new NamedNativeQueryDefinitionImpl(
					getName(),
					sqlString,
					resultSetMappingName,
					resultSetMappingClassName,
					getQuerySpaces(),
					getCacheable(),
					getCacheRegion(),
					getCacheMode(),
					getFlushMode(),
					getReadOnly(),
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

		public Set<String> getQuerySpaces() {
			return querySpaces;
		}

		public Builder addSynchronizedQuerySpaces(Set<String> querySpaces) {
			if ( querySpaces == null || querySpaces.isEmpty() ) {
				return this;
			}

			if ( this.querySpaces == null ) {
				this.querySpaces = new HashSet<>();
			}

			this.querySpaces.addAll( querySpaces );

			return getThis();
		}

		public Builder addSynchronizedQuerySpace(String space) {
			if ( this.querySpaces == null ) {
				this.querySpaces = new HashSet<>();
			}
			this.querySpaces.add( space );
			return getThis();
		}

		public Builder setQuerySpaces(Set<String> spaces) {
			this.querySpaces = spaces;
			return this;
		}

		public Builder setResultSetMappingName(String resultSetMappingName) {
			this.resultSetMappingName = resultSetMappingName;
			return this;
		}

		public Builder setResultSetMappingClassName(String resultSetMappingClassName) {
			this.resultSetMappingClassName = resultSetMappingClassName;
			return this;
		}

		public void addParameterTypeHint(String name, String type) {
			if ( parameterTypes == null ) {
				parameterTypes = new HashMap<>();
			}

			parameterTypes.put( name, type );
		}
	}
}
