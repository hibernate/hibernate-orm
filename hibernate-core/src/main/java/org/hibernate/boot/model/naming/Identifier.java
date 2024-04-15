/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * Models an identifier (name), which may or may not be quoted.
 *
 * @author Steve Ebersole
 */
public class Identifier implements Comparable<Identifier> {
	private final String text;
	private final boolean isQuoted;

	/**
	 * Means to generate an {@link Identifier} instance from its simple text form.
	 * <p/>
	 * If passed text is {@code null}, {@code null} is returned.
	 * <p/>
	 * If passed text is surrounded in quote markers, the generated Identifier
	 * is considered quoted.  Quote markers include back-ticks (`), and
	 * double-quotes (").
	 *
	 * @param text The text form
	 *
	 * @return The identifier form, or {@code null} if text was {@code null}
	 */
	public static Identifier toIdentifier(String text) {
		if ( StringHelper.isEmpty( text ) ) {
			return null;
		}
		final String trimmedText = text.trim();
		if ( isQuoted( trimmedText ) ) {
			final String bareName = trimmedText.substring( 1, trimmedText.length() - 1 );
			return new Identifier( bareName, true );
		}
		else {
			return new Identifier( trimmedText, false );
		}
	}

	/**
	 * Means to generate an {@link Identifier} instance from its simple text form.
	 * <p/>
	 * If passed text is {@code null}, {@code null} is returned.
	 * <p/>
	 * If passed text is surrounded in quote markers, the generated Identifier
	 * is considered quoted.  Quote markers include back-ticks (`), and
	 * double-quotes (").
	 *
	 * @param text The text form
	 * @param quote Whether to quote unquoted text forms
	 *
	 * @return The identifier form, or {@code null} if text was {@code null}
	 */
	public static Identifier toIdentifier(String text, boolean quote) {
		if ( StringHelper.isEmpty( text ) ) {
			return null;
		}
		final String trimmedText = text.trim();
		if ( isQuoted( trimmedText ) ) {
			final String bareName = trimmedText.substring( 1, trimmedText.length() - 1 );
			return new Identifier( bareName, true );
		}
		else {
			return new Identifier( trimmedText, quote );
		}
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
	 * @param text The text form
	 * @param quote Whether to quote unquoted text forms
	 * @param quoteOnNonIdentifierChar Controls whether to treat the result as quoted if text contains characters that are invalid for identifiers
	 *
	 * @return The identifier form, or {@code null} if text was {@code null}
	 */
	public static Identifier toIdentifier(String text, boolean quote, boolean quoteOnNonIdentifierChar) {
		if ( StringHelper.isEmpty( text ) ) {
			return null;
		}
		int start = 0;
		int end = text.length();
		while ( start < end ) {
			if ( !Character.isWhitespace( text.charAt( start ) ) ) {
				break;
			}
			start++;
		}
		while ( start < end ) {
			if ( !Character.isWhitespace( text.charAt( end - 1 ) ) ) {
				break;
			}
			end--;
		}
		if ( isQuoted( text, start, end ) ) {
			start++;
			end--;
			quote = true;
		}
		else if ( quoteOnNonIdentifierChar && !quote ) {
			// Check the letters to determine if we must quote the text
			char c = text.charAt( start );
			if ( !Character.isLetter( c ) && c != '_' ) {
				// SQL identifiers must begin with a letter or underscore
				quote = true;
			}
			else {
				for ( int i = start + 1; i < end; i++ ) {
					c = text.charAt( i );
					if ( !Character.isLetterOrDigit( c ) && c != '_' ) {
						quote = true;
						break;
					}
				}
			}
		}
		return new Identifier( text.substring( start, end ), quote );
	}

	/**
	 * Is the given identifier text considered quoted.  The following patterns are
	 * recognized as quoted:<ul>
	 *     <li>{@code `name`}</li>
	 *     <li>{@code [name]}</li>
	 *     <li>{@code "name"}</li>
	 * </ul>
	 * <p/>
	 * That final form using double-quote (") is the JPA-defined quoting pattern.  Although
	 * it is the standard, it makes for ugly declarations.
	 *
	 * @param name
	 *
	 * @return {@code true} if the given identifier text is considered quoted; {@code false} otherwise.
	 */
	public static boolean isQuoted(String name) {
		return ( name.startsWith( "`" ) && name.endsWith( "`" ) )
				|| ( name.startsWith( "[" ) && name.endsWith( "]" ) )
				|| ( name.startsWith( "\"" ) && name.endsWith( "\"" ) );
	}

	public static boolean isQuoted(String name, int start, int end) {
		if ( start + 2 < end ) {
			switch ( name.charAt( start ) ) {
				case '`':
					return name.charAt( end - 1 ) == '`';
				case '[':
					return name.charAt( end - 1 ) == ']';
				case '"':
					return name.charAt( end - 1 ) == '"';
			}
		}
		return false;
	}

	/**
	 * Constructs an identifier instance.
	 *
	 * @param text The identifier text.
	 * @param quoted Is this a quoted identifier?
	 */
	public Identifier(String text, boolean quoted) {
		if ( StringHelper.isEmpty( text ) ) {
			throw new IllegalIdentifierException( "Identifier text cannot be null" );
		}
		if ( isQuoted( text ) ) {
			throw new IllegalIdentifierException( "Identifier text should not contain quote markers (` or \")" );
		}
		this.text = text;
		this.isQuoted = quoted;
	}

	/**
	 * Constructs an unquoted identifier instance.
	 *
	 * @param text The identifier text.
	 */
	protected Identifier(String text) {
		this.text = text;
		this.isQuoted = false;
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
				? String.valueOf( dialect.openQuote() ) + getText() + dialect.closeQuote()
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
	public boolean equals(Object o) {
		if ( !(o instanceof Identifier) ) {
			return false;
		}

		final Identifier that = (Identifier) o;
		return getCanonicalName().equals( that.getCanonicalName() );
	}

	@Override
	public int hashCode() {
		return isQuoted ? text.hashCode() : text.toLowerCase( Locale.ENGLISH ).hashCode();
	}

	public static boolean areEqual(Identifier id1, Identifier id2) {
		if ( id1 == null ) {
			return id2 == null;
		}
		else {
			return id1.equals( id2 );
		}
	}

	public static Identifier quote(Identifier identifier) {
		return identifier.isQuoted()
				? identifier
				: Identifier.toIdentifier( identifier.getText(), true );
	}

	@Override
	public int compareTo(Identifier o) {
		return getCanonicalName().compareTo( o.getCanonicalName() );
	}
}
