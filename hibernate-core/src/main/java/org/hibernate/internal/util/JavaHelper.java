/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util;

/**
 * @author Steve Ebersole
 */
public class JavaHelper {
	public static Package getPackageFor(String name) {
		return getPackageFor( name, JavaHelper.class.getClassLoader() );
	}

	public static Package getPackageFor(String name, ClassLoader classLoader) {
		// after Java 9 we can do -
		//return classLoader.getDefinedPackage( name );

		return Package.getPackage( name );
	}

	private JavaHelper() {
		// disallow direct instantiation
	}
}
