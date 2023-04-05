/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen;

import org.hibernate.jpamodelgen.util.NullnessUtil;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Information about the Meta Model Generator version.
 *
 * @author Hardy Ferentschik
 */
public final class Version {
	private static @Nullable String version;

	private Version() {
	}

	public static String getVersionString() {
		if ( version == null ) {
			version = NullnessUtil.castNonNull( Version.class.getPackage() ).getImplementationVersion();
			if ( version == null ) {
				version = "[WORKING]";
			}
		}
		return version;
	}
}
