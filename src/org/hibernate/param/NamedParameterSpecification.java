// $Id$
package org.hibernate.param;

import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TypedValue;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parameter bind specification for an explicit named parameter.
 *
 * @author Steve Ebersole
 */
public class NamedParameterSpecification extends AbstractExplicitParameterSpecification implements ParameterSpecification {
	private final String name;

	/**
	 * Constructs a named parameter bind specification.
	 *
	 * @param sourceLine See {@link #getSourceLine()}
	 * @param sourceColumn See {@link #getSourceColumn()} 
	 * @param name The named parameter name.
	 */
	public NamedParameterSpecification(int sourceLine, int sourceColumn, String name) {
		super( sourceLine, sourceColumn );
		this.name = name;
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
	public int bind(PreparedStatement statement, QueryParameters qp, SessionImplementor session, int position)
	        throws SQLException {
		TypedValue typedValue = ( TypedValue ) qp.getNamedParameters().get( name );
		typedValue.getType().nullSafeSet( statement, typedValue.getValue(), position, session );
		return typedValue.getType().getColumnSpan( session.getFactory() );
	}

	/**
	 * {@inheritDoc}
	 */
	public String renderDisplayInfo() {
		return "name=" + name + ", expectedType=" + getExpectedType();
	}

	/**
	 * Getter for property 'name'.
	 *
	 * @return Value for property 'name'.
	 */
	public String getName() {
		return name;
	}
}
