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
import org.hibernate.engine.spi.TypedValue;

/**
 * Parameter bind specification for an explicit named parameter.
 *
 * @author Steve Ebersole
 */
public class NamedParameterSpecification extends AbstractExplicitParameterSpecification {
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
	@Override
	public int bind(
			PreparedStatement statement,
			QueryParameters qp,
			SharedSessionContractImplementor session,
			int position) throws SQLException {
		TypedValue typedValue = qp.getNamedParameters().get( name );
		typedValue.getType().nullSafeSet( statement, typedValue.getValue(), position, session );
		return typedValue.getType().getColumnSpan( session.getFactory() );
	}

	@Override
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
