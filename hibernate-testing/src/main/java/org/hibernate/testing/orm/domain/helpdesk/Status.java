/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.helpdesk;

public enum Status {
	CREATED,
	INITIALIZING,
	ACTIVE,
	INACTIVE;

	private final int code;

	Status() {
		this.code = this.ordinal() + 1000;
	}

	public int getCode() {
		return code;
	}

	public static Status fromCode(Integer code) {
		if ( code == null ) {
			return null;
		}
		return values()[ code - 1000 ];
	}
}
