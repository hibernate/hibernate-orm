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
	String NO_SEPARATOR = "";
	String COMA_SEPARATOR = ", ";
	String EMPTY_STRING = " ";

	String OPEN_PARENTHESIS = "(";
	String CLOSE_PARENTHESIS = ")";

	String PARAM_MARKER = "?";

	String NULL_KEYWORD = "null";

	/**
	 * Add the passed fragment into the in-flight buffer
	 */
	void appendSql(String fragment);

	void appendSql(char fragment);

	default void appendQuoted(String value, char quoteChar) {
		appendSql( quoteChar );
		for ( int i = 0; i < value.length(); i++ ) {
			final char c = value.charAt( i );
			if ( c == quoteChar ) {
				appendSql( quoteChar );
			}
			appendSql( c );
		}
		appendSql( quoteChar );
	}
}
