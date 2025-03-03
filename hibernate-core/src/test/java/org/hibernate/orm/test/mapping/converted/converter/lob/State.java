/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.lob;

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
