/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * A more performant version of {@link java.util.concurrent.atomic.AtomicLong} in cases
 * where we do not have to worry about concurrency.  So usually as a variable referenced in
 * anonymous-inner or lambda or ...
 *
 * @author Andrea Boriero
 */
public class MutableLong {
	private long value;

	public MutableLong() {
	}

	public MutableLong(long value) {
		this.value = value;
	}

	public MutableLong deepCopy() {
		return new MutableLong( value );
	}

	public long getAndIncrement() {
		return value++;
	}

	public long incrementAndGet() {
		return ++value;
	}

	public long get() {
		return value;
	}

	public void set(long value) {
		this.value = value;
	}

	public void increase() {
		++value;
	}
}
