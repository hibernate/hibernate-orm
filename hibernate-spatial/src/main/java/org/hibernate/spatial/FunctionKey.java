/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial;

import java.util.Objects;
import java.util.Optional;

/**
 * A the key vor a SpatialFunction
 */
public class FunctionKey {

	final private String name;
	final private String altName;

	private FunctionKey(String name, String altName) {
		this.name = name;
		this.altName = altName;
	}

	public static FunctionKey apply(String name, String altName) {
		return new FunctionKey( name, altName );
	}

	public static FunctionKey apply(String name) {
		return new FunctionKey( name, null );
	}

	public String getName() {
		return name;
	}

	public Optional<String> getAltName() {
		return Optional.ofNullable( altName );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		FunctionKey that = (FunctionKey) o;
		return Objects.equals( name, that.name ) && Objects.equals( altName, that.altName );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, altName );
	}

	@Override
	public String toString() {
		return "SpatialFunctionKey{" + name + '}';
	}
}
