/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * Support for mutable boolean references, generally used from within
 * anon inner classes, lambdas, etc
 *
 * @author Steve Ebersole
 */
public class MutableBoolean {
	private boolean value;

	public MutableBoolean() {
	}

	public MutableBoolean(boolean value) {
		this.value = value;
	}

	public boolean getValue() {
		return value;
	}

	public void setValue(boolean value) {
		this.value = value;
	}
}
