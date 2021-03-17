/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.param;

import java.sql.PreparedStatement;

import org.hibernate.type.Type;

/**
 * Maintains information relating to parameters which need to get bound into a
 * JDBC {@link PreparedStatement}.
 *
 * @author Steve Ebersole
 */
public interface ParameterSpecification extends ParameterBinder {
	/**
	 * Get the type which we are expecting for a bind into this parameter based
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
