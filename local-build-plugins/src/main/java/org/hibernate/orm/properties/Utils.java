/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jsoup.nodes.Element;

/**
 * @author Steve Ebersole
 */
public class Utils {
	public static String fieldJavadocLink(String publishedJavadocsUrl, String className, String simpleFieldName) {
		final String packageRelativePath = packagePrefix( className ).replace( ".", File.separator );
		final String classRelativePath = packageRelativePath + "/" + withoutPackagePrefix( className ) + ".html";
		return publishedJavadocsUrl + classRelativePath + "#" + simpleFieldName;
	}

	public static String withoutPackagePrefix(String className) {
		return className.substring( className.lastIndexOf( '.' ) + 1 );
	}

	public static String packagePrefix(String className) {
		return className.substring( 0, className.lastIndexOf( '.' ) );
	}

	public static Map<SettingsDocSection, SortedSet<SettingDescriptor>> createResultMap(Map<String, SettingsDocSection> sections) {
		final TreeMap<SettingsDocSection, SortedSet<SettingDescriptor>> map = new TreeMap<>( SettingsDocSection::compare );
		sections.forEach( (name, descriptor) -> {
			map.put( descriptor, new TreeSet<>( SettingDescriptorComparator.INSTANCE ) );
		} );
		return map;
	}

	public static boolean containsHref(Element fieldJavadocElement, String target) {
		final String cssQuery = "a[href$=" + target + "]";
		final Element incubatingMarkerElement = fieldJavadocElement.selectFirst( cssQuery );
		return incubatingMarkerElement != null;

	}

	public static boolean interpretIncubation(Element fieldJavadocElement) {
		return containsHref( fieldJavadocElement, "Incubating.html" );
	}

	public static boolean interpretUnsafe(Element fieldJavadocElement) {
		return containsHref( fieldJavadocElement, "Unsafe.html" );
	}

	public static boolean interpretCompatibility(Element fieldJavadocElement) {
		return containsHref( fieldJavadocElement, "Compatibility.html" );
	}

	public static boolean interpretDeprecation(Element fieldJavadocElement) {
		// A setting is considered deprecated with either `@Deprecated`
		final Element deprecationDiv = fieldJavadocElement.selectFirst( ".deprecationBlock" );
		// presence of this <div/> indicates the member is deprecated
		if ( deprecationDiv != null ) {
			return true;
		}

		// or `@Remove`
		if ( containsHref( fieldJavadocElement, "Remove.html" ) ) {
			return true;
		}

		return false;
	}
}
