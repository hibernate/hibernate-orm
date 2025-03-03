/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * A more performant version of {@link java.util.concurrent.atomic.AtomicInteger} in cases
 * where we do not have to worry about concurrency.  So usually as a variable referenced in
 * anonymous-inner or lambda or ...
 *
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class MutableInteger {
	private int value;

	public MutableInteger() {
		this( 0 );
	}

	public MutableInteger(int value) {
		this.value = value;
	}

	public MutableInteger deepCopy() {
		return new MutableInteger( value );
	}

	public int getAndIncrement() {
		return value++;
	}

	public int getAndIncrementBy(int i) {
		final int local = value;
		value += i;
		return local;
	}

	public int incrementAndGet() {
		return ++value;
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

	public void increment() {
		++value;
	}

	public void incrementBy(int i) {
		value += i;
	}

	public void increase(int i) {
		value += i;
	}

	public void plus(int i) {
		value += i;
	}

	public void minus(int i) {
		value -= i;
	}
}
