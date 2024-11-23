/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.jboss.logging.Logger;

public class ClassLoadingIsolaterExtension implements AfterEachCallback, BeforeEachCallback {

	private static final Logger log = Logger.getLogger( ClassLoadingIsolaterExtension.class );

	private IsolatedClassLoaderProvider provider;

	public interface IsolatedClassLoaderProvider {
		ClassLoader buildIsolatedClassLoader();

		void releaseIsolatedClassLoader(ClassLoader isolatedClassLoader);
	}


	private ClassLoader isolatedClassLoader;
	private ClassLoader originalTCCL;

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		assert Thread.currentThread().getContextClassLoader() == isolatedClassLoader;
		log.infof( "Reverting TCCL [%s] -> [%s]", isolatedClassLoader, originalTCCL );

		Thread.currentThread().setContextClassLoader( originalTCCL );
		provider.releaseIsolatedClassLoader( isolatedClassLoader );
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getTestInstance().get();
		if ( !( testInstance instanceof IsolatedClassLoaderProvider ) ) {
			throw new RuntimeException(
					"Test @ExtendWith( ClassLoadingIsolaterExtension.class ) have to implement ClassLoadingIsolaterExtension.IsolatedClassLoaderProvider" );
		}
		provider = (IsolatedClassLoaderProvider) testInstance;
		originalTCCL = Thread.currentThread().getContextClassLoader();
		isolatedClassLoader = provider.buildIsolatedClassLoader();

		log.infof( "Overriding TCCL [%s] -> [%s]", originalTCCL, isolatedClassLoader );

		Thread.currentThread().setContextClassLoader( isolatedClassLoader );

	}
}
