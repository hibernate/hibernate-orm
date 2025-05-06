/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MutableBoolean {
	private boolean value;

	public MutableBoolean() {
	}

	public MutableBoolean(boolean value) {
		this.value = value;
	}

	public boolean isSet() {
		return value;
	}

	public void set() {
		value = true;
	}

	public void unset() {
		value = false;
	}
}
