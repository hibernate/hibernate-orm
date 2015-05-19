/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import java.io.Serializable;

import org.hibernate.type.Type;

/**
 * Descriptor regarding a named parameter.
 *
 * @author Steve Ebersole
 */
public class NamedParameterDescriptor implements Serializable {
	private final String name;
	private Type expectedType;
	private final int[] sourceLocations;
	private final boolean jpaStyle;

	/**
	 * Constructs a NamedParameterDescriptor
	 *
	 * @param name The name of the parameter
	 * @param expectedType The expected type of the parameter, according to the translator
	 * @param sourceLocations The locations of the named parameters (aye aye aye)
	 * @param jpaStyle Was the parameter a JPA style "named parameter"?
	 */
	public NamedParameterDescriptor(String name, Type expectedType, int[] sourceLocations, boolean jpaStyle) {
		this.name = name;
		this.expectedType = expectedType;
		this.sourceLocations = sourceLocations;
		this.jpaStyle = jpaStyle;
	}

	public String getName() {
		return name;
	}

	public Type getExpectedType() {
		return expectedType;
	}

	public int[] getSourceLocations() {
		return sourceLocations;
	}

	public boolean isJpaStyle() {
		return jpaStyle;
	}

	/**
	 * Set the parameters expected type
	 *
	 * @param type The new expected type
	 */
	public void resetExpectedType(Type type) {
		this.expectedType = type;
	}
}
