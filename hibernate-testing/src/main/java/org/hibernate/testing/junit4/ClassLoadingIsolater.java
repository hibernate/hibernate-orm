/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.junit4;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ClassLoadingIsolater implements MethodRule {
	private static final Logger log = Logger.getLogger( ClassLoadingIsolater.class );

	public static interface IsolatedClassLoaderProvider {
		ClassLoader buildIsolatedClassLoader();
		void releaseIsolatedClassLoader(ClassLoader isolatedClassLoader);
	}

	private final IsolatedClassLoaderProvider provider;

	public ClassLoadingIsolater(IsolatedClassLoaderProvider provider) {
		this.provider = provider;
	}

	@Override
	public Statement apply(final Statement base, FrameworkMethod method, Object target) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				final ClassLoader originalTCCL = Thread.currentThread().getContextClassLoader();
				final ClassLoader isolatedClassLoader = provider.buildIsolatedClassLoader();

				log.infof( "Overriding TCCL [%s] -> [%s]", originalTCCL, isolatedClassLoader );

				Thread.currentThread().setContextClassLoader( isolatedClassLoader );

				try {
					base.evaluate();
				}
				finally {
					assert Thread.currentThread().getContextClassLoader() == isolatedClassLoader;
					log.infof( "Reverting TCCL [%s] -> [%s]", isolatedClassLoader, originalTCCL );

					Thread.currentThread().setContextClassLoader( originalTCCL );
					provider.releaseIsolatedClassLoader( isolatedClassLoader );
				}
			}
		};
	}
}
