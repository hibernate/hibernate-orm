/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import org.hibernate.dialect.Dialect;

/**
 * Additional contract for a {@link Type} that may appear as an SQL literal
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface LiteralType<T> {
	/**
	 * Convert the value into a string representation, suitable for embedding in an SQL statement as a
	 * literal.
	 *
	 * @param value The value to convert
	 * @param dialect The SQL dialect
	 *
	 * @return The value's string representation
	 * 
	 * @throws Exception Indicates an issue converting the value to literal string.
	 */
	public String objectToSQLString(T value, Dialect dialect) throws Exception;

}
