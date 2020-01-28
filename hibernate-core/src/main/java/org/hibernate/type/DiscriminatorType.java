/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;


import org.hibernate.dialect.Dialect;

/**
 * Additional contract for a {@link Type} may be used for a discriminator.
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface DiscriminatorType<T> extends IdentifierType<T>, LiteralType<T> {
	/**
	 * Render the given discriminator value to a literal format
	 * for embedding in the generated SQL.
	 *
	 * @param value The value to convert
	 * @param dialect The SQL dialect
	 *
	 * @return The value's SQL literal representation
	 */
	@Override
	String objectToSQLString(T value, Dialect dialect) throws Exception;
}
