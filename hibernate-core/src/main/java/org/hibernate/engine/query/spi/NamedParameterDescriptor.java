/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.query.spi;

import org.hibernate.Incubating;
import org.hibernate.type.BindableType;

/**
 * Descriptor regarding a named parameter.
 *
 * @author Steve Ebersole
 */
@Incubating
public class NamedParameterDescriptor<T> extends AbstractParameterDescriptor<T> {
	private final String name;

	/**
	 * Constructs a NamedParameterDescriptor
	 *
	 * @param name The name of the parameter
	 * @param expectedType The expected type of the parameter, according to the translator
	 * @param sourceLocations The locations of the named parameters (aye aye aye)
	 */
	public NamedParameterDescriptor(String name, BindableType<T> expectedType, int[] sourceLocations) {
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

		final NamedParameterDescriptor<?> that = (NamedParameterDescriptor<?>) o;
		return getName().equals( that.getName() );
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

}
