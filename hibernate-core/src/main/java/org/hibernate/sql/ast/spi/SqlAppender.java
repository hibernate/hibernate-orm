/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

/**
 * Access to appending SQL fragments to an in-flight buffer
 *
 * @author Steve Ebersole
 */
public interface SqlAppender {
	// todo (6.0) : add all the others sql keywords

	String COMA_SEPARATOR = ", ";
	String NO_SEPARATOR = "";
	String EMPTY_STRING = " ";

	String OPEN_PARENTHESIS = "(";
	String CLOSE_PARENTHESIS = ")";

	String SELECT_KEYWORD = "select";
	String DISTINCT_KEYWORD = "distinct";
	String ORDER_BY_KEYWORD = "order by";
	String COLLATE_KEYWORD = "collate";

	String FROM_KEYWORD = "from";
	String JOIN_KEYWORD = "join";
	String AS_KEYWORD = "as";
	String ON_KEYWORD = "on";

	String WHERE_KEYWORD = "where";

	String PARAM_MARKER = "?";

	String NOT_KEYWORD = "not";
	String IS_KEYWORD = "is";

	String NULL_KEYWORD = "null";
	String IS_NULL_FRAGMENT = IS_KEYWORD + EMPTY_STRING + NULL_KEYWORD;
	String IS_NOT_NULL_FRAGMENT = IS_KEYWORD + EMPTY_STRING + NOT_KEYWORD + EMPTY_STRING + NULL_KEYWORD;

	String AND_KEYWORD = "and";
	String OR_KEYWORD = "or";

	String LIKE_KEYWORD = "like";
	String ESCAPE_KEYWORD = "escape";

	String BETWEEN_KEYWORD = "between";

	String IN_KEYWORD = "in";

	String CASE_KEYWORD = "case";
	String WHEN_KEYWORD = "when";
	String THEN_KEYWORD = "then";
	String ELSE_KEYWORD = "else";
	String END_KEYWORD = "end";

	String ASC_KEYWORD = "asc";
	String DESC_KEYWORD = "desc";

	/**
	 * Add the passed fragment into the in-flight buffer
	 */
	void appendSql(String fragment);
}
