/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.relational;


import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * Models an identifier (name).
 *
 * @author Steve Ebersole
 */
public class Identifier {
	private final String name;
	private final boolean isQuoted;

	/**
	 * Means to generate an {@link Identifier} instance from its simple name
	 *
	 * @param name The name
	 *
	 * @return The identifier form of the name.
	 */
	public static Identifier toIdentifier(String name) {
		if ( StringHelper.isEmpty( name ) ) {
			return null;
		}
		final String trimmedName = name.trim();
		if ( isQuoted( trimmedName ) ) {
			final String bareName = trimmedName.substring( 1, trimmedName.length() - 1 );
			return new Identifier( bareName, true );
		}
		else {
			return new Identifier( trimmedName, false );
		}
	}

	public static boolean isQuoted(String name) {
		return name.startsWith( "`" ) && name.endsWith( "`" );
	}

	/**
	 * Constructs an identifier instance.
	 *
	 * @param name The identifier text.
	 * @param quoted Is this a quoted identifier?
	 */
	public Identifier(String name, boolean quoted) {
		if ( StringHelper.isEmpty( name ) ) {
			throw new IllegalIdentifierException( "Identifier text cannot be null" );
		}
		if ( isQuoted( name ) ) {
			throw new IllegalIdentifierException( "Identifier text should not contain quote markers (`)" );
		}
		this.name = name;
		this.isQuoted = quoted;
	}

	/**
	 * Get the identifiers name (text)
	 *
	 * @return The name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Is this a quoted identifier>
	 *
	 * @return True if this is a quote identifier; false otherwise.
	 */
	public boolean isQuoted() {
		return isQuoted;
	}

	/**
	 * If this is a quoted identifier, then return the identifier name
	 * enclosed in dialect-specific open- and end-quotes; otherwise,
	 * simply return the identifier name.
	 *
	 * @param dialect The dialect whose dialect-specific quoting should be used.
	 * @return if quoted, identifier name enclosed in dialect-specific open- and end-quotes; otherwise, the
	 * identifier name.
	 */
	public String encloseInQuotesIfQuoted(Dialect dialect) {
		return isQuoted ?
				new StringBuilder( name.length() + 2 )
						.append( dialect.openQuote() )
						.append( name )
						.append( dialect.closeQuote() )
						.toString() :
				name;
	}

	@Override
	public String toString() {
		return isQuoted
				? '`' + getName() + '`'
				: getName();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Identifier that = (Identifier) o;

		return isQuoted == that.isQuoted
				&& name.equals( that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
