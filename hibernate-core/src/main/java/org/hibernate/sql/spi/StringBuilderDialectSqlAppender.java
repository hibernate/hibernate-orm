package org.hibernate.sql.spi;

import org.hibernate.dialect.Dialect;

/**
 * Access to appending SQL fragments to a StringBuilder that has knowledge about the dialect
 */
public class StringBuilderDialectSqlAppender extends StringBuilderSqlAppender {

	protected final Dialect dialect;

	public StringBuilderDialectSqlAppender(Dialect dialect) {
		this( dialect, new StringBuilder() );
	}

	public StringBuilderDialectSqlAppender(Dialect dialect, StringBuilder sb) {
		super( sb );
		this.dialect = dialect;
	}

	@Override
	public void appendLiteral(String value) {
		dialect.getLiteralSupport().appendLiteral( this, value );
	}

	@Override
	public void appendLiteralContent(String value) {
		dialect.getLiteralSupport().appendLiteralContent( this, value );
	}
}
