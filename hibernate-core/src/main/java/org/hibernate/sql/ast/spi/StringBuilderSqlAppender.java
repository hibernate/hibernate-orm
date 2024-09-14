/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.spi;

/**
 * Access to appending SQL fragments to a StringBuilder
 */
public class StringBuilderSqlAppender implements SqlAppender {

	protected final StringBuilder sb;

	public StringBuilderSqlAppender() {
		this(new StringBuilder());
	}

	public StringBuilderSqlAppender(StringBuilder sb) {
		this.sb = sb;
	}

	public StringBuilder getStringBuilder() {
		return sb;
	}

	@Override
	public void appendSql(String fragment) {
		append( fragment );
	}

	@Override
	public void appendSql(char fragment) {
		append( fragment );
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
		sb.append( csq );
		return this;
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) {
		sb.append( csq, start, end );
		return this;
	}

	@Override
	public Appendable append(char c) {
		sb.append( c );
		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}
}
