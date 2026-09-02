/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Calculates the default compatibility baseline from an ORM source version.
///
/// @author Steve Ebersole
/// @since 8.0
final class MigrationCompatibilityFamilies {
	private static final Pattern VERSION = Pattern.compile( "^(\\d+)\\.(\\d+)\\.(\\d+)(?:[.-].*)?$" );
	private static final Pattern FAMILY = Pattern.compile( "^\\d+\\.\\d+$" );

	private MigrationCompatibilityFamilies() {
	}

	static String defaultBaseline(String version) {
		final Matcher matcher = VERSION.matcher( version );
		if ( !matcher.matches() ) {
			throw new IllegalArgumentException( "Hibernate ORM version must begin with major.minor.patch: " + version );
		}
		final int major = Integer.parseInt( matcher.group( 1 ) );
		final int minor = Integer.parseInt( matcher.group( 2 ) );
		final int patch = Integer.parseInt( matcher.group( 3 ) );
		if ( patch > 0 ) {
			return major + "." + minor;
		}
		return minor == 0 ? null : major + "." + (minor - 1);
	}

	static String requireFamily(String family) {
		if ( !FAMILY.matcher( family ).matches() ) {
			throw new IllegalArgumentException( "Hibernate ORM compatibility family must have form X.Y: " + family );
		}
		return family;
	}
}
