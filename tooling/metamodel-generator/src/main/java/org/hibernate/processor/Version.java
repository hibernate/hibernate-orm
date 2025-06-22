/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor;

import org.hibernate.processor.util.NullnessUtil;

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
