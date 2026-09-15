/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.env;

import java.util.Optional;

public final class CiEnvironment {

	private CiEnvironment() {
	}

	public static boolean isCiEnvironment() {
		return isJenkins() || isGitHubActions() || isGenericCi();
	}

	private static boolean isJenkins() {
		return getSetting( "JENKINS_URL" ).isPresent();
	}

	private static boolean isGitHubActions() {
		return getSetting( "GITHUB_ACTIONS" ).isPresent();
	}

	private static boolean isGenericCi() {
		return System.getenv( "CI" ) != null || System.getProperty( "CI" ) != null;
	}

	public static Optional<String> getSetting(String name) {
		String envVar = System.getenv( name );
		if ( envVar != null ) {
			return Optional.of( envVar );
		}
		String sysProp = System.getProperty( name );
		return Optional.ofNullable( sysProp );
	}
}
