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
}
