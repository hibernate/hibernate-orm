package org.hibernate.param;

import org.hibernate.type.Type;

/**
 * Convenience base class for explicitly defined query parameters.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public abstract class AbstractExplicitParameterSpecification implements ExplicitParameterSpecification {

	private final int sourceLine;
	private final int sourceColumn;
	private Type expectedType;

	protected AbstractExplicitParameterSpecification(int sourceLine, int sourceColumn) {
		this.sourceLine = sourceLine;
		this.sourceColumn = sourceColumn;
	}

	public int getSourceLine() {
		return sourceLine;
	}

	public int getSourceColumn() {
		return sourceColumn;
	}

	public Type getExpectedType() {
		return expectedType;
	}

	public void setExpectedType(Type expectedType) {
		this.expectedType = expectedType;
	}
}
