/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

/**
 * Access to appending SQL fragments to an in-flight buffer
 *
 * @author Steve Ebersole
 */
public interface SqlAppender {
	// todo (6.0) : add all the others sql keywords
	String SELECT_KEYWORD = "select ";
	String DISTINCT_KEYWORD = "distinct ";
	String COMA_SEPARATOR = ", ";
	String OPEN_PARENTHESYS = "(";
	String CLOSE_PARENTHESYS = ")";
	String NO_SEPARATOR = "";
	String EMPTY_STRING_SEPARATOR = " ";
	String FROM_KEYWORD = " from ";

	/**
	 * Add the passed fragment into the in-flight buffer
	 */
	void appendSql(String fragment);
}
