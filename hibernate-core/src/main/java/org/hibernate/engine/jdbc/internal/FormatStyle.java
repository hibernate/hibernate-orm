/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
