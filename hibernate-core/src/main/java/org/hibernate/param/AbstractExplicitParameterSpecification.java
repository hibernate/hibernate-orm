/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.param;
import org.hibernate.type.Type;

/**
 * Convenience base class for explicitly defined query parameters.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractExplicitParameterSpecification implements ExplicitParameterSpecification {
	private final int sourceLine;
	private final int sourceColumn;
	private Type expectedType;

	/**
	 * Constructs an AbstractExplicitParameterSpecification.
	 *
	 * @param sourceLine See {@link #getSourceLine()}
	 * @param sourceColumn See {@link #getSourceColumn()} 
	 */
	protected AbstractExplicitParameterSpecification(int sourceLine, int sourceColumn) {
		this.sourceLine = sourceLine;
		this.sourceColumn = sourceColumn;
	}

	@Override
	public int getSourceLine() {
		return sourceLine;
	}

	@Override
	public int getSourceColumn() {
		return sourceColumn;
	}

	@Override
	public Type getExpectedType() {
		return expectedType;
	}

	@Override
	public void setExpectedType(Type expectedType) {
		this.expectedType = expectedType;
	}
}
