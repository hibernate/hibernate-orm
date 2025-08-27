/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;

/**
 * Extends the TypePool contract of ByteBuddy with our additional needs.
 */
public interface EnhancerClassLocator extends TypePool {

	/**
	 * Register a new class to the locator explicitly.
	 * @param className
	 * @param originalBytes
	 */
	void registerClassNameAndBytes(String className, byte[] originalBytes);

	/**
	 * This can optionally be used to remove an explicit mapping when it's no longer
	 * essential to retain it.
	 * The underlying implementation might ignore the operation.
	 * @param className
	 */
	void deregisterClassNameAndBytes(String className);

	/**
	 * @return the underlying {@link ClassFileLocator}
	 */
	ClassFileLocator asClassFileLocator();
}
