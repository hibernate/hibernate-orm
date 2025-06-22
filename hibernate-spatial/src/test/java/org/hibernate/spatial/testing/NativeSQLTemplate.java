/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import java.util.Locale;

/**
 * Template for native SQL statement
 * <p>
 * This is mainly introduced to avoid a confusion between template and non-template Strings.
 */
public class NativeSQLTemplate {

	final private String nativeSql;

	public NativeSQLTemplate(String template) {
		this.nativeSql = template;
	}

	public String mkNativeSQLString(Object... params) {
		return String.format( Locale.ROOT, nativeSql, params );
	}
}
