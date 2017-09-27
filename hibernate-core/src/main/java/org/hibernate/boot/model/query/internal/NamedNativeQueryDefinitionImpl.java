/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.query.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.boot.model.query.spi.NamedNativeQueryDefinition;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;

/**
 * @author Steve Ebersole
 */
public class NamedNativeQueryDefinitionImpl extends AbstractNamedQueryDefinition implements NamedNativeQueryDefinition {
	private final String sqlString;

	public NamedNativeQueryDefinitionImpl(String name, String sqlString) {
		super( name );
		this.sqlString = sqlString;
	}

	@Override
	public String getQueryString() {
		return sqlString;
	}

	public static class Builder extends AbstractBuilder<Builder> {
		private final String sqlString;

		private String resultSetMapping;
		private List<ResultSetMappingDefinition.Result> resultDefinitions;

		public Builder(String name, String sqlString) {
			super( name );
			this.sqlString = sqlString;
		}

		public NamedNativeQueryDefinitionImpl build() {
			return new NamedNativeQueryDefinitionImpl( getName(), sqlString );
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public Builder setResultSetMapping(String resultSetMapping) {
			this.resultSetMapping = resultSetMapping;
			return this;
		}

		public Builder addResult(ResultSetMappingDefinition.Result resultDefinition) {
			if ( resultDefinitions == null ) {
				resultDefinitions = new ArrayList<>();
			}

			resultDefinitions.add ( resultDefinition );

			return this;
		}
	}
}
