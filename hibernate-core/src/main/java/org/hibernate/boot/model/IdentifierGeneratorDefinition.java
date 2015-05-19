/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * Identifier generator definition, should be immutable.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 * @author Strong Liu
 */
public class IdentifierGeneratorDefinition implements Serializable {
	private final String name;
	private final String strategy;
	private final Map<String, String> parameters;

	public IdentifierGeneratorDefinition(
			final String name,
			final String strategy,
			final Map<String, String> parameters) {
		this.name = name;
		this.strategy = strategy;
		if ( CollectionHelper.isEmpty( parameters ) ) {
			this.parameters = Collections.emptyMap();
		}
		else {
			this.parameters = Collections.unmodifiableMap( parameters );
		}
	}

	public IdentifierGeneratorDefinition(String name) {
		this( name, name );
	}

	public IdentifierGeneratorDefinition(String name, String strategy) {
		this.name = name;
		this.strategy = strategy;
		this.parameters = Collections.emptyMap();
	}

	/**
	 * @return identifier generator strategy
	 */
	public String getStrategy() {
		return strategy;
	}

	/**
	 * @return generator name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return generator configuration parameters
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof IdentifierGeneratorDefinition ) ) {
			return false;
		}

		IdentifierGeneratorDefinition that = (IdentifierGeneratorDefinition) o;

		if ( name != null ? !name.equals( that.name ) : that.name != null ) {
			return false;
		}
		if ( parameters != null ? !parameters.equals( that.parameters ) : that.parameters != null ) {
			return false;
		}
		if ( strategy != null ? !strategy.equals( that.strategy ) : that.strategy != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( strategy != null ? strategy.hashCode() : 0 );
		result = 31 * result + ( parameters != null ? parameters.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "IdentifierGeneratorDefinition{" +
				"name='" + name + '\'' +
				", strategy='" + strategy + '\'' +
				", parameters=" + parameters +
				'}';
	}

	public static class Builder {
		private String name;
		private String strategy;
		private Map<String, String> parameters;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getStrategy() {
			return strategy;
		}

		public void setStrategy(String strategy) {
			this.strategy = strategy;
		}

		public void addParam(String name, String value) {
			parameters().put( name, value );
		}

		private Map<String, String> parameters() {
			if ( parameters == null ) {
				parameters = new HashMap<String, String>();
			}
			return parameters;
		}

		public void addParams(Map<String,String> parameters) {
			parameters().putAll( parameters );
		}

		public IdentifierGeneratorDefinition build() {
			return new IdentifierGeneratorDefinition( name, strategy, parameters );
		}
	}
}
