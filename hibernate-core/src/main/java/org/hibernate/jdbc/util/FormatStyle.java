/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.jdbc.util;

/**
 * Represents the the understood types or styles of formatting. 
 *
 * @author Steve Ebersole
 */
public class FormatStyle {
	public static final FormatStyle BASIC = new FormatStyle( "basic", new BasicFormatterImpl() );
	public static final FormatStyle DDL = new FormatStyle( "ddl", new DDLFormatterImpl() );
	public static final FormatStyle NONE = new FormatStyle( "none", new NoFormatImpl() );

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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		FormatStyle that = ( FormatStyle ) o;

		return name.equals( that.name );

	}

	public int hashCode() {
		return name.hashCode();
	}

	private static class NoFormatImpl implements Formatter {
		public String format(String source) {
			return source;
		}
	}
}
