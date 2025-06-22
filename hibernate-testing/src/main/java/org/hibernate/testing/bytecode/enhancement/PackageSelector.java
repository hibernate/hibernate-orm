/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

/**
 * EnhancementSelector based on package name
 *
 * @author Steve Ebersole
 */
public class PackageSelector implements EnhancementSelector {
	private final String packageName;

	public PackageSelector(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public boolean select(String name) {
		return name.startsWith( packageName );
	}
}
