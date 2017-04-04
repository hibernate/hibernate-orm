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
 * Maintains information relating to parameters which need to get bound into a
 * JDBC {@link PreparedStatement}.
 *
 * @author Steve Ebersole
 */
public interface ParameterSpecification {
	/**
	 * Bind the appropriate value into the given statement at the specified position.
	 *
	 * @param statement The statement into which the value should be bound.
	 * @param qp The defined values for the current query execution.
	 * @param session The session against which the current execution is occuring.
	 * @param position The position from which to start binding value(s).
	 *
	 * @return The number of sql bind positions "eaten" by this bind operation.
	 * @throws java.sql.SQLException Indicates problems performing the JDBC biind operation.
	 */
	int bind(PreparedStatement statement, QueryParameters qp, SharedSessionContractImplementor session, int position) throws SQLException;

	/**
	 * Get the type which we are expeting for a bind into this parameter based
	 * on translated contextual information.
	 *
	 * @return The expected type.
	 */
	Type getExpectedType();

	/**
	 * Injects the expected type.  Called during translation.
	 *
	 * @param expectedType The type to expect.
	 */
	void setExpectedType(Type expectedType);

	/**
	 * Render this parameter into displayable info (for logging, etc).
	 *
	 * @return The displayable info.
	 */
	String renderDisplayInfo();
}
