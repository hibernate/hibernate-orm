/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.tracker;

/**
 * Interface to be implemented by dirty trackers, a simplified Set of String.
 *
 * @author Luis Barreiro
 */
public interface DirtyTracker {

	void add(String name);

	boolean contains(String name);

	void clear();

	boolean isEmpty();

	String[] get();

	void suspend(boolean suspend);
}
