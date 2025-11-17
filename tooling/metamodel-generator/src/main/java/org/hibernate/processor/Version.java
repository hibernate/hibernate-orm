/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor;

import org.hibernate.processor.util.NullnessUtil;

/**
 * Information about the Meta Model Generator version.
 *
 * @author Hardy Ferentschik
 */
public final class Version {
	private static final String VERSION = initVersion();

	private static String initVersion() {
		final String version = NullnessUtil.castNonNull( Version.class.getPackage() ).getImplementationVersion();
		return version != null ? version : "[WORKING]";
	}

	private Version() {
	}

	public static String getVersionString() {
		return VERSION;
	}
}
