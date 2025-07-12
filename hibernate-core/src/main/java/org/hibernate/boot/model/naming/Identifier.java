/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.util.Locale;
import java.util.Objects;

import org.hibernate.dialect.Dialect;

import static java.lang.Character.isLetter;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isWhitespace;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Models an identifier (name), which may or may not be quoted.
 *
 * @author Steve Ebersole
 */
public class Identifier implements Comparable<Identifier> {
	private final String text;
	private final boolean isQuoted;
	private final boolean isExplicit;

	/**
	 * Means to generate an {@link Identifier} instance from its simple text form.
	 * <p>
	 * If passed text is {@code null}, {@code null} is returned.
	 * <p>
	 * If passed text is surrounded in quote markers, the generated Identifier
	 * is considered quoted.  Quote markers include back-ticks (`),
	 * double-quotes (") and brackets ([ and ]).
	 *
	 * If the text, after trimming, contains a character that is not a valid identifier character,
	 * the identifier is treated as quoted.
	 *
	 * @param text The text form
	 *
	 * @return The identifier form, or {@code null} if text was {@code null}
	 */
	public static Identifier toIdentifier(String text) {
		return toIdentifier( text, false );
	}

	/**
	 * Means to generate an {@link Identifier} instance from its simple text form.
	 * <p>
	 * If passed text is {@code null}, {@code null} is returned.
	 * <p>
	 * If passed text is surrounded in quote markers, the generated Identifier
	 * is considered quoted.  Quote markers include back-ticks (`),
	 * double-quotes (") and brackets ([ and ]).
	 *
	 * If the text, after trimming, contains a character that is not a valid identifier character,
	 * the identifier is treated as quoted.
	 *
	 * @param text The text form
	 * @param quote Whether to quote unquoted text forms
	 *
	 * @return The identifier form, or {@code null} if text was {@code null}
	 */
	public static Identifier toIdentifier(String text, boolean quote) {
		return toIdentifier( text, quote, true, false );
	}

	/**
	 * Means to generate an {@link Identifier} instance from its simple text form.
	 * <p>
	 * If passed {@code text} is {@code null}, {@code null} is returned.
	 * <p>
	 * If passed {@code text} is surrounded in quote markers, the returned Identifier
	 * is considered quoted. Quote markers include back-ticks (`), double-quotes ("),
	 * and brackets ([ and ]).
	 *
	 * @param text The text form
	 * @param quote Whether to quote unquoted text forms
	 * @param autoquote Whether to quote the result if it contains special characters
	 *
	 * @return The identifier form, or {@code null} if text was {@code null}
	 */
	public static Identifier toIdentifier(String text, boolean quote, boolean autoquote) {
		return toIdentifier( text, quote, autoquote, false );
	}

	/**
	 * Means to generate an {@link Identifier} instance from its simple text form.
	 * <p>
	 * If passed {@code text} is {@code null}, {@code null} is returned.
	 * <p>
	 * If passed {@code text} is surrounded in quote markers, the returned Identifier
	 * is considered quoted. Quote markers include back-ticks (`), double-quotes ("),
	 * and brackets ([ and ]).
	 *
	 * @param text The text form
	 * @param quote Whether to quote unquoted text forms
	 * @param autoquote Whether to quote the result if it contains special characters
	 * @param isExplicit Whether the name is explicitly set
	 * @return The identifier form, or {@code null} if text was {@code null}
	 */
	public static Identifier toIdentifier(String text, boolean quote, boolean autoquote, boolean isExplicit) {
		if ( isBlank( text ) ) {
			return null;
		}
		int start = 0;
		int end = text.length();
		while ( start < end ) {
			if ( !isWhitespace( text.charAt( start ) ) ) {
				break;
			}
			start++;
		}
		while ( start < end ) {
			if ( !isWhitespace( text.charAt( end - 1 ) ) ) {
				break;
			}
			end--;
		}
		if ( isQuoted( text, start, end ) ) {
			start++;
			end--;
			quote = true;
		}
		else if ( autoquote && !quote ) {
			quote = autoquote( text, start, end );
		}
		return new Identifier( text.substring( start, end ), quote, isExplicit );
	}

	private static boolean autoquote(String text, int start, int end) {
		// Check the letters to determine if we must quote the text
		if ( !isLegalFirstChar( text.charAt( start ) ) ) {
			// SQL identifiers must begin with a letter or underscore
			return true;
		}
		else {
			for ( int i = start + 1; i < end; i++ ) {
				if ( !isLegalChar( text.charAt( i ) ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isLegalChar(char current) {
		return isLetterOrDigit( current )
			// every database also allows _ here
			|| current == '_'
			// every database except HSQLDB also allows $ here
			|| current == '$';
	}

	private static boolean isLegalFirstChar(char first) {
		return isLetter( first )
			// many databases also allow _ here
			|| first == '_';
	}

	/**
	 * Is the given identifier text considered quoted.  The following patterns are
	 * recognized as quoted:<ul>
	 *     <li>{@code `name`}</li>
	 *     <li>{@code [name]}</li>
	 *     <li>{@code "name"}</li>
	 * </ul>
	 * <p>
	 * That final form using double-quote (") is the JPA-defined quoting pattern.  Although
	 * it is the standard, it makes for ugly declarations.
	 *
	 * @return {@code true} if the given identifier text is considered quoted; {@code false} otherwise.
	 */
	public static boolean isQuoted(String name) {
		return isQuoted( name, 0, name.length() );
	}

	public static boolean isQuoted(String name, int start, int end) {
		if ( start + 2 < end ) {
			final char first = name.charAt( start );
			final char last = name.charAt( end - 1 );
			return switch ( first ) {
				case '`' -> last == '`';
				case '[' -> last == ']';
				case '"' -> last == '"';
				default -> false;
			};
		}
		else {
			return false;
		}
	}

	public static String unQuote(String name) {
		assert isQuoted( name );
		return name.substring( 1, name.length() - 1 );
	}

	/**
	 * Constructs an identifier instance.
	 *
	 * @param text The identifier text.
	 * @param quoted Is this a quoted identifier?
	 */
	public Identifier(String text, boolean quoted) {
		this( text, quoted, false );
	}

	/**
	 * Constructs an identifier instance.
	 *
	 * @param text The identifier text.
	 * @param quoted Is this a quoted identifier?
	 * @param isExplicit Whether the name is explicitly set
	 */
	public Identifier(String text, boolean quoted, boolean isExplicit) {
		if ( isEmpty( text ) ) {
			throw new IllegalIdentifierException( "Identifier text cannot be null" );
		}
		if ( isQuoted( text ) ) {
			throw new IllegalIdentifierException( "Identifier text should not contain quote markers (` or \")" );
		}
		this.text = text;
		this.isQuoted = quoted;
		this.isExplicit = isExplicit;
	}

	/**
	 * Constructs an unquoted identifier instance.
	 *
	 * @param text The identifier text.
	 */
	protected Identifier(String text) {
		this.text = text;
		this.isQuoted = false;
		this.isExplicit = false;
	}

	/**
	 * Get the identifiers name (text)
	 *
	 * @return The name
	 */
	public String getText() {
		return text;
	}

	/**
	 * Is this a quoted identifier?
	 *
	 * @return True if this is a quote identifier; false otherwise.
	 */
	public boolean isQuoted() {
		return isQuoted;
	}

	/**
	 * A quoted form of this identifier.
	 */
	public Identifier quoted() {
		return isQuoted ? this : toIdentifier( text, true );
	}

	/**
	 * If this is a quoted identifier, then return the identifier name
	 * enclosed in dialect-specific open- and end-quotes; otherwise,
	 * simply return the unquoted identifier.
	 *
	 * @param dialect The dialect whose dialect-specific quoting should be used.
	 *
	 * @return if quoted, identifier name enclosed in dialect-specific open- and
	 * end-quotes; otherwise, the unquoted identifier.
	 */
	public String render(Dialect dialect) {
		return isQuoted
				? dialect.toQuotedIdentifier( getText() )
				: getText();
	}

	public String render() {
		return isQuoted
				? '`' + getText() + '`'
				: getText();
	}

	public String getCanonicalName() {
		return isQuoted ? text : text.toLowerCase( Locale.ENGLISH );
	}

	@Override
	public String toString() {
		return render();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof Identifier that
			&& getCanonicalName().equals( that.getCanonicalName() );
	}

	public boolean matches(String name) {
		return isQuoted()
				? text.equals( name )
				: text.equalsIgnoreCase( name );
	}

	@Override
	public int hashCode() {
		return isQuoted
				? text.hashCode()
				: text.toLowerCase( Locale.ENGLISH ).hashCode();
	}

	@Override
	public int compareTo(Identifier identifier) {
		return getCanonicalName().compareTo( identifier.getCanonicalName() );
	}

	public static boolean areEqual(Identifier id1, Identifier id2) {
		return Objects.equals( id1, id2 );
	}

	/**
	 * @deprecated Use {@link #quoted()}.
	 */
	@Deprecated(since = "7.1", forRemoval = true)
	public static Identifier quote(Identifier identifier) {
		return identifier.quoted();
	}

	public boolean isExplicit() {
		return isExplicit;
	}
}
