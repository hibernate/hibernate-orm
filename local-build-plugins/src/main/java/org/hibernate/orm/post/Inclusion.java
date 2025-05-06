/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

/**
 * @author Steve Ebersole
 */
class Inclusion {
	private final String path;
	private final boolean isPackage;

	public Inclusion(String path, boolean isPackage) {
		this.path = path;
		this.isPackage = isPackage;
	}

	public Inclusion(String path) {
		this( path, false );
	}

	public String getPath() {
		return path;
	}

	public boolean isPackage() {
		return isPackage;
	}

}
