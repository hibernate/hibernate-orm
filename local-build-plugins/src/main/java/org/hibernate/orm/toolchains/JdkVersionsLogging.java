/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.toolchains;

/**
 * @author Steve Ebersole
 */
public class JdkVersionsLogging {
	private static boolean logged = false;

	public static void logVersions(JdkVersionConfig jdkVersionConfig) {
		if ( logged ) {
			return;
		}

		logged = true;

		final String implicitExplicitString = jdkVersionConfig.isExplicit() ? "explicit" : "implicit";

		System.out.println(
				"Java versions for main code: " + jdkVersionConfig.getMain()
						+ " (" + implicitExplicitString + ")"
		);
		System.out.println(
				"Java versions for test code: " + jdkVersionConfig.getTest()
						+ " (" + implicitExplicitString + ")"
		);
	}

	public static void logOnce(String message) {
		if ( logged ) {
			return;
		}

		logged = true;

		System.out.println( message );
	}
}
