/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.instrument.runtime;

import java.lang.reflect.InvocationTargetException;

import org.junit.Rule;
import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.buildtime.spi.BasicClassFilter;
import org.hibernate.bytecode.buildtime.spi.FieldFilter;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.InstrumentedClassLoader;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.junit4.ClassLoadingIsolater;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTransformingClassLoaderInstrumentTestCase extends BaseUnitTestCase {

	@Rule
	public ClassLoadingIsolater isolater = new ClassLoadingIsolater(
			new ClassLoadingIsolater.IsolatedClassLoaderProvider() {
				final BytecodeProvider provider = buildBytecodeProvider();

				@Override
				public ClassLoader buildIsolatedClassLoader() {
					return new InstrumentedClassLoader(
							Thread.currentThread().getContextClassLoader(),
							provider.getTransformer(
									new BasicClassFilter( new String[] { "org.hibernate.jpa.test.instrument" }, null ),
									new FieldFilter() {
										public boolean shouldInstrumentField(String className, String fieldName) {
											return className.startsWith( "org.hibernate.jpa.test.instrument.domain" );
										}
										public boolean shouldTransformFieldAccess(String transformingClassName, String fieldOwnerClassName, String fieldName) {
											return fieldOwnerClassName.startsWith( "org.hibernate.jpa.test.instrument.domain" )
													&& transformingClassName.equals( fieldOwnerClassName );
										}
									}
							)
					);
				}

				@Override
				public void releaseIsolatedClassLoader(ClassLoader isolatedClassLoader) {
					// nothing to do
				}
			}
	);

	protected abstract BytecodeProvider buildBytecodeProvider();


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
		executeExecutable( "org.hibernate.jpa.test.instrument.cases.TestLazyPropertyOnPreUpdateExecutable" );
	}

	// reflection code to ensure isolation into the created classloader ~~~~~~~

	private static final Class[] SIG = new Class[] {};
	private static final Object[] ARGS = new Object[] {};

	public void executeExecutable(String name) {
		Class execClass = null;
		Object executable = null;
		try {
			execClass = Thread.currentThread().getContextClassLoader().loadClass( name );
			executable = execClass.newInstance();
		}
		catch( Throwable t ) {
			throw new HibernateException( "could not load executable", t );
		}
		try {
			execClass.getMethod( "prepare", SIG ).invoke( executable, ARGS );
			execClass.getMethod( "execute", SIG ).invoke( executable, ARGS );
		}
		catch ( NoSuchMethodException e ) {
			throw new HibernateException( "could not exeucte executable", e );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "could not exeucte executable", e );
		}
		catch ( InvocationTargetException e ) {
			throw new HibernateException( "could not exeucte executable", e.getTargetException() );
		}
		finally {
			try {
				execClass.getMethod( "complete", SIG ).invoke( executable, ARGS );
			}
			catch ( Throwable ignore ) {
			}
		}
	}
}
