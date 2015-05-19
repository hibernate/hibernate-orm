/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

/**
 * Represents the the understood types or styles of formatting. 
 *
 * @author Steve Ebersole
 */
public enum FormatStyle {
	/**
	 * Formatting for SELECT, INSERT, UPDATE and DELETE statements
	 */
	BASIC( "basic", new BasicFormatterImpl() ),
	/**
	 * Formatting for DDL (CREATE, ALTER, DROP, etc) statements
	 */
	DDL( "ddl", DDLFormatterImpl.INSTANCE ),
	/**
	 * No formatting
	 */
	NONE( "none", NoFormatImpl.INSTANCE );

	private final String name;
	private final Formatter formatter;

	private FormatStyle(String name, Formatter formatter) {
		this.name = name;
		this.formatter = formatter;
	}

	public String getName() {
		return name;
	}

	public Formatter getFormatter() {
		return formatter;
	}

	private static class NoFormatImpl implements Formatter {
		/**
		 * Singleton access
		 */
		public static final NoFormatImpl INSTANCE = new NoFormatImpl();

		public String format(String source) {
			return source;
		}
	}
}
