/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import org.hibernate.Incubating;
import org.hibernate.type.Type;

/**
 * Descriptor regarding a named parameter.
 *
 * @author Steve Ebersole
 */
@Incubating
public class NamedParameterDescriptor extends AbstractParameterDescriptor {
	private final String name;

	/**
	 * Constructs a NamedParameterDescriptor
	 *
	 * @param name The name of the parameter
	 * @param expectedType The expected type of the parameter, according to the translator
	 * @param sourceLocations The locations of the named parameters (aye aye aye)
	 */
	public NamedParameterDescriptor(String name, Type expectedType, int[] sourceLocations) {
		super( sourceLocations, expectedType );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		NamedParameterDescriptor that = (NamedParameterDescriptor) o;
		return getName().equals( that.getName() );
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
