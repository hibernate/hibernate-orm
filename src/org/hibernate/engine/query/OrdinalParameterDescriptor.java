package org.hibernate.engine.query;

import org.hibernate.type.Type;

import java.io.Serializable;

/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class OrdinalParameterDescriptor implements Serializable {
	private final int ordinalPosition;
	private final Type expectedType;
	private final int sourceLocation;

	public OrdinalParameterDescriptor(int ordinalPosition, Type expectedType, int sourceLocation) {
		this.ordinalPosition = ordinalPosition;
		this.expectedType = expectedType;
		this.sourceLocation = sourceLocation;
	}

	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	public Type getExpectedType() {
		return expectedType;
	}

	public int getSourceLocation() {
		return sourceLocation;
	}
}
