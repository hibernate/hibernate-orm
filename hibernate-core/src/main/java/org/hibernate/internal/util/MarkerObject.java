/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public class MarkerObject implements Serializable {
	private String name;

	public MarkerObject(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
