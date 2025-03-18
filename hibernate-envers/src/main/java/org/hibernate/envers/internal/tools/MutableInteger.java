/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MutableInteger {
	private int value;

	public MutableInteger() {
	}

	public MutableInteger(int value) {
		this.value = value;
	}

	public MutableInteger deepCopy() {
		return new MutableInteger( value );
	}

	public int getAndIncrease() {
		return value++;
	}

	public int get() {
		return value;
	}

	public void set(int value) {
		this.value = value;
	}

	public void increase() {
		++value;
	}
}
