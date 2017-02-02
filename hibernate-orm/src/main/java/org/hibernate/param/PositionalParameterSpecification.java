/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.param;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.Type;

/**
 * Parameter bind specification for an explicit  positional (or ordinal) parameter.
 *
 * @author Steve Ebersole
 */
public class PositionalParameterSpecification extends AbstractExplicitParameterSpecification  {
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
	@Override
	public int bind(PreparedStatement statement, QueryParameters qp, SharedSessionContractImplementor session, int position) throws SQLException {
		Type type = qp.getPositionalParameterTypes()[hqlPosition];
		Object value = qp.getPositionalParameterValues()[hqlPosition];

		type.nullSafeSet( statement, value, position, session );
		return type.getColumnSpan( session.getFactory() );
	}

	@Override
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
