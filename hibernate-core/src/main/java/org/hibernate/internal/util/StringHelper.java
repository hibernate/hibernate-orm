/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.hibernate.boot.model.source.internal.hbm.CommaSeparatedStringHelper;
import org.hibernate.dialect.Dialect;
import org.hibernate.loader.internal.AliasConstantsHelper;

public final class StringHelper {

	private static final int ALIAS_TRUNCATE_LENGTH = 10;
	public static final String WHITESPACE = " \n\r\f\t";
	public static final String[] EMPTY_STRINGS = new String[0];

	private StringHelper() { /* static methods only - hide constructor */
	}

	public static int lastIndexOfLetter(String string) {
		for ( int i = 0; i < string.length(); i++ ) {
			char character = string.charAt( i );
			// Include "_".  See HHH-8073
			if ( !Character.isLetter( character ) && !( '_' == character ) ) {
				return i - 1;
			}
		}
		return string.length() - 1;
	}

	public static String joinWithQualifierAndSuffix(
			String[] values,
			String qualifier,
			String suffix,
			String deliminator) {
		int length = values.length;
		if ( length == 0 ) {
			return "";
		}
		StringBuilder buf = new StringBuilder( length * ( values[0].length() + suffix.length() ) )
				.append( qualify( qualifier, values[0] ) ).append( suffix );
		for ( int i = 1; i < length; i++ ) {
			buf.append( deliminator ).append( qualify( qualifier, values[i] ) ).append( suffix );
		}
		return buf.toString();
	}

	public static String join(String separator, Iterator<?> objects) {
		StringBuilder buf = new StringBuilder();
		if ( objects.hasNext() ) {
			buf.append( objects.next() );
		}
		while ( objects.hasNext() ) {
			buf.append( separator ).append( objects.next() );
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
		StringBuilder buf = new StringBuilder( string.length() * times );
		for ( int i = 0; i < times; i++ ) {
			buf.append( string );
		}
		return buf.toString();
	}

	public static String repeat(String string, int times, String deliminator) {
		StringBuilder buf = new StringBuilder( ( string.length() * times ) + ( deliminator.length() * ( times - 1 ) ) )
				.append( string );
		for ( int i = 1; i < times; i++ ) {
			buf.append( deliminator ).append( string );
		}
		return buf.toString();
	}

	public static String repeat(char character, int times) {
		char[] buffer = new char[times];
		Arrays.fill( buffer, character );
		return new String( buffer );
	}

	public static String replace(String template, String placeholder, String replacement) {
		return replace( template, placeholder, replacement, false );
	}

	public static String[] replace(String[] templates, String placeholder, String replacement) {
		String[] result = new String[templates.length];
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
		int loc = indexOfPlaceHolder( template, placeholder, wholeWords );
		if ( loc < 0 ) {
			return template;
		}
		else {
			String beforePlaceholder = template.substring( 0, loc );
			String afterPlaceholder = template.substring( loc + placeholder.length() );
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
						|| afterPlaceholder.length() == 0
						|| !Character.isJavaIdentifierPart( afterPlaceholder.charAt( 0 ) );
		// We only need to check the left param to determine if the placeholder is already
		// enclosed in parentheses (HHH-10383)
		// Examples:
		// 1) "... IN (?1", we assume that "?1" does not need to be enclosed because there
		// there is already a right-parenthesis; we assume there will be a matching right-parenthesis.
		// 2) "... IN ?1", we assume that "?1" needs to be enclosed in parentheses, because there
		// is no left-parenthesis.

		// We need to check the placeholder is not used in `Order By FIELD(...)` (HHH-10502)
		// Examples:
		// " ... Order By FIELD(id,?1)",  after expand parameters, the sql is "... Order By FIELD(id,?,?,?)"
		boolean encloseInParens =
				actuallyReplace
						&& encloseInParensIfNecessary
						&& !( getLastNonWhitespaceCharacter( beforePlaceholder ) == '(' ) &&
						!( getLastNonWhitespaceCharacter( beforePlaceholder ) == ',' && getFirstNonWhitespaceCharacter(
								afterPlaceholder ) == ')' );
		StringBuilder buf = new StringBuilder( beforePlaceholder );
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
						Character.isJavaIdentifierPart( template.charAt( placeholderIndex + placeholder.length() ) );
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
		if ( str == null || str.length() == 0 || word == null || word.length() == 0 ) {
			return -1;
		}

		int position = str.indexOf( word );
		while ( position >= 0 && position < str.length() ) {
			if (
					( position == 0 || !Character.isJavaIdentifierPart( str.charAt( position - 1 ) ) ) &&
					( position + word.length() == str.length() || !Character.isJavaIdentifierPart( str.charAt( position + word.length() ) ) )
			) {
				return position;
			}
			position = str.indexOf( word, position + 1 );
		}

		return -1;
	}

	public static char getLastNonWhitespaceCharacter(String str) {
		if ( str != null && str.length() > 0 ) {
			for ( int i = str.length() - 1; i >= 0; i-- ) {
				char ch = str.charAt( i );
				if ( !Character.isWhitespace( ch ) ) {
					return ch;
				}
			}
		}
		return '\0';
	}

	public static char getFirstNonWhitespaceCharacter(String str) {
		if ( str != null && str.length() > 0 ) {
			for ( int i = 0; i < str.length(); i++ ) {
				char ch = str.charAt( i );
				if ( !Character.isWhitespace( ch ) ) {
					return ch;
				}
			}
		}
		return '\0';
	}

	public static String replaceOnce(String template, String placeholder, String replacement) {
		if ( template == null ) {
			return null;  // returning null!
		}
		int loc = template.indexOf( placeholder );
		if ( loc < 0 ) {
			return template;
		}
		else {
			return template.substring( 0, loc ) + replacement + template.substring( loc + placeholder.length() );
		}
	}


	public static String[] split(String separators, String list) {
		return split( separators, list, false );
	}

	public static String[] split(String separators, String list, boolean include) {
		StringTokenizer tokens = new StringTokenizer( list, separators, include );
		String[] result = new String[tokens.countTokens()];
		int i = 0;
		while ( tokens.hasMoreTokens() ) {
			result[i++] = tokens.nextToken();
		}
		return result;
	}

	public static String[] splitTrimmingTokens(String separators, String list, boolean include) {
		StringTokenizer tokens = new StringTokenizer( list, separators, include );
		String[] result = new String[tokens.countTokens()];
		int i = 0;
		while ( tokens.hasMoreTokens() ) {
			result[i++] = tokens.nextToken().trim();
		}
		return result;
	}

	public static String unqualify(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf( '.' );
		return ( loc < 0 ) ? qualifiedName : qualifiedName.substring( loc + 1 );
	}

	public static String qualifier(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf( '.' );
		return ( loc < 0 ) ? "" : qualifiedName.substring( 0, loc );
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
		int breakPoint = name.lastIndexOf( '.' );
		if ( breakPoint < 0 ) {
			return name;
		}
		return collapseQualifier(
				name.substring( 0, breakPoint ),
				true
		) + name.substring( breakPoint ); // includes last '.'
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
		StringTokenizer tokenizer = new StringTokenizer( qualifier, "." );
		StringBuilder sb = new StringBuilder();
		sb.append( Character.toString( tokenizer.nextToken().charAt( 0 ) ) );
		while ( tokenizer.hasMoreTokens() ) {
			if ( includeDots ) {
				sb.append( '.' );
			}
			sb.append( tokenizer.nextToken().charAt( 0 ) );
		}
		return sb.toString();
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
		if ( name == null || !name.startsWith( qualifierBase ) ) {
			return name;
		}
		return name.substring( qualifierBase.length() + 1 ); // +1 to start after the following '.'
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
		if ( name == null || !name.startsWith( qualifierBase ) ) {
			return collapse( name );
		}
		return collapseQualifier( qualifierBase, true ) + name.substring( qualifierBase.length() );
	}

	public static String[] suffix(String[] columns, String suffix) {
		if ( suffix == null ) {
			return columns;
		}
		String[] qualified = new String[columns.length];
		for ( int i = 0; i < columns.length; i++ ) {
			qualified[i] = suffix( columns[i], suffix );
		}
		return qualified;
	}

	private static String suffix(String name, String suffix) {
		return ( suffix == null ) ? name : name + suffix;
	}

	public static String root(String qualifiedName) {
		int loc = qualifiedName.indexOf( '.' );
		return ( loc < 0 ) ? qualifiedName : qualifiedName.substring( 0, loc );
	}

	public static String unroot(String qualifiedName) {
		int loc = qualifiedName.indexOf( '.' );
		return ( loc < 0 ) ? qualifiedName : qualifiedName.substring( loc + 1, qualifiedName.length() );
	}

	public static String toString(Object[] array) {
		int len = array.length;
		if ( len == 0 ) {
			return "";
		}
		StringBuilder buf = new StringBuilder( len * 12 );
		for ( int i = 0; i < len - 1; i++ ) {
			buf.append( array[i] ).append( ", " );
		}
		return buf.append( array[len - 1] ).toString();
	}

	public static String[] multiply(String string, Iterator<String> placeholders, Iterator<String[]> replacements) {
		String[] result = new String[] {string};
		while ( placeholders.hasNext() ) {
			result = multiply( result, placeholders.next(), replacements.next() );
		}
		return result;
	}

	private static String[] multiply(String[] strings, String placeholder, String[] replacements) {
		String[] results = new String[replacements.length * strings.length];
		int n = 0;
		for ( String replacement : replacements ) {
			for ( String string : strings ) {
				results[n++] = replaceOnce( string, placeholder, replacement );
			}
		}
		return results;
	}

	public static int countUnquoted(String string, char character) {
		if ( '\'' == character ) {
			throw new IllegalArgumentException( "Unquoted count of quotes is invalid" );
		}
		if ( string == null ) {
			return 0;
		}
		// Impl note: takes advantage of the fact that an escpaed single quote
		// embedded within a quote-block can really be handled as two seperate
		// quote-blocks for the purposes of this method...
		int count = 0;
		int stringLength = string.length();
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

	public static boolean isNotEmpty(String string) {
		return string != null && string.length() > 0;
	}

	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	public static boolean isEmptyOrWhiteSpace(String string) {
		return isEmpty( string ) || isEmpty( string.trim() );
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

	public static String[] qualify(String prefix, String[] names) {
		if ( prefix == null ) {
			return names;
		}
		int len = names.length;
		String[] qualified = new String[len];
		for ( int i = 0; i < len; i++ ) {
			qualified[i] = qualify( prefix, names[i] );
		}
		return qualified;
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
		BitSet keys = new BitSet();
		for ( int i = 0, size = string.length(); i < size; i++ ) {
			keys.set( string.charAt( i ) );
		}
		return firstIndexOfChar( sqlString, keys, startindex );
	}

	public static String truncate(String string, int length) {
		if ( string.length() <= length ) {
			return string;
		}
		else {
			return string.substring( 0, length );
		}
	}

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
	 */
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
	 */
	private static String generateAliasRoot(String description) {
		String result = truncate( unqualifyEntityName( description ), ALIAS_TRUNCATE_LENGTH )
				.toLowerCase( Locale.ROOT )
				.replace( '/', '_' ) // entityNames may now include slashes for the representations
				.replace( '$', '_' ); //classname may be an inner class
		result = cleanAlias( result );
		if ( Character.isDigit( result.charAt( result.length() - 1 ) ) ) {
			return result + "x"; //ick!
		}
		else {
			return result;
		}
	}

	/**
	 * Clean the generated alias by removing any non-alpha characters from the
	 * beginning.
	 *
	 * @param alias The generated alias to be cleaned.
	 *
	 * @return The cleaned alias, stripped of any leading non-alpha characters.
	 */
	private static String cleanAlias(String alias) {
		char[] chars = alias.toCharArray();
		// short cut check...
		if ( !Character.isLetter( chars[0] ) ) {
			for ( int i = 1; i < chars.length; i++ ) {
				// as soon as we encounter our first letter, return the substring
				// from that position
				if ( Character.isLetter( chars[i] ) ) {
					return alias.substring( i );
				}
			}
		}
		return alias;
	}

	public static String unqualifyEntityName(String entityName) {
		String result = unqualify( entityName );
		int slashPos = result.indexOf( '/' );
		if ( slashPos > 0 ) {
			result = result.substring( 0, slashPos - 1 );
		}
		return result;
	}

	public static String moveAndToBeginning(String filter) {
		if ( filter.trim().length() > 0 ) {
			filter += " and ";
			if ( filter.startsWith( " and " ) ) {
				filter = filter.substring( 4 );
			}
		}
		return filter;
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

		final char first = name.charAt( 0 );
		final char last = name.charAt( name.length() - 1 );

		return ( ( first == last ) && ( first == '`' || first == '"' ) );
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
		final char first = name.charAt( 0 );
		final char last = name.charAt( name.length() - 1 );

		return ( ( first == last ) && ( first == '`' || first == '"' ) )
				|| ( first == dialect.openQuote() && last == dialect.closeQuote() );
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
			String[] unquoted = new String[length];
			System.arraycopy( names, 0, unquoted, 0, failedIndex );
			for ( int i = failedIndex; i < length; i++ ) {
				unquoted[i] = unquote( names[i], dialect );
			}
			return unquoted;
		}
	}


	public static final String BATCH_ID_PLACEHOLDER = "$$BATCH_ID_PLACEHOLDER$$";

	public static StringBuilder buildBatchFetchRestrictionFragment(
			String alias,
			String[] columnNames,
			Dialect dialect) {
		// the general idea here is to just insert a placeholder that we can easily find later...
		if ( columnNames.length == 1 ) {
			// non-composite key
			return new StringBuilder( StringHelper.qualify( alias, columnNames[0] ) )
					.append( " in (" ).append( BATCH_ID_PLACEHOLDER ).append( ')' );
		}
		else {
			// composite key - the form to use here depends on what the dialect supports.
			if ( dialect.supportsRowValueConstructorSyntaxInInList() ) {
				// use : (col1, col2) in ( (?,?), (?,?), ... )
				StringBuilder builder = new StringBuilder();
				builder.append( '(' );
				boolean firstPass = true;
				String deliminator = "";
				for ( String columnName : columnNames ) {
					builder.append( deliminator ).append( StringHelper.qualify( alias, columnName ) );
					if ( firstPass ) {
						firstPass = false;
						deliminator = ",";
					}
				}
				builder.append( ") in (" );
				builder.append( BATCH_ID_PLACEHOLDER );
				builder.append( ')' );
				return builder;
			}
			else {
				// use : ( (col1 = ? and col2 = ?) or (col1 = ? and col2 = ?) or ... )
				//		unfortunately most of this building needs to be held off until we know
				//		the exact number of ids :(
				final StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append( '(' )
						.append( BATCH_ID_PLACEHOLDER )
						.append( ')' );
				return stringBuilder;
			}
		}
	}

	public static String expandBatchIdPlaceholder(
			String sql,
			Serializable[] ids,
			String alias,
			String[] keyColumnNames,
			Dialect dialect) {
		if ( keyColumnNames.length == 1 ) {
			// non-composite
			return StringHelper.replace( sql, BATCH_ID_PLACEHOLDER, repeat( "?", ids.length, "," ) );
		}
		else {
			// composite
			if ( dialect.supportsRowValueConstructorSyntaxInInList() ) {
				final String tuple = '(' + StringHelper.repeat( "?", keyColumnNames.length, "," ) + ')';
				return StringHelper.replace( sql, BATCH_ID_PLACEHOLDER, repeat( tuple, ids.length, "," ) );
			}
			else {
				final String keyCheck = '(' + joinWithQualifierAndSuffix(
						keyColumnNames,
						alias,
						" = ?",
						" and "
				) + ')';
				return replace( sql, BATCH_ID_PLACEHOLDER, repeat( keyCheck, ids.length, " or " ) );
			}
		}
	}

	public static String nullIfEmpty(String value) {
		return isEmpty( value ) ? null : value;
	}

	public static List<String> parseCommaSeparatedString(String incomingString) {
		return CommaSeparatedStringHelper.parseCommaSeparatedString( incomingString );
	}

	public static <T> String join(Collection<T> values, Renderer<T> renderer) {
		final StringBuilder buffer = new StringBuilder();
		for ( T value : values ) {
			buffer.append( String.join(", ", renderer.render( value ) ) );
		}
		return buffer.toString();
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
		final boolean isFirstExpressionNonEmpty = StringHelper.isNotEmpty( firstExpression );
		final boolean isSecondExpressionNonEmpty = StringHelper.isNotEmpty( secondExpression );
		if ( isFirstExpressionNonEmpty && isSecondExpressionNonEmpty ) {
			final StringBuilder buffer = new StringBuilder();
			buffer.append( "( " )
					.append( firstExpression )
					.append( " ) and ( ")
					.append( secondExpression )
					.append( " )" );
			return buffer.toString();
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
	 * Best to use only with strings which are known to be long lived constants,
	 * and for which the chances of being actual duplicates is proven.
	 * (Even better: avoid needing interning by design changes such as reusing
	 * the known reference)
	 * @param string The string to intern.
	 * @return The interned string.
	 */
	public static String safeInterning(final String string) {
		if ( string == null ) {
			return null;
		}
		else {
			return string.intern();
		}
	}

}
