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
 */
package org.hibernate.annotations.common.util;

import java.util.Iterator;
import java.util.Collection;

/**
 * Complete duplication of {@link org.hibernate.util.StringHelper}.
 *
 * @author Emmanuel Bernard
 * @deprecated Use {@link org.hibernate.util.StringHelper} instead.
 */
public final class StringHelper {

	private static final int ALIAS_TRUNCATE_LENGTH = 10;
	public static final String WHITESPACE = " \n\r\f\t";

	private StringHelper() {
	}

	public static int lastIndexOfLetter(String string) {
		return org.hibernate.util.StringHelper.lastIndexOfLetter( string );
	}

	public static String join(String seperator, String[] strings) {
		return org.hibernate.util.StringHelper.join( seperator, strings );
	}

	public static String join(String seperator, Iterator objects) {
		return org.hibernate.util.StringHelper.join( seperator, objects );
	}

	public static String[] add(String[] x, String sep, String[] y) {
		return org.hibernate.util.StringHelper.add( x, sep, y );
	}

	public static String repeat(String string, int times) {
		return org.hibernate.util.StringHelper.repeat( string, times );
	}

	public static String replace(String template, String placeholder, String replacement) {
		return org.hibernate.util.StringHelper.replace( template, placeholder, replacement );
	}

	public static String[] replace(String templates[], String placeholder, String replacement) {
		return org.hibernate.util.StringHelper.replace( templates, placeholder, replacement );
	}

	public static String replace(String template, String placeholder, String replacement, boolean wholeWords) {
		return org.hibernate.util.StringHelper.replace( template, placeholder, replacement, wholeWords );
	}

	public static String replaceOnce(String template, String placeholder, String replacement) {
		return org.hibernate.util.StringHelper.replaceOnce( template, placeholder, replacement );
	}

	public static String[] split(String seperators, String list) {
		return org.hibernate.util.StringHelper.split( seperators, list );
	}

	public static String[] split(String seperators, String list, boolean include) {
		return org.hibernate.util.StringHelper.split( seperators, list, include );
	}

	public static String unqualify(String qualifiedName) {
		return org.hibernate.util.StringHelper.unqualify( qualifiedName );
	}

	public static String qualify(String prefix, String name) {
		return org.hibernate.util.StringHelper.qualify( prefix, name );
	}

	public static String[] qualify(String prefix, String[] names) {
		return org.hibernate.util.StringHelper.qualify( prefix, names );
	}

	public static String qualifier(String qualifiedName) {
		return org.hibernate.util.StringHelper.qualifier( qualifiedName );
	}

	public static String[] suffix(String[] columns, String suffix) {
		return org.hibernate.util.StringHelper.suffix( columns, suffix );
	}

	public static String root(String qualifiedName) {
		return org.hibernate.util.StringHelper.root( qualifiedName );
	}

	public static String unroot(String qualifiedName) {
		return org.hibernate.util.StringHelper.unroot( qualifiedName );
	}

	public static boolean booleanValue(String tfString) {
		return org.hibernate.util.StringHelper.booleanValue( tfString );
	}

	public static String toString(Object[] array) {
		return org.hibernate.util.StringHelper.toString( array );
	}

	public static String[] multiply(String string, Iterator placeholders, Iterator replacements) {
		return org.hibernate.util.StringHelper.multiply( string, placeholders, replacements );
	}

	public static int countUnquoted(String string, char character) {
		return org.hibernate.util.StringHelper.countUnquoted( string, character );
	}

	public static int[] locateUnquoted(String string, char character) {
		return org.hibernate.util.StringHelper.locateUnquoted( string, character );
	}

	public static boolean isNotEmpty(String string) {
		return org.hibernate.util.StringHelper.isNotEmpty( string );
	}

	public static boolean isEmpty(String string) {
		return org.hibernate.util.StringHelper.isEmpty( string );
	}

	public static int firstIndexOfChar(String sqlString, String string, int startindex) {
		return org.hibernate.util.StringHelper.firstIndexOfChar( sqlString, string, startindex );
	}

	public static String truncate(String string, int length) {
		return org.hibernate.util.StringHelper.truncate( string, length );
	}

	public static String generateAlias(String description) {
		return org.hibernate.util.StringHelper.generateAlias( description );
	}

	public static String generateAlias(String description, int unique) {
		return org.hibernate.util.StringHelper.generateAlias( description, unique );
	}

	public static String unqualifyEntityName(String entityName) {
		return org.hibernate.util.StringHelper.unqualifyEntityName( entityName );
	}

	public static String toUpperCase(String str) {
		return org.hibernate.util.StringHelper.toUpperCase( str );
	}

	public static String toLowerCase(String str) {
		return org.hibernate.util.StringHelper.toLowerCase( str );
	}

	public static String moveAndToBeginning(String filter) {
		return org.hibernate.util.StringHelper.moveAndToBeginning( filter );
	}

	/**
	 * Not a direct copy from {@link org.hibernate.util.StringHelper}, this is instead directly copied
	 * from {@link org.hibernate.util.ArrayHelper}.
	 *
	 * @param coll the collection
	 * @return The int array
	 * @deprecated Use {@link org.hibernate.util.ArrayHelper#toIntArray} instead.
	 */
	public static int[] toIntArray(Collection coll) {
		return org.hibernate.util.ArrayHelper.toIntArray( coll );
	}

	public static boolean isQuoted(String name) {
		return org.hibernate.util.StringHelper.isQuoted( name );
	}

	public static String quote(String name) {
		return org.hibernate.util.StringHelper.quote( name );
	}

	public static String unquote(String name) {
		return org.hibernate.util.StringHelper.unquote( name );
	}
}