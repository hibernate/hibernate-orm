/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
