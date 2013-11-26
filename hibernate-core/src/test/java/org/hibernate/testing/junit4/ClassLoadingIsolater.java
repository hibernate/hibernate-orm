/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
