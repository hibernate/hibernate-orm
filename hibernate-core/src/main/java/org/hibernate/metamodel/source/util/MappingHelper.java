/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.source.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.dom4j.Element;

import org.hibernate.internal.util.ReflectHelper;

/**
 * Helper class for working with DOM documents.
 *
 * @author Gail Badner
 */
public class MappingHelper {
	private MappingHelper() {
	}

	public static String extractAttributeValue(Element element, String attributeName) {
		return extractAttributeValue( element, attributeName, null );
	}

	public static String extractAttributeValue(Element element, String attributeName, String defaultValue) {
		String attributeValue = ( element == null ? null : element.attributeValue( attributeName ) );
		return attributeValue == null ? defaultValue : attributeValue;
	}

	public static String getStringValue(String value, String defaultValue) {
		return value == null ? defaultValue : value;
	}

	public static int extractIntAttributeValue(Element element, String attributeName) {
		return extractIntAttributeValue( element, attributeName, -1 );
	}

	public static int extractIntAttributeValue(Element element, String attributeName, int defaultValue) {
		String attributeValue = ( element == null ? null : element.attributeValue( attributeName ) );
		return attributeValue == null ? defaultValue : Integer.valueOf( attributeValue );
	}

	public static int getIntValue(String value, int defaultValue) {
		return value == null ? defaultValue : Integer.parseInt( value );
	}

	public static boolean extractBooleanAttributeValue(Element element, String attributeName) {
		return extractBooleanAttributeValue( element, attributeName, false );
	}

	public static boolean extractBooleanAttributeValue(Element element, String attributeName, boolean defaultValue) {
		String attributeValue = ( element == null ? null : element.attributeValue( attributeName ) );
		return attributeValue == null ? defaultValue : Boolean.valueOf( attributeValue );
	}

	public static boolean getBooleanValue(String value, boolean defaultValue) {
		return value == null ? defaultValue : Boolean.valueOf( value );
	}

	public static Class extractClassAttributeValue(Element element, String attributeName)
	throws ClassNotFoundException {
		String attributeValue = ( element == null ? null : element.attributeValue( attributeName ) );
		return (
				attributeValue == null ?
				null :
				ReflectHelper.classForName( attributeValue )
		);
	}

	public static Class getClassValue(String className)
	throws ClassNotFoundException {
		return (
				className == null ?
				null :
				ReflectHelper.classForName( className )
		);
	}

	public static Set<String> getStringValueTokens(String str, String delimiters) {
		if ( str == null ) {
			return Collections.emptySet();
		}
		else {
			StringTokenizer tokenizer = new StringTokenizer( str, delimiters );
			Set<String> tokens = new HashSet<String>();
			while ( tokenizer.hasMoreTokens() ) {
				tokens.add( tokenizer.nextToken() );
			}
			return tokens;
		}
	}
}
