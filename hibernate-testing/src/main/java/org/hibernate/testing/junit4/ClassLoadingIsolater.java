/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * @author Steve Ebersole
 */
public class ClassLoadingIsolater implements MethodRule {
	public static interface IsolatedClassLoaderProvider {
		public ClassLoader buildIsolatedClassLoader();
		public void releaseIsolatedClassLoader(ClassLoader isolatedClassLoader);
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
				final ClassLoader isolatedClassLoader = provider.buildIsolatedClassLoader();
				final ClassLoader originalTCCL = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader( isolatedClassLoader );

				try {
					base.evaluate();
				}
				finally {
					Thread.currentThread().setContextClassLoader( originalTCCL );
					provider.releaseIsolatedClassLoader( isolatedClassLoader );
				}
			}
		};
	}
}
