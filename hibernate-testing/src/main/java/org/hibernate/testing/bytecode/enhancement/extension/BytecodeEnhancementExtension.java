/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement.extension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

/**
 * Jupiter extension used by {@link BytecodeEnhanced} tests while they are being executed by the bytecode enhancement
 * engine.
 * <p>
 * The extension does not create enhanced classes and does not decide which tests are enhanced.  That is handled by
 * {@link org.hibernate.testing.bytecode.enhancement.extension.engine.BytecodeEnhancedTestEngine}.  By the time Jupiter
 * invokes this extension, the test class is already the variant class selected by the engine.
 * <p>
 * Its job is to make the variant class loader the thread context class loader while Jupiter constructs and owns the
 * test instance.  This keeps test code and Jupiter extensions that rely on the context class loader aligned with the
 * class loader that loaded the enhanced test class and selected domain classes.
 */
public class BytecodeEnhancementExtension implements TestInstancePreConstructCallback, TestInstancePreDestroyCallback {

	private ClassLoader originalClassLoader;

	@Override
	public void preConstructTestInstance(TestInstanceFactoryContext testInstanceFactoryContext,
			ExtensionContext extensionContext) {
		originalClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader( testInstanceFactoryContext.getTestClass().getClassLoader() );
	}

	@Override
	public void preDestroyTestInstance(ExtensionContext extensionContext) {
		Thread.currentThread().setContextClassLoader( originalClassLoader );
	}
}
