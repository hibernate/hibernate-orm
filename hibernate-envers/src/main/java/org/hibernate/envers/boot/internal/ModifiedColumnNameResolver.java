/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

/**
 * @author Chris Cranford
 */
public class ModifiedColumnNameResolver {
	public static String getName(String propertyName, String suffix) {
		return propertyName + suffix;
	}

	private ModifiedColumnNameResolver() {
	}
}
