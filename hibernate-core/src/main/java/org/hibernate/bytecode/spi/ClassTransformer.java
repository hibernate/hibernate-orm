/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.spi;

import java.security.ProtectionDomain;

import jakarta.persistence.spi.TransformerException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A persistence provider provides an instance of this interface to the
 * {@link jakarta.persistence.spi.PersistenceUnitInfo#addTransformer} method.
 * The supplied transformer instance will get called to transform entity class
 * files when they are loaded and redefined. The transformation occurs before
 * the class is defined by the JVM.
 *
 * @author Bill Burke
 * @author Emmanuel Bernard
 */
public interface ClassTransformer extends jakarta.persistence.spi.ClassTransformer {
	/**
	 * Invoked when a class is being loaded or redefined to add hooks for persistence
	 * bytecode manipulation.
	 *
	 * @param loader The defining class loader of the class being transformed.
	 *               It may be null if using bootstrap loader
	 * @param className The name of the class being transformed
	 * @param classBeingRedefined If an already loaded class is being redefined,
	 *                            then pass this as a parameter
	 * @param protectionDomain ProtectionDomain of the class being (re)-defined
	 * @param classfileBuffer The input byte buffer in class file format
	 * @return A well-formed class file that can be loaded
	 */
	@Override
	byte[] transform(
			@Nullable ClassLoader loader,
			String className,
			@Nullable Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer)  throws TransformerException;

	void discoverTypes(ClassLoader loader, String className);
}
