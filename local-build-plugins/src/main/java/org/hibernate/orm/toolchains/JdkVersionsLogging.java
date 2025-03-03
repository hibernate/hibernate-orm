/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
