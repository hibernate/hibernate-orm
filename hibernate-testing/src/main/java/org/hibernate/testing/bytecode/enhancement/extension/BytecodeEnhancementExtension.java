/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement.extension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

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
