/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi;

import org.hibernate.SPI;
import org.hibernate.internal.util.QuotingHelper;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Appends complete SQL fragments to an in-flight SQL buffer. Dialect
/// strategies receive this contract from Hibernate and must not retain it.
///
/// @author Steve Ebersole
@FunctionalInterface
@SPI({ USE, IMPLEMENT })
public interface SqlAppender extends Appendable {
	String NO_SEPARATOR = "";
	String COMMA_SEPARATOR = ",";
	char COMMA_SEPARATOR_CHAR = ',';
	char WHITESPACE = ' ';

	char OPEN_PARENTHESIS = '(';
	char CLOSE_PARENTHESIS = ')';

	char PARAM_MARKER = '?';

	String NULL_KEYWORD = "null";

	/**
	 * Add the passed fragment into the in-flight buffer
	 */
	void appendSql(String fragment);

	default void appendSql(char fragment) {
		appendSql( Character.toString( fragment ) );
	}

	default void appendSql(int value) {
		appendSql( Integer.toString( value ) );
	}

	default void appendSql(long value) {
		appendSql( Long.toString( value ) );
	}

	default void appendSql(boolean value) {
		appendSql( String.valueOf( value ) );
	}

	default void appendSql(double value) {
		appendSql( String.valueOf( value ) );
	}

	default void appendSql(float value) {
		appendSql( String.valueOf( value ) );
	}

	default void appendDoubleQuoteEscapedString(String value) {
		final StringBuilder sb = new StringBuilder( value.length() + 2 );
		QuotingHelper.appendDoubleQuoteEscapedString( sb, value );
		appendSql( sb.toString() );
	}

	default void appendSingleQuoteEscapedString(String value) {
		final StringBuilder sb = new StringBuilder( value.length() + 2 );
		QuotingHelper.appendSingleQuoteEscapedString( sb, value );
		appendSql( sb.toString() );
	}

	default Appendable append(CharSequence csq) {
		appendSql( csq.toString() );
		return this;
	}

	default Appendable append(CharSequence csq, int start, int end) {
		appendSql( csq.toString().substring( start, end ) );
		return this;
	}

	default Appendable append(char c) {
		appendSql( Character.toString( c ) );
		return this;
	}

}
