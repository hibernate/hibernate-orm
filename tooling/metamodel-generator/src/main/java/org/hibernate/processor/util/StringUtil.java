/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import java.util.Locale;

import static java.lang.Character.charCount;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toUpperCase;

/**
 * @author Hardy Ferentschik
 */
public final class StringUtil {
	private static final String GET = "get";
	private static final String IS = "is";
	private static final String HAS = "has";

	private StringUtil() {
	}

	public static String determineFullyQualifiedClassName(String defaultPackage, String name) {
		return isFullyQualified( name ) ? name : defaultPackage + "." + name;
	}

	public static boolean isFullyQualified(String name) {
		return name.contains(".");
	}

	public static String packageNameFromFullyQualifiedName(String fullyQualifiedName) {
		return fullyQualifiedName.substring( 0, fullyQualifiedName.lastIndexOf(".") );
	}

	public static String classNameFromFullyQualifiedName(String fullyQualifiedName) {
		return fullyQualifiedName.substring( fullyQualifiedName.lastIndexOf(".") + 1 );
	}

	public static boolean isProperty(String methodName, String returnType) {
		if ( methodName == null ) {
			return false;
		}
		else {
			return !isVoid( returnType )
					&& isValidPropertyName( methodName, GET )
				|| isBoolean( returnType )
					&& ( isValidPropertyName( methodName, IS )
						|| isValidPropertyName( methodName, HAS ) );
		}

	}

	private static boolean isVoid(String returnType) {
		return "void".equals( returnType );
	}

	private static boolean isBoolean(String type) {
		return "Boolean".equals( type ) || "java.lang.Boolean".equals( type ) || "boolean".equals( type );
	}

	private static boolean isValidPropertyName(String name, String prefix) {
		// the name has to start with the prefix and have at least one more character
		return name.startsWith( prefix ) && name.length() > prefix.length();
	}

	public static String getPropertyName(String name) {
		return decapitalize( trimPropertyPrefix( name ) );
	}

	private static String trimPropertyPrefix(String name) {
		if ( name.startsWith( GET ) ) {
			return name.replaceFirst( GET, "" );
		}
		else if ( name.startsWith( IS ) ) {
			return name.replaceFirst( IS, "" );
		}
		else if ( name.startsWith( HAS ) ) {
			return name.replaceFirst( HAS, "" );
		}
		else {
			return name;
		}
	}

	public static String decapitalize(String string) {
		return string == null || string.isEmpty() || startsWithSeveralUpperCaseLetters( string )
				? string
				: string.substring( 0, 1 ).toLowerCase(Locale.ROOT) + string.substring( 1 );
	}

	public static String nameToFieldName(String name){
		return getUpperUnderscoreCaseFromLowerCamelCase( nameToMethodName( name ) );
	}

	public static String nameToMethodName(String name) {
		return name.replaceAll("[\\s.\\-!@#%=+/*^&|(){}\\[\\],]", "_");
	}

	public static String getUpperUnderscoreCaseFromLowerCamelCase(String lowerCamelCaseString) {
		final StringBuilder result = new StringBuilder();
		int position = 0;
		boolean wasLowerCase = false;
		while ( position < lowerCamelCaseString.length() ) {
			final int codePoint = lowerCamelCaseString.codePointAt( position );
			final boolean isUpperCase = isUpperCase( codePoint );
			if ( wasLowerCase && isUpperCase ) {
				result.append('_');
			}
			result.appendCodePoint( toUpperCase( codePoint ) );
			position += charCount( codePoint );
			wasLowerCase = !isUpperCase;
		}
		if ( result.toString().equals( lowerCamelCaseString ) ) {
			result.insert(0, '_');
		}
		return result.toString();
	}

	private static boolean startsWithSeveralUpperCaseLetters(String string) {
		return string.length() > 1
			&& isUpperCase( string.charAt( 0 ) )
			&& isUpperCase( string.charAt( 1 ) );
	}

	/**
	 * If this is an "intermediate" class providing {@code @Query}
	 * annotations for the query by magical method name crap, then
	 * by convention it will be named with a trailing $ sign. Strip
	 * that off, so we get the standard constructor.
	 */
	public static String removeDollar(String simpleName) {
		return simpleName.endsWith("$")
				? simpleName.substring(0, simpleName.length()-1)
				: simpleName;
	}
}
