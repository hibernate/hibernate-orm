// $Id$
package org.hibernate.param;

import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parameter bind specification for an explicit  positional (or ordinal) parameter.
 *
 * @author Steve Ebersole
 */
public class PositionalParameterSpecification extends AbstractExplicitParameterSpecification implements ParameterSpecification {
	private final int hqlPosition;

	/**
	 * Constructs a position/ordinal parameter bind specification.
	 *
	 * @param sourceLine See {@link #getSourceLine()}
	 * @param sourceColumn See {@link #getSourceColumn()}
	 * @param hqlPosition The position in the source query, relative to the other source positional parameters.
	 */
	public PositionalParameterSpecification(int sourceLine, int sourceColumn, int hqlPosition) {
		super( sourceLine, sourceColumn );
		this.hqlPosition = hqlPosition;
	}

	/**
	 * Bind the appropriate value into the given statement at the specified position.
	 *
	 * @param statement The statement into which the value should be bound.
	 * @param qp The defined values for the current query execution.
	 * @param session The session against which the current execution is occuring.
	 * @param position The position from which to start binding value(s).
	 *
	 * @return The number of sql bind positions "eaten" by this bind operation.
	 */
	public int bind(PreparedStatement statement, QueryParameters qp, SessionImplementor session, int position) throws SQLException {
		Type type = qp.getPositionalParameterTypes()[hqlPosition];
		Object value = qp.getPositionalParameterValues()[hqlPosition];

		type.nullSafeSet( statement, value, position, session );
		return type.getColumnSpan( session.getFactory() );
	}

	/**
	 * {@inheritDoc}
	 */
	public String renderDisplayInfo() {
		return "ordinal=" + hqlPosition + ", expectedType=" + getExpectedType();
	}

	/**
	 * Getter for property 'hqlPosition'.
	 *
	 * @return Value for property 'hqlPosition'.
	 */
	public int getHqlPosition() {
		return hqlPosition;
	}
}
