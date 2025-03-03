/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;

/**
 * The most general abstraction over collections which may be fetched lazily.
 * <ul>
 * <li>Hibernate core "wraps" a Java collection in an instance of
 *     {@link PersistentCollection}.
 * <li>Similarly, Envers uses its own custom collection wrappers:
 *     {@code ListProxy}, {@code SetProxy}, and friends).
 * </ul>
 * <p>
 * All of these wrapper objects extend {@code LazyInitializable}, allowing:
 * <ul>
 * <li>the method {@link org.hibernate.Hibernate#isInitialized(Object)} to
 *     determine if the collection was already fetched, and
 * <li>the method {@link org.hibernate.Hibernate#initialize(Object)} to
 *     force it to be fetched.
 * </ul>
 *
 * @author Fabricio Gregorio
 */
@Incubating
public interface LazyInitializable {

	/**
	 * Is this instance initialized?
	 *
	 * @return Was this collection initialized? Or is its data still not (fully) loaded?
	 *
	 * @see org.hibernate.Hibernate#isInitialized(Object)
	 */
	boolean wasInitialized();

	/**
	 * To be called internally by the session, forcing immediate initialization.
	 *
	 * @see org.hibernate.Hibernate#initialize(Object)
	 */
	void forceInitialization();

}
