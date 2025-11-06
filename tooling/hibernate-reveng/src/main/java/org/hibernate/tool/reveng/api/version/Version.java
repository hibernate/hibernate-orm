/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.version;

public interface Version {

	static String versionString() {
		return org.hibernate.Version.getVersionString();
	}

}
