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
package org.hibernate.metamodel.spi.relational;


import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * Models an identifier (name).
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class Identifier implements Comparable<Identifier> {
	private String text;
	private final boolean isQuoted;

	/**
	 * Means to generate an {@link Identifier} instance from its simple name
	 *
	 * @param text The text
	 *
	 * @return The identifier form of the name.
	 */
	public static Identifier toIdentifier(String text) {
		if ( StringHelper.isEmpty( text ) ) {
			return null;
		}
		final String trimmed = text.trim();
		if ( isQuoted( trimmed ) ) {
			final String bareName = trimmed.substring( 1, trimmed.length() - 1 );
			return new Identifier( bareName, true );
		}
		else {
			return new Identifier( trimmed, false );
		}
	}

	/**
	 * Means to generate an {@link Identifier} instance from its simple name
	 *
	 * @param text The name
	 *
	 * @return The identifier form of the name.
	 */
	public static Identifier toIdentifier(String text, boolean quote) {
		if ( StringHelper.isEmpty( text ) ) {
			return null;
		}
		final String trimmed = text.trim();
		if ( isQuoted( trimmed ) ) {
			final String bareName = trimmed.substring( 1, trimmed.length() - 1 );
			return new Identifier( bareName, quote );
		}
		else {
			return new Identifier( trimmed, quote );
		}
	}

	public static boolean isQuoted(String text) {
		return text.startsWith( "`" ) && text.endsWith( "`" );
	}

	/**
	 * Constructs an identifier instance.
	 *
	 * private access.  Use one of the static {@link #toIdentifier} forms instead to get a reference.
	 *
	 * @param text The identifier text.
	 * @param quoted Is this a quoted identifier?
	 */
	private Identifier(String text, boolean quoted) {
		if ( StringHelper.isEmpty( text ) ) {
			throw new IllegalIdentifierException( "Identifier text cannot be null" );
		}
		if ( isQuoted( text ) ) {
			throw new IllegalIdentifierException( "Identifier text should not contain quote markers (`)" );
		}
		this.text = text;
		this.isQuoted = quoted;
	}

	public boolean isEmpty() {
		return text.equals( "" );
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
	 * Get the identifier text
	 *
	 * @return The text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @deprecated Use {@link #getText} instead
	 */
	@Deprecated
	public String getName() {
		return text;
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
	public String getText(Dialect dialect) {
		return getText( dialect.openQuote(), dialect.closeQuote() );
	}

	/**
	 * If this is a quoted identifier, then return the identifier name
	 * enclosed in dialect-specific open- and end-quotes; otherwise,
	 * simply return the identifier name.
	 *
	 * @param openQuote The character to use as start quote
	 * @param closeQuote The character to use as end quote
	 * @return if quoted, identifier name enclosed in dialect-specific open- and end-quotes; otherwise, the
	 * identifier name.
	 */
	public String getText(char openQuote, char closeQuote) {
		return isQuoted ?
				String.valueOf( openQuote ) + text + closeQuote :
				text;
	}
	
	public String getQualifiedText(String prefix, Dialect dialect) {
		String qualified = prefix + "." + text;
		return isQuoted ? dialect.openQuote() + qualified + dialect.closeQuote() : qualified;
	}
	
	public String getUnqualifiedText(Dialect dialect) {
		int loc = text.lastIndexOf(".");
		String unqualified = ( loc < 0 ) ? text : text.substring( loc + 1 );
		return isQuoted ? dialect.openQuote() + unqualified + dialect.closeQuote() : unqualified;
	}
	
	public Identifier applyPostfix(String postfix) {
		String newText = text + postfix;
		return Identifier.toIdentifier( newText, isQuoted );
	}

	@Override
	public String toString() {
		return isQuoted
				? '`' + getText() + '`'
				: getText();
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
				&& isQuoted ? text.equals( that.text ) : text.equalsIgnoreCase( that.text );
	}

	@Override
	public int hashCode() {
		return isQuoted ? text.hashCode() : text.toUpperCase().hashCode();
	}

	@Override
	public int compareTo(Identifier o) {
		return text.compareTo( o.getText() );
	}
}
