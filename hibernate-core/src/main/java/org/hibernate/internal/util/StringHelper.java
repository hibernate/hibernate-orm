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
package org.hibernate.internal.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.collections.ArrayHelper;

public final class StringHelper {

	private static final int ALIAS_TRUNCATE_LENGTH = 10;
	public static final String WHITESPACE = " \n\r\f\t";
	public static final String [] EMPTY_STRINGS = new String[0];

	private StringHelper() { /* static methods only - hide constructor */
	}
	
	/*public static boolean containsDigits(String string) {
		for ( int i=0; i<string.length(); i++ ) {
			if ( Character.isDigit( string.charAt(i) ) ) return true;
		}
		return false;
	}*/

	public static int lastIndexOfLetter(String string) {
		for ( int i=0; i<string.length(); i++ ) {
			char character = string.charAt(i);
			// Include "_".  See HHH-8073
			if ( !Character.isLetter(character) && !('_'==character) ) return i-1;
		}
		return string.length()-1;
	}

	public static String join(String seperator, String[] strings) {
		int length = strings.length;
		if ( length == 0 ) return "";
		StringBuilder buf = new StringBuilder( length * strings[0].length() )
				.append( strings[0] );
		for ( int i = 1; i < length; i++ ) {
			buf.append( seperator ).append( strings[i] );
		}
		return buf.toString();
	}

	public static String joinWithQualifier(String[] values, String qualifier, String deliminator) {
		int length = values.length;
		if ( length == 0 ) return "";
		StringBuilder buf = new StringBuilder( length * values[0].length() )
				.append( qualify( qualifier, values[0] ) );
		for ( int i = 1; i < length; i++ ) {
			buf.append( deliminator ).append( qualify( qualifier, values[i] ) );
		}
		return buf.toString();
	}

	public static String join(String seperator, Iterator objects) {
		StringBuilder buf = new StringBuilder();
		if ( objects.hasNext() ) buf.append( objects.next() );
		while ( objects.hasNext() ) {
			buf.append( seperator ).append( objects.next() );
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
		for ( int i = 0; i < times; i++ ) buf.append( string );
		return buf.toString();
	}

	public static String repeat(String string, int times, String deliminator) {
		StringBuilder buf = new StringBuilder(  ( string.length() * times ) + ( deliminator.length() * (times-1) ) )
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
		for ( int i =0; i<templates.length; i++ ) {
			result[i] = replace( templates[i], placeholder, replacement );
		}
		return result;
	}

	public static String replace(String template, String placeholder, String replacement, boolean wholeWords) {
		return replace( template, placeholder, replacement, wholeWords, false );
	}

	public static String replace(String template,
								 String placeholder,
								 String replacement,
								 boolean wholeWords,
								 boolean encloseInParensIfNecessary) {
		if ( template == null ) {
			return template;
		}
		int loc = template.indexOf( placeholder );
		if ( loc < 0 ) {
			return template;
		}
		else {
			String beforePlaceholder = template.substring( 0, loc );
			String afterPlaceholder = template.substring( loc + placeholder.length() );
			return replace( beforePlaceholder, afterPlaceholder, placeholder, replacement, wholeWords, encloseInParensIfNecessary );
		}
	}


	public static String replace(String beforePlaceholder,
								 String afterPlaceholder,
								 String placeholder,
								 String replacement,
								 boolean wholeWords,
								 boolean encloseInParensIfNecessary) {
		final boolean actuallyReplace =
				! wholeWords ||
				afterPlaceholder.length() == 0 ||
				! Character.isJavaIdentifierPart( afterPlaceholder.charAt( 0 ) );
		boolean encloseInParens =
				actuallyReplace &&
				encloseInParensIfNecessary &&
				! ( getLastNonWhitespaceCharacter( beforePlaceholder ) == '(' ) &&
				! ( getFirstNonWhitespaceCharacter( afterPlaceholder ) == ')' );		
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

	public static char getLastNonWhitespaceCharacter(String str) {
		if ( str != null && str.length() > 0 ) {
			for ( int i = str.length() - 1 ; i >= 0 ; i-- ) {
				char ch = str.charAt( i );
				if ( ! Character.isWhitespace( ch ) ) {
					return ch;
				}
			}
		}
		return '\0';
	}

	public static char getFirstNonWhitespaceCharacter(String str) {
		if ( str != null && str.length() > 0 ) {
			for ( int i = 0 ; i < str.length() ; i++ ) {
				char ch = str.charAt( i );
				if ( ! Character.isWhitespace( ch ) ) {
					return ch;
				}
			}
		}
		return '\0';
	}

	public static String replaceOnce(String template, String placeholder, String replacement) {
		if ( template == null ) {
			return template; // returnign null!
		}
        int loc = template.indexOf( placeholder );
		if ( loc < 0 ) {
			return template;
		}
		else {
			return new StringBuilder( template.substring( 0, loc ) )
					.append( replacement )
					.append( template.substring( loc + placeholder.length() ) )
					.toString();
		}
	}


	public static String[] split(String seperators, String list) {
		return split( seperators, list, false );
	}

	public static String[] split(String seperators, String list, boolean include) {
		StringTokenizer tokens = new StringTokenizer( list, seperators, include );
		String[] result = new String[ tokens.countTokens() ];
		int i = 0;
		while ( tokens.hasMoreTokens() ) {
			result[i++] = tokens.nextToken();
		}
		return result;
	}

	public static String unqualify(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf(".");
		return ( loc < 0 ) ? qualifiedName : qualifiedName.substring( loc + 1 );
	}

	public static String qualifier(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf(".");
		return ( loc < 0 ) ? "" : qualifiedName.substring( 0, loc );
	}

	/**
	 * Collapses a name.  Mainly intended for use with classnames, where an example might serve best to explain.
	 * Imagine you have a class named <samp>'org.hibernate.internal.util.StringHelper'</samp>; calling collapse on that
	 * classname will result in <samp>'o.h.u.StringHelper'<samp>.
	 *
	 * @param name The name to collapse.
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
		return collapseQualifier( name.substring( 0, breakPoint ), true ) + name.substring( breakPoint ); // includes last '.'
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
		String collapsed = Character.toString( tokenizer.nextToken().charAt( 0 ) );
		while ( tokenizer.hasMoreTokens() ) {
			if ( includeDots ) {
				collapsed += '.';
			}
			collapsed += tokenizer.nextToken().charAt( 0 );
		}
		return collapsed;
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
		if ( name == null || ! name.startsWith( qualifierBase ) ) {
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
		if ( name == null || ! name.startsWith( qualifierBase ) ) {
			return collapse( name );
		}
		return collapseQualifier( qualifierBase, true ) + name.substring( qualifierBase.length() );
	}

	public static String[] suffix(String[] columns, String suffix) {
		if ( suffix == null ) return columns;
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
		int loc = qualifiedName.indexOf( "." );
		return ( loc < 0 ) ? qualifiedName : qualifiedName.substring( 0, loc );
	}

	public static String unroot(String qualifiedName) {
		int loc = qualifiedName.indexOf( "." );
		return ( loc < 0 ) ? qualifiedName : qualifiedName.substring( loc+1, qualifiedName.length() );
	}

	public static boolean booleanValue(String tfString) {
		String trimmed = tfString.trim().toLowerCase();
		return trimmed.equals( "true" ) || trimmed.equals( "t" );
	}

	public static String toString(Object[] array) {
		int len = array.length;
		if ( len == 0 ) return "";
		StringBuilder buf = new StringBuilder( len * 12 );
		for ( int i = 0; i < len - 1; i++ ) {
			buf.append( array[i] ).append(", ");
		}
		return buf.append( array[len - 1] ).toString();
	}

	public static String[] multiply(String string, Iterator placeholders, Iterator replacements) {
		String[] result = new String[]{string};
		while ( placeholders.hasNext() ) {
			result = multiply( result, ( String ) placeholders.next(), ( String[] ) replacements.next() );
		}
		return result;
	}

	private static String[] multiply(String[] strings, String placeholder, String[] replacements) {
		String[] results = new String[replacements.length * strings.length];
		int n = 0;
		for ( int i = 0; i < replacements.length; i++ ) {
			for ( int j = 0; j < strings.length; j++ ) {
				results[n++] = replaceOnce( strings[j], placeholder, replacements[i] );
			}
		}
		return results;
	}

	public static int countUnquoted(String string, char character) {
		if ( '\'' == character ) {
			throw new IllegalArgumentException( "Unquoted count of quotes is invalid" );
		}
		if (string == null)
			return 0;
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

	public static int[] locateUnquoted(String string, char character) {
		if ( '\'' == character ) {
			throw new IllegalArgumentException( "Unquoted count of quotes is invalid" );
		}
		if (string == null) {
			return new int[0];
		}

		ArrayList locations = new ArrayList( 20 );

		// Impl note: takes advantage of the fact that an escpaed single quote
		// embedded within a quote-block can really be handled as two seperate
		// quote-blocks for the purposes of this method...
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
				locations.add( indx );
			}
		}
		return ArrayHelper.toIntArray( locations );
	}

	public static boolean isNotEmpty(String string) {
		return string != null && string.length() > 0;
	}

	public static boolean isEmpty(String string) {
		return string == null || string.length() == 0;
	}

	public static boolean isEmptyOrWhiteSpace(String string){
		return isEmpty( string ) || isEmpty( string.trim() );
	}

	public static String qualify(String prefix, String name) {
		if ( name == null || prefix == null ) {
			throw new NullPointerException( "prefix or name were null attempting to build qualified name" );
		}
		return prefix + '.' + name;
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

	public static String[] qualifyIfNot(String prefix, String[] names) {
		if ( prefix == null ) {
			return names;
		}
		int len = names.length;
		String[] qualified = new String[len];
		for ( int i = 0; i < len; i++ ) {
			if ( names[i].indexOf( '.' ) < 0 ) {
				qualified[i] = qualify( prefix, names[i] );
			}
			else {
				qualified[i] = names[i];
			}
		}
		return qualified;
	}

	public static String qualifyIfNot(String qualifier, String name) {
		if ( isEmpty( name ) ) {
			return null;
		}

		if ( name.indexOf( '.' ) > 0 ) {
			return name;
		}

		return qualify( qualifier, name );
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
		return generateAliasRoot(description) + '_';
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
		return generateAliasRoot(description) +
			Integer.toString(unique) +
			'_';
	}

	/**
	 * Generates a root alias by truncating the "root name" defined by
	 * the incoming decription and removing/modifying any non-valid
	 * alias characters.
	 *
	 * @param description The root name from which to generate a root alias.
	 * @return The generated root alias.
	 */
	private static String generateAliasRoot(String description) {
		String result = truncate( unqualifyEntityName(description), ALIAS_TRUNCATE_LENGTH )
				// Important to use Locale.ENGLISH.  See HHH-8579.  #toLowerCase() uses the default Locale.  Certain DBs
				// do not like non-ascii characters in aliases, etc., so ensure consistency/portability here.
				.toLowerCase(Locale.ENGLISH)
		        .replace( '/', '_' ) // entityNames may now include slashes for the representations
				.replace( '$', '_' ); //classname may be an inner class
		result = cleanAlias( result );
		if ( Character.isDigit( result.charAt(result.length()-1) ) ) {
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
		String result = unqualify(entityName);
		int slashPos = result.indexOf( '/' );
		if ( slashPos > 0 ) {
			result = result.substring( 0, slashPos - 1 );
		}
		return result;
	}
	
	public static String toUpperCase(String str) {
		return str == null ? null : str.toUpperCase(Locale.ENGLISH);
	}
	
	public static String toLowerCase(String str) {
		// Important to use Locale.ENGLISH.  See HHH-8579.  #toLowerCase() uses the default Locale.  Certain DBs do not
		// like non-ascii characters in aliases, etc., so ensure consistency/portability here.
		return str == null ? null : str.toLowerCase(Locale.ENGLISH);
	}

	public static String moveAndToBeginning(String filter) {
		if ( filter.trim().length()>0 ){
			filter += " and ";
			if ( filter.startsWith(" and ") ) filter = filter.substring(4);
		}
		return filter;
	}

	/**
	 * Determine if the given string is quoted (wrapped by '`' characters at beginning and end).
	 *
	 * @param name The name to check.
	 * @return True if the given string starts and ends with '`'; false otherwise.
	 */
	public static boolean isQuoted(String name) {
		return name != null && name.length() != 0 
				&& ( ( name.charAt( 0 ) == '`' && name.charAt( name.length() - 1 ) == '`' )
						|| ( name.charAt( 0 ) == '"' && name.charAt( name.length() - 1 ) == '"' ) );
	}

	/**
	 * Return a representation of the given name ensuring quoting (wrapped with '`' characters).  If already wrapped
	 * return name.
	 *
	 * @param name The name to quote.
	 * @return The quoted version.
	 */
	public static String quote(String name) {
		if ( isEmpty( name ) || isQuoted( name ) ) {
			return name;
		}
		// Convert the JPA2 specific quoting character (double quote) to Hibernate's (back tick)
        else if ( name.startsWith( "\"" ) && name.endsWith( "\"" ) ) {
            name = name.substring( 1, name.length() - 1 );
        }

		return new StringBuilder( name.length() + 2 ).append('`').append( name ).append( '`' ).toString();
	}

	/**
	 * Return the unquoted version of name (stripping the start and end '`' characters if present).
	 *
	 * @param name The name to be unquoted.
	 * @return The unquoted version.
	 */
	public static String unquote(String name) {
		return isQuoted( name ) ? name.substring( 1, name.length() - 1 ) : name;
	}

	/**
	 * Determine if the given name is quoted.  It is considered quoted if either:
	 * <ol>
	 * <li>starts AND ends with backticks (`)</li>
	 * <li>starts with dialect-specified {@link org.hibernate.dialect.Dialect#openQuote() open-quote}
	 * 		AND ends with dialect-specified {@link org.hibernate.dialect.Dialect#closeQuote() close-quote}</li>
	 * </ol>
	 *
	 * @param name The name to check
	 * @param dialect The dialect (to determine the "real" quoting chars).
	 *
	 * @return True if quoted, false otherwise
	 */
	public static boolean isQuoted(String name, Dialect dialect) {
		return name != null && name.length() != 0 
				&& ( ( name.charAt( 0 ) == '`' && name.charAt( name.length() - 1 ) == '`' )
						|| ( name.charAt( 0 ) == '"' && name.charAt( name.length() - 1 ) == '"' )
						|| ( name.charAt( 0 ) == dialect.openQuote()
								&& name.charAt( name.length() - 1 ) == dialect.closeQuote() ) );
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
	public static String[] unquote(String[] names, Dialect dialect) {
		if ( names == null ) {
			return null;
		}
		String[] unquoted = new String[ names.length ];
		for ( int i = 0; i < names.length; i++ ) {
			unquoted[i] = unquote( names[i], dialect );
		}
		return unquoted;
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
					.append( " in (" ).append( BATCH_ID_PLACEHOLDER ).append( ")" );
		}
		else {
			// composite key - the form to use here depends on what the dialect supports.
			if ( dialect.supportsRowValueConstructorSyntaxInInList() ) {
				// use : (col1, col2) in ( (?,?), (?,?), ... )
				StringBuilder builder = new StringBuilder();
				builder.append( "(" );
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
				builder.append( ")" );
				return builder;
			}
			else {
				// use : ( (col1 = ? and col2 = ?) or (col1 = ? and col2 = ?) or ... )
				//		unfortunately most of this building needs to be held off until we know
				//		the exact number of ids :(
				return new StringBuilder( "(" ).append( BATCH_ID_PLACEHOLDER ).append( ")" );
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
				final String tuple = "(" + StringHelper.repeat( "?", keyColumnNames.length, "," );
				return StringHelper.replace( sql, BATCH_ID_PLACEHOLDER, repeat( tuple, ids.length, "," ) );
			}
			else {
				final String keyCheck = joinWithQualifier( keyColumnNames, alias, " and " );
				return replace( sql, BATCH_ID_PLACEHOLDER, repeat( keyCheck, ids.length, " or " ) );
			}
		}
	}
	
	/**
	 * Takes a String s and returns a new String[1] with s as the only element.
	 * If s is null or "", return String[0].
	 * 
	 * @param s
	 * @return String[]
	 */
	public static String[] toArrayElement(String s) {
		return ( s == null || s.length() == 0 ) ? new String[0] : new String[] { s };
	}

	public static String makePath(String base, String name) {
		if ( isEmpty( name ) ) {
			throw new IllegalArgumentException( "Name used to make path cannot be empty" );
		}

		return isEmpty( base ) ? name : base + '.' + name;
	}

	public static String nullIfEmpty(String value) {
		return isEmpty( value ) ? null : value;
	}
	
	public static boolean containsIgnoreCase(List<String> list, String s) {
		s = toLowerCase( s );
		for (int i = 0; i < list.size(); i++) {
			list.set( i, toLowerCase( list.get( i ) ) );
		}
		return list.contains( s );
	}
}
