/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.binding;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * Identifier generator definition, should be immutable.
 *
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
}
