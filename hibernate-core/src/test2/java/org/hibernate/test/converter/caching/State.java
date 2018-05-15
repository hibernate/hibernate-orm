/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter.caching;

import org.hibernate.annotations.Immutable;

/**
 * @author Steve Ebersole
 */
@Immutable
public enum State {
	TX( "TX", "Texas" );

	private final String code;
	private final String name;

	State(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
