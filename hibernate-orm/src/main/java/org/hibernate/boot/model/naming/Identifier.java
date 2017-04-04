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
	 * @return
	 */
	public static boolean isQuoted(String name) {
		return ( name.startsWith( "`" ) && name.endsWith( "`" ) )
				|| ( name.startsWith( "[" ) && name.endsWith( "]" ) )
				|| ( name.startsWith( "\"" ) && name.endsWith( "\"" ) );
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
	 * Get the identifiers name (text)
	 *
	 * @return The name
	 */
	public String getText() {
		return text;
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
