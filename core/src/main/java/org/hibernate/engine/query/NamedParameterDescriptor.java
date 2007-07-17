package org.hibernate.engine.query;

import org.hibernate.type.Type;

import java.io.Serializable;

/**
 * Descriptor regarding a named parameter.
 *
 * @author Steve Ebersole
 */
public class NamedParameterDescriptor implements Serializable {
	private final String name;
	private final Type expectedType;
	private final int[] sourceLocations;
	private final boolean jpaStyle;

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
}
