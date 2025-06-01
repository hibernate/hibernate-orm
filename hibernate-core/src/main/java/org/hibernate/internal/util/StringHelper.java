/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.hibernate.dialect.Dialect;
import org.hibernate.loader.internal.AliasConstantsHelper;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.Character.isDigit;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isLetter;
import static java.lang.Character.isWhitespace;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;

public final class StringHelper {

	private static final int ALIAS_TRUNCATE_LENGTH = 10;
	public static final String WHITESPACE = " \n\r\f\t";
	public static final String[] EMPTY_STRINGS = EMPTY_STRING_ARRAY;

	private static final Pattern COMMA_SEPARATED_PATTERN = Pattern.compile( "\\s*,\\s*" );

	private StringHelper() { /* static methods only - hide constructor */
	}

	public static int lastIndexOfLetter(String string) {
		for ( int i = 0; i < string.length(); i++ ) {
			final char character = string.charAt( i );
			// Include "_".  See HHH-8073
			if ( !isLetter( character ) && !( '_' == character ) ) {
				return i - 1;
			}
		}
		return string.length() - 1;
	}

	public static String join(String separator, String[] strings) {
		final int length = strings.length;
		if ( length == 0 ) {
			return "";
		}
		else {
			// Allocate space for length * firstStringLength;
			// If strings[0] is null, then its length is defined as 4, since that's the
			// length of "null".
			final int firstStringLength = strings[0] != null ? strings[0].length() : 4;
			final StringBuilder buf =
					new StringBuilder( length * firstStringLength )
							.append( strings[0] );
			for ( int i = 1; i < length; i++ ) {
				buf.append( separator ).append( strings[i] );
			}
			return buf.toString();
		}
	}

	public static String join(String separator, Object[] values) {
		final int length = values.length;
		if ( length == 0 ) {
			return "";
		}
		else {
			// Allocate space for length * firstStringLength;
			// If strings[0] is null, then its length is defined as 4, since that's the
			// length of "null".
			final int firstStringLength = values[0] != null ? values[0].toString().length() : 4;
			final StringBuilder buf =
					new StringBuilder( length * firstStringLength )
							.append( values[0] );
			for ( int i = 1; i < length; i++ ) {
				buf.append( separator ).append( values[i] );
			}
			return buf.toString();
		}
	}

	public static String join(String separator, Iterable<?> objects) {
		return join( separator, objects.iterator() );
	}

	public static String join(String separator, Iterator<?> objects) {
		final StringBuilder buf = new StringBuilder();
		if ( objects.hasNext() ) {
			buf.append( objects.next() );
		}
		while ( objects.hasNext() ) {
			buf.append( separator ).append( objects.next() );
		}
		return buf.toString();
	}

	public static String joinWithQualifierAndSuffix(
			String[] values,
			String qualifier,
			String suffix,
			String deliminator) {
		final int length = values.length;
		if ( length == 0 ) {
			return "";
		}
		final StringBuilder buf =
				new StringBuilder( length * ( values[0].length() + suffix.length() ) )
						.append( qualify( qualifier, values[0] ) ).append( suffix );
		for ( int i = 1; i < length; i++ ) {
			buf.append( deliminator ).append( qualify( qualifier, values[i] ) ).append( suffix );
		}
		return buf.toString();
	}

	public static String[] add(String[] x, String sep, String[] y) {
		final String[] result = new String[x.length];
		for ( int i = 0; i < x.length; i++ ) {
			result[i] = x[i] + sep + y[i];
		}
		return result;
	}

	public static String repeat(String string, int times) {
		return string.repeat( Math.max( 0, times ) );
	}

	public static String repeat(String string, int times, String deliminator) {
		final StringBuilder buf =
				new StringBuilder( string.length() * times + deliminator.length() * ( times - 1 ) )
						.append( string );
		for ( int i = 1; i < times; i++ ) {
			buf.append( deliminator ).append( string );
		}
		return buf.toString();
	}

	public static String repeat(char character, int times) {
		final char[] buffer = new char[times];
		Arrays.fill( buffer, character );
		return new String( buffer );
	}

	public static void repeat(String string, int times, String separator, StringBuilder buffer) {
		buffer.append( string );
		for ( int i = 1; i < times; i++ ) {
			buffer.append( separator ).append( string );
		}
	}

	public static String replace(String template, String placeholder, String replacement) {
		return replace( template, placeholder, replacement, false );
	}

	public static String[] replace(String[] templates, String placeholder, String replacement) {
		final String[] result = new String[templates.length];
		for ( int i = 0; i < templates.length; i++ ) {
			result[i] = replace( templates[i], placeholder, replacement );
		}
		return result;
	}

	public static String replace(String template, String placeholder, String replacement, boolean wholeWords) {
		return replace( template, placeholder, replacement, wholeWords, false );
	}

	public static String replace(
			String template,
			String placeholder,
			String replacement,
			boolean wholeWords,
			boolean encloseInParensIfNecessary) {
		if ( template == null ) {
			return null;
		}
		final int loc = indexOfPlaceHolder( template, placeholder, wholeWords );
		if ( loc < 0 ) {
			return template;
		}
		else {
			final String beforePlaceholder = template.substring( 0, loc );
			final String afterPlaceholder = template.substring( loc + placeholder.length() );
			return replace(
					beforePlaceholder,
					afterPlaceholder,
					placeholder,
					replacement,
					wholeWords,
					encloseInParensIfNecessary
			);
		}
	}

	public static String replace(
			String beforePlaceholder,
			String afterPlaceholder,
			String placeholder,
			String replacement,
			boolean wholeWords,
			boolean encloseInParensIfNecessary) {
		final boolean actuallyReplace =
				!wholeWords
						|| afterPlaceholder.isEmpty()
						|| !isJavaIdentifierPart( afterPlaceholder.charAt( 0 ) );
		// We only need to check the left param to determine if the placeholder is already
		// enclosed in parentheses (HHH-10383)
		// Examples:
		// 1) "... IN (?1", we assume that "?1" does not need to be enclosed because there
		// is already a right-parenthesis; we assume there will be a matching right-parenthesis.
		// 2) "... IN ?1", we assume that "?1" needs to be enclosed in parentheses, because there
		// is no left-parenthesis.

		// We need to check the placeholder is not used in `Order By FIELD(...)` (HHH-10502)
		// Examples:
		// " ... Order By FIELD(id,?1)",  after expand parameters, the sql is "... Order By FIELD(id,?,?,?)"
		final boolean encloseInParens =
				actuallyReplace
						&& encloseInParensIfNecessary
						&& !( getLastNonWhitespaceCharacter( beforePlaceholder ) == '(' )
						&& !( getLastNonWhitespaceCharacter( beforePlaceholder ) == ','
							&& getFirstNonWhitespaceCharacter( afterPlaceholder ) == ')' );
		final StringBuilder buf = new StringBuilder( beforePlaceholder );
		if ( encloseInParens ) {
			buf.append( '(' );
		}
		buf.append( actuallyReplace ? replacement : placeholder );
		if ( encloseInParens ) {
			buf.append( ')' );
		}
		buf.append(
				replace(
						afterPlaceholder,
						placeholder,
						replacement,
						wholeWords,
						encloseInParensIfNecessary
				)
		);
		return buf.toString();
	}

	private static int indexOfPlaceHolder(String template, String placeholder, boolean wholeWords) {
		if ( wholeWords ) {
			int placeholderIndex = -1;
			boolean isPartialPlaceholderMatch;
			do {
				placeholderIndex = template.indexOf( placeholder, placeholderIndex + 1 );
				isPartialPlaceholderMatch = placeholderIndex != -1 &&
						template.length() > placeholderIndex + placeholder.length() &&
						isJavaIdentifierPart( template.charAt( placeholderIndex + placeholder.length() ) );
			} while ( placeholderIndex != -1 && isPartialPlaceholderMatch );

			return placeholderIndex;
		}
		else {
			return template.indexOf( placeholder );
		}
	}

	/**
	 * Used to find the ordinal parameters (e.g. '?1') in a string.
	 */
	public static int indexOfIdentifierWord(String str, String word) {
		if ( str != null && !str.isEmpty() && word != null && !word.isEmpty() ) {
			int position = str.indexOf( word );
			while ( position >= 0 && position < str.length() ) {
				if (
						( position == 0 || !isJavaIdentifierPart( str.charAt( position - 1 ) ) ) &&
								( position + word.length() == str.length()
										|| !isJavaIdentifierPart( str.charAt( position + word.length() ) ) )
				) {
					return position;
				}
				position = str.indexOf( word, position + 1 );
			}
		}
		return -1;
	}

	public static char getLastNonWhitespaceCharacter(String str) {
		if ( str != null && !str.isEmpty() ) {
			for ( int i = str.length() - 1; i >= 0; i-- ) {
				final char ch = str.charAt( i );
				if ( !isWhitespace( ch ) ) {
					return ch;
				}
			}
		}
		return '\0';
	}

	public static char getFirstNonWhitespaceCharacter(String str) {
		if ( str != null && !str.isEmpty() ) {
			for ( int i = 0; i < str.length(); i++ ) {
				final char ch = str.charAt( i );
				if ( !isWhitespace( ch ) ) {
					return ch;
				}
			}
		}
		return '\0';
	}

	public static String replaceOnce(String template, String placeholder, String replacement) {
		if ( template == null ) {
			return null;
		}
		else {
			final int loc = template.indexOf( placeholder );
			return loc < 0 ? template
					: template.substring( 0, loc )
							+ replacement
							+ template.substring( loc + placeholder.length() );
		}
	}

	public static String[] split(String separators, String list) {
		return split( separators, list, false );
	}

	public static String[] split(String separators, String list, boolean include) {
		final StringTokenizer tokens = new StringTokenizer( list, separators, include );
		final String[] result = new String[tokens.countTokens()];
		int i = 0;
		while ( tokens.hasMoreTokens() ) {
			result[i++] = tokens.nextToken();
		}
		return result;
	}

	public static String[] splitTrimmingTokens(String separators, String list, boolean include) {
		final StringTokenizer tokens = new StringTokenizer( list, separators, include );
		final String[] result = new String[tokens.countTokens()];
		int i = 0;
		while ( tokens.hasMoreTokens() ) {
			result[i++] = tokens.nextToken().trim();
		}
		return result;
	}

	public static String[] splitFull(String separators, String list) {
		final List<String> parts = new ArrayList<>();
		int prevIndex = 0;
		int index;
		while ( ( index = list.indexOf( separators, prevIndex ) ) != -1 ) {
			parts.add( list.substring( prevIndex, index ) );
			prevIndex = index + separators.length();
		}
		parts.add( list.substring( prevIndex ) );
		return parts.toArray( EMPTY_STRING_ARRAY );
	}

	public static String unqualify(String qualifiedName) {
		final int loc = qualifiedName.lastIndexOf( '.' );
		return loc < 0 ? qualifiedName : qualifiedName.substring( loc + 1 );
	}

	public static String qualifier(String qualifiedName) {
		final int loc = qualifiedName.lastIndexOf( '.' );
		return loc < 0 ? "" : qualifiedName.substring( 0, loc );
	}

	/**
	 * Collapses a name.  Mainly intended for use with classnames, where an example might serve best to explain.
	 * Imagine you have a class named <samp>'org.hibernate.internal.util.StringHelper'</samp>; calling collapse on that
	 * classname will result in <samp>'o.h.u.StringHelper'</samp>.
	 *
	 * @param name The name to collapse.
	 *
	 * @return The collapsed name.
	 */
	public static String collapse(String name) {
		if ( name == null ) {
			return null;
		}
		else {
			final int breakPoint = name.lastIndexOf( '.' );
			if ( breakPoint < 0 ) {
				return name;
			}
			return collapseQualifier( name.substring( 0, breakPoint ), true )
					+ name.substring( breakPoint ); // includes last '.'
		}
	}

	/**
	 * Given a qualifier, collapse it.
	 *
	 * @param qualifier The qualifier to collapse.
	 * @param includeDots Should we include the dots in the collapsed form?
	 *
	 * @return The collapsed form.
	 */
	public static String collapseQualifier(String qualifier, boolean includeDots) {
		final StringTokenizer tokenizer = new StringTokenizer( qualifier, "." );
		final StringBuilder result = new StringBuilder();
		result.append( tokenizer.nextToken().charAt( 0 ) );
		while ( tokenizer.hasMoreTokens() ) {
			if ( includeDots ) {
				result.append( '.' );
			}
			result.append( tokenizer.nextToken().charAt( 0 ) );
		}
		return result.toString();
	}

	/**
	 * Partially unqualifies a qualified name.  For example, with a base of 'org.hibernate' the name
	 * 'org.hibernate.internal.util.StringHelper' would become 'util.StringHelper'.
	 *
	 * @param name The (potentially) qualified name.
	 * @param qualifierBase The qualifier base.
	 *
	 * @return The name itself, or the partially unqualified form if it begins with the qualifier base.
	 */
	public static String partiallyUnqualify(String name, String qualifierBase) {
		return name == null || !name.startsWith( qualifierBase )
				? name
				: name.substring( qualifierBase.length() + 1 ); // +1 to start after the following '.'
	}

	/**
	 * Cross between {@link #collapse} and {@link #partiallyUnqualify}.  Functions much like {@link #collapse}
	 * except that only the qualifierBase is collapsed.  For example, with a base of 'org.hibernate' the name
	 * 'org.hibernate.internal.util.StringHelper' would become 'o.h.util.StringHelper'.
	 *
	 * @param name The (potentially) qualified name.
	 * @param qualifierBase The qualifier base.
	 *
	 * @return The name itself if it does not begin with the qualifierBase, or the properly collapsed form otherwise.
	 */
	public static String collapseQualifierBase(String name, String qualifierBase) {
		return name == null || !name.startsWith( qualifierBase )
				? collapse( name )
				: collapseQualifier( qualifierBase, true )
						+ name.substring( qualifierBase.length() );
	}

	public static String[] suffix(String[] columns, String suffix) {
		if ( suffix == null ) {
			return columns;
		}
		else {
			final String[] qualified = new String[columns.length];
			for ( int i = 0; i < columns.length; i++ ) {
				qualified[i] = suffix( columns[i], suffix );
			}
			return qualified;
		}
	}

	private static String suffix(String name, String suffix) {
		return suffix == null ? name : name + suffix;
	}

	public static String root(String qualifiedName) {
		final int loc = qualifiedName.indexOf( '.' );
		return loc < 0 ? qualifiedName : qualifiedName.substring( 0, loc );
	}

	public static String unroot(String qualifiedName) {
		final int loc = qualifiedName.indexOf( '.' );
		return loc < 0 ? qualifiedName : qualifiedName.substring( loc + 1 );
	}

	public static String toString(Object[] array) {
		final int len = array.length;
		if ( len == 0 ) {
			return "";
		}
		else {
			final StringBuilder buf = new StringBuilder( len * 12 );
			for ( int i = 0; i < len - 1; i++ ) {
				buf.append( array[i] ).append( ", " );
			}
			return buf.append( array[len - 1] ).toString();
		}
	}

	public static String[] multiply(String string, Iterator<String> placeholders, Iterator<String[]> replacements) {
		String[] result = new String[] {string};
		while ( placeholders.hasNext() ) {
			result = multiply( result, placeholders.next(), replacements.next() );
		}
		return result;
	}

	private static String[] multiply(String[] strings, String placeholder, String[] replacements) {
		final String[] results = new String[replacements.length * strings.length];
		int n = 0;
		for ( String replacement : replacements ) {
			for ( String string : strings ) {
				results[n++] = replaceOnce( string, placeholder, replacement );
			}
		}
		return results;
	}

	public static int count(String text, String match) {
		int count = 0;
		int index = text.indexOf( match );
		while ( index > -1 ) {
			count++;
			index = text.indexOf( match, index + 1 );
		}
		return count;
	}

	public static int count(String text, char match) {
		if ( text == null ) {
			return 0;
		}
		text = text.trim();
		if ( text.isEmpty() ) {
			return 0;
		}
		int count = 0;
		for ( int i = 0, max = text.length(); i < max; i++ ) {
			final char check = text.charAt( i );
			if ( check == match ) {
				count++;
			}
		}
		return count;
	}

	public static int countUnquoted(String string, char character) {
		if ( '\'' == character ) {
			throw new IllegalArgumentException( "Unquoted count of quotes is invalid" );
		}
		if ( string == null ) {
			return 0;
		}
		// Impl note: takes advantage of the fact that an escaped single quote
		// embedded within a quote-block can really be handled as two separate
		// quote-blocks for the purposes of this method...
		int count = 0;
		final int stringLength = string.length();
		boolean inQuote = false;
		for ( int indx = 0; indx < stringLength; indx++ ) {
			char c = string.charAt( indx );
			if ( inQuote ) {
				if ( '\'' == c ) {
					inQuote = false;
				}
			}
			else if ( '\'' == c ) {
				inQuote = true;
			}
			else if ( c == character ) {
				count++;
			}
		}
		return count;
	}

	public static boolean isNotEmpty(@Nullable String string) {
		return string != null && !string.isEmpty();
	}

	public static boolean isEmpty(@Nullable String string) {
		return string == null || string.isEmpty();
	}

	public static boolean isNotBlank(@Nullable String string) {
		return string != null && !string.isBlank();
	}

	public static boolean isBlank(@Nullable String string) {
		return string == null || string.isBlank();
	}

	public static String qualify(String prefix, String name) {
		if ( name == null || prefix == null ) {
			throw new NullPointerException( "prefix or name were null attempting to build qualified name" );
		}
		return prefix + '.' + name;
	}

	public static String qualifyConditionally(String prefix, String name) {
		if ( name == null ) {
			throw new NullPointerException( "name was null attempting to build qualified name" );
		}
		return isEmpty( prefix ) ? name : prefix + '.' + name;
	}

	/**
	 * Qualifies {@code name} with {@code prefix} separated by a '.' if<ul>
	 *     <li>{@code name} is not already qualified</li>
	 *     <li>{@code prefix} is not null</li>
	 * </ul>
	 *
	 * @apiNote Similar to {@link #qualifyConditionally}, except that here we explicitly
	 * check whether {@code name} is already qualified.
	 */
	public static String qualifyConditionallyIfNot(String prefix, String name) {
		if ( name == null ) {
			throw new NullPointerException( "name was null attempting to build qualified name" );
		}
		if ( name.indexOf( '.' ) > 0 || isEmpty( prefix ) ) {
			return name;
		}
		return prefix + '.' + name;
	}

	public static String[] qualify(String prefix, String[] names) {
		if ( prefix == null ) {
			return names;
		}
		else {
			final int len = names.length;
			final String[] qualified = new String[len];
			for ( int i = 0; i < len; i++ ) {
				qualified[i] = qualify( prefix, names[i] );
			}
			return qualified;
		}
	}

	public static int firstIndexOfChar(String sqlString, BitSet keys, int startindex) {
		for ( int i = startindex, size = sqlString.length(); i < size; i++ ) {
			if ( keys.get( sqlString.charAt( i ) ) ) {
				return i;
			}
		}
		return -1;
	}

	public static int firstIndexOfChar(String sqlString, String string, int startindex) {
		final BitSet keys = new BitSet();
		for ( int i = 0, size = string.length(); i < size; i++ ) {
			keys.set( string.charAt( i ) );
		}
		return firstIndexOfChar( sqlString, keys, startindex );
	}

	public static String truncate(String string, int length) {
		return string.length() <= length ? string : string.substring( 0, length );
	}

	/**
	 * @deprecated No longer used
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static String generateAlias(String description) {
		return generateAliasRoot( description ) + '_';
	}

	/**
	 * Generate a nice alias for the given class name or collection role name and unique integer. Subclasses of
	 * Loader do <em>not</em> have to use aliases of this form.
	 *
	 * @param description The base name (usually an entity-name or collection-role)
	 * @param unique A uniquing value
	 *
	 * @return an alias of the form <samp>foo1_</samp>
	 *
	 * @deprecated No longer used
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static String generateAlias(String description, int unique) {
		return generateAliasRoot( description )
				+ AliasConstantsHelper.get( unique );
	}

	/**
	 * Generates a root alias by truncating the "root name" defined by
	 * the incoming description and removing/modifying any non-valid
	 * alias characters.
	 *
	 * @param description The root name from which to generate a root alias.
	 *
	 * @return The generated root alias.
	 *
	 * @deprecated No longer used
	 */
	@Deprecated(since = "7", forRemoval = true)
	private static String generateAliasRoot(String description) {
		String result = truncate( unqualifyEntityName( description ), ALIAS_TRUNCATE_LENGTH )
				.toLowerCase( Locale.ROOT )
				.replace( '/', '_' ) // entityNames may now include slashes for the representations
				.replace( '$', '_' ); //classname may be an inner class
		result = cleanAlias( result );
		return isDigit( result.charAt( result.length() - 1 ) ) ? result + "x" : result; //ick!
	}

	/**
	 * Clean the generated alias by removing any non-alpha characters from the
	 * beginning.
	 *
	 * @param alias The generated alias to be cleaned.
	 *
	 * @return The cleaned alias, stripped of any leading non-alpha characters.
	 *
	 * @deprecated No longer used
	 */
	@Deprecated(since = "7", forRemoval = true)
	private static String cleanAlias(String alias) {
		final char[] chars = alias.toCharArray();
		// shortcut check...
		if ( !isLetter( chars[0] ) ) {
			for ( int i = 1; i < chars.length; i++ ) {
				// as soon as we encounter our first letter, return the substring
				// from that position
				if ( isLetter( chars[i] ) ) {
					return alias.substring( i );
				}
			}
		}
		return alias;
	}

	public static String unqualifyEntityName(String entityName) {
		final String result = unqualify( entityName );
		final int slashPos = result.indexOf( '/' );
		return slashPos > 0 ? result.substring( 0, slashPos - 1 ) : result;
	}

	/**
	 * Determine if the given string is quoted (wrapped by '`' characters at beginning and end).
	 *
	 * @param name The name to check.
	 *
	 * @return True if the given string starts and ends with '`'; false otherwise.
	 */
	public static boolean isQuoted(final String name) {
		if ( name == null || name.isEmpty() ) {
			return false;
		}
		else {
			final char first = name.charAt( 0 );
			final char last = name.charAt( name.length() - 1 );
			return first == last && ( first == '`' || first == '"' );
		}
	}

	/**
	 * Return the unquoted version of name (stripping the start and end '`' characters if present).
	 *
	 * @param name The name to be unquoted.
	 *
	 * @return The unquoted version.
	 */
	public static String unquote(String name) {
		return isQuoted( name ) ? name.substring( 1, name.length() - 1 ) : name;
	}

	/**
	 * Determine if the given name is quoted.  It is considered quoted if either:
	 * <ol>
	 * <li>starts AND ends with backticks (`)</li>
	 * <li>starts with dialect-specified {@link Dialect#openQuote() open-quote}
	 * AND ends with dialect-specified {@link Dialect#closeQuote() close-quote}</li>
	 * </ol>
	 *
	 * @param name The name to check
	 * @param dialect The dialect (to determine the "real" quoting chars).
	 *
	 * @return True if quoted, false otherwise
	 */
	public static boolean isQuoted(final String name, final Dialect dialect) {
		if ( name == null || name.isEmpty() ) {
			return false;
		}
		else {
			final char first = name.charAt( 0 );
			final char last = name.charAt( name.length() - 1 );
			return first == last && ( first == '`' || first == '"' )
				|| first == dialect.openQuote() && last == dialect.closeQuote();
		}
	}

	/**
	 * Return the unquoted version of name stripping the start and end quote characters.
	 *
	 * @param name The name to be unquoted.
	 * @param dialect The dialect (to determine the "real" quoting chars).
	 *
	 * @return The unquoted version.
	 */
	public static String unquote(String name, Dialect dialect) {
		return isQuoted( name, dialect ) ? name.substring( 1, name.length() - 1 ) : name;
	}

	/**
	 * Return the unquoted version of name stripping the start and end quote characters.
	 *
	 * @param names The names to be unquoted.
	 * @param dialect The dialect (to determine the "real" quoting chars).
	 *
	 * @return The unquoted versions.
	 */
	public static String[] unquote(final String[] names, final Dialect dialect) {
		if ( names == null ) {
			return null;
		}
		int failedIndex = -1;
		final int length = names.length;
		for ( int i = 0; i < length; i++ ) {
			if ( isQuoted( names[i], dialect ) ) {
				failedIndex = i;
				break;
			}
		}
		if ( failedIndex == -1 ) {
			//In this case all strings are already unquoted, so return the same array as the input:
			//this is a good optimisation to skip an array copy as typically either all names are consistently quoted, or none are;
			//yet for safety we need to deal with mixed scenarios as well.
			return names;
		}
		else {
			final String[] unquoted = new String[length];
			System.arraycopy( names, 0, unquoted, 0, failedIndex );
			for ( int i = failedIndex; i < length; i++ ) {
				unquoted[i] = unquote( names[i], dialect );
			}
			return unquoted;
		}
	}

	public static @Nullable String nullIfEmpty(@Nullable String value) {
		return isEmpty( value ) ? null : value;
	}

	public static @Nullable String nullIfBlank(@Nullable String value) {
		return isBlank( value ) ? null : value;
	}

	public static @Nullable String subStringNullIfEmpty(String value, Character startChar) {
		if ( isEmpty( value ) ) {
			return null;
		}
		else {
			final int index = value.indexOf( startChar );
			return index != -1 ? value.substring( index + 1 ) : value;
		}
	}

	public static String[] splitAtCommas(@Nullable String incomingString) {
		return incomingString==null || incomingString.isBlank()
				? EMPTY_STRINGS
				: COMMA_SEPARATED_PATTERN.split( incomingString );
	}

	public static <T> String join(Collection<T> values, Renderer<T> renderer) {
		final StringBuilder buffer = new StringBuilder();
		for ( T value : values ) {
			buffer.append( String.join(", ", renderer.render( value ) ) );
		}
		return buffer.toString();
	}

	public static String coalesce(@NonNull String fallbackValue, @NonNull String... values) {
		for ( int i = 0; i < values.length; i++ ) {
			if ( isNotEmpty( values[i] ) ) {
				return values[i];
			}
		}
		return fallbackValue;
	}

	public static String coalesce(@NonNull String fallbackValue, String value) {
		if ( isNotEmpty( value ) ) {
			return value;
		}
		return fallbackValue;
	}

	public interface Renderer<T> {
		String render(T value);
	}

	/**
	 * @param firstExpression the first expression
	 * @param secondExpression the second expression
	 * @return if {@code firstExpression} and {@code secondExpression} are both non-empty,
	 * then "( " + {@code firstExpression} + " ) and ( " + {@code secondExpression} + " )" is returned;
	 * if {@code firstExpression} is non-empty and {@code secondExpression} is empty,
	 * then {@code firstExpression} is returned;
	 * if {@code firstExpression} is empty and {@code secondExpression} is non-empty,
	 * then {@code secondExpression} is returned;
	 * if both {@code firstExpression} and {@code secondExpression} are empty, then null is returned.
	 */
	public static String getNonEmptyOrConjunctionIfBothNonEmpty( String firstExpression, String secondExpression ) {
		final boolean isFirstExpressionNonEmpty = isNotEmpty( firstExpression );
		final boolean isSecondExpressionNonEmpty = isNotEmpty( secondExpression );
		if ( isFirstExpressionNonEmpty && isSecondExpressionNonEmpty ) {
			return "( " + firstExpression + " ) and ( " + secondExpression + " )";
		}
		else if ( isFirstExpressionNonEmpty ) {
			return firstExpression;
		}
		else if ( isSecondExpressionNonEmpty ) {
			return secondExpression;
		}
		else {
			return null;
		}
	}

	/**
	 * Return the interned form of a String, or null if the parameter is null.
	 * <p>
	 * Use with caution: excessive interning is known to cause issues.
	 * Best to use only with strings which are known to be long-lived constants,
	 * and for which the chances of being actual duplicates is proven.
	 * (Even better: avoid needing interning by design changes such as reusing
	 * the known reference)
	 *
	 * @param string The string to intern.
	 * @return The interned string.
	 */
	public static String safeInterning(final String string) {
		return string == null ? null : string.intern();
	}

}
