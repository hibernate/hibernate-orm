/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

/**
 * Access to appending SQL fragments to a StringBuilder
 */
public class StringBuilderSqlAppender implements SqlAppender {

	private final StringBuilder sb;

	public StringBuilderSqlAppender() {
		this(new StringBuilder());
	}

	public StringBuilderSqlAppender(StringBuilder sb) {
		this.sb = sb;
	}

	@Override
	public void appendSql(String fragment) {
		sb.append( fragment );
	}

	@Override
	public void appendSql(char fragment) {
		sb.append( fragment );
	}

	@Override
	public void appendSql(int value) {
		sb.append( value );
	}

	@Override
	public void appendSql(long value) {
		sb.append( value );
	}

	@Override
	public void appendSql(boolean value) {
		sb.append( value );
	}

	@Override
	public Appendable append(CharSequence csq) {
		return sb.append( csq );
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) {
		return sb.append( csq, start, end );
	}

	@Override
	public Appendable append(char c) {
		return sb.append( c );
	}

	@Override
	public String toString() {
		return sb.toString();
	}
}
