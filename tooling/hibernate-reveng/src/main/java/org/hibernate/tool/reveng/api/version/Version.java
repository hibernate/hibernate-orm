/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.version;

public interface Version {

	/**
	 * @deprecated Use {@link #versionString()} instead.
	 */
	@Deprecated
	final static String CURRENT_VERSION = versionString();

	static String versionString() {
		return org.hibernate.Version.getVersionString();
	}
}
