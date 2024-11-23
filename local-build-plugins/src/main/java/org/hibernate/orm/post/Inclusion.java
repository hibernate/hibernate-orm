/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
