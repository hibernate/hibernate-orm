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
 * Descriptor regarding an ordinal parameter.
 *
 * @author Steve Ebersole
 */
public class OrdinalParameterDescriptor implements Serializable {
	private final int ordinalPosition;
	private final Type expectedType;
	private final int sourceLocation;

	/**
	 * Constructs an ordinal parameter descriptor.
	 *
	 * @param ordinalPosition The ordinal position
	 * @param expectedType The expected type of the parameter
	 * @param sourceLocation The location of the parameter
	 */
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
