/*
 * SPDX-License-Identifier: Apache-2.0
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
