/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.enhancement.runtime;

import org.hibernate.jpa.test.enhancement.cases.TestLazyPropertyOnPreUpdateExecutable;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class JpaRuntimeEnhancementTest extends BaseUnitTestCase {
//
//	@Rule
//	public ClassLoadingIsolater isolater = new ClassLoadingIsolater(
//			new ClassLoadingIsolater.IsolatedClassLoaderProvider() {
//				@Override
//				public ClassLoader buildIsolatedClassLoader() {
//					final EnhancementContext enhancementContext = new DefaultEnhancementContext() {
//						@Override
//						public boolean doExtendedEnhancement(CtClass classDescriptor) {
//							return classDescriptor.getPackageName().startsWith( "org.hibernate.jpa.test.enhancement.domain" );
//						}
//					};
//
//					final Enhancer enhancer = new Enhancer( enhancementContext );
//
//					return new InstrumentedClassLoader(
//							Thread.currentThread().getContextClassLoader(),
//							new ClassTransformer() {
//								@Override
//								public byte[] transform(
//										ClassLoader loader,
//										String className,
//										Class<?> classBeingRedefined,
//										ProtectionDomain protectionDomain,
//										byte[] classfileBuffer) throws IllegalClassFormatException {
//
//									try {
//										return enhancer.enhance( className, classfileBuffer );
//									}
//									catch (final Exception e) {
//										throw new IllegalClassFormatException( "Error performing enhancement" ) {
//											@Override
//											public synchronized Throwable getCause() {
//												return e;
//											}
//										};
//									}
//								}
//							}
//					);
//				}
//
//				@Override
//				public void releaseIsolatedClassLoader(ClassLoader isolatedClassLoader) {
//					// nothing to do
//				}
//			}
//	);


	// the tests ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Test for HHH-7573.
	 * Load some test data into an entity which has a lazy property and a @PreUpdate callback, then reload and update a
	 * non lazy field which will trigger the PreUpdate lifecycle callback.
	 * @throws Exception
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-7573" )
	public void LazyPropertyOnPreUpdate() throws Exception {
		EnhancerTestUtils.runEnhancerTestTask( TestLazyPropertyOnPreUpdateExecutable.class );
	}

//	// reflection code to ensure isolation into the created classloader ~~~~~~~
//
//	private static final Class[] SIG = new Class[] {};
//	private static final Object[] ARGS = new Object[] {};
//
//	public void executeExecutable(String name) {
//		Class execClass = null;
//		Object executable = null;
//		try {
//			execClass = Thread.currentThread().getContextClassLoader().loadClass( name );
//			executable = execClass.newInstance();
//		}
//		catch( Throwable t ) {
//			throw new HibernateException( "could not load executable", t );
//		}
//		try {
//			execClass.getMethod( "prepare", SIG ).invoke( executable, ARGS );
//			execClass.getMethod( "execute", SIG ).invoke( executable, ARGS );
//		}
//		catch ( NoSuchMethodException e ) {
//			throw new HibernateException( "could not exeucte executable", e );
//		}
//		catch ( IllegalAccessException e ) {
//			throw new HibernateException( "could not exeucte executable", e );
//		}
//		catch ( InvocationTargetException e ) {
//			throw new HibernateException( "could not exeucte executable", e.getTargetException() );
//		}
//		finally {
//			try {
//				execClass.getMethod( "complete", SIG ).invoke( executable, ARGS );
//			}
//			catch ( Throwable ignore ) {
//			}
//		}
//	}
}
