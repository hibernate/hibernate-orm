// $Id: NamedParameterSpecification.java 8513 2005-11-02 18:47:40Z steveebersole $
package org.hibernate.param;

import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TypedValue;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Relates to an explicit query named-parameter.
 *
 * @author Steve Ebersole
 */
public class NamedParameterSpecification extends AbstractExplicitParameterSpecification implements ParameterSpecification {

	private final String name;

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

	public String renderDisplayInfo() {
		return "name=" + name + ", expectedType=" + getExpectedType();
	}

	public String getName() {
		return name;
	}
}
