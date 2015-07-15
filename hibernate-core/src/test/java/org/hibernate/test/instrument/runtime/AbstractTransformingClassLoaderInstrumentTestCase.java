/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.runtime;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.buildtime.spi.BasicClassFilter;
import org.hibernate.bytecode.buildtime.spi.FieldFilter;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.InstrumentedClassLoader;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.junit4.ClassLoadingIsolater;
import org.junit.Rule;
import org.junit.Test;

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
									new BasicClassFilter( new String[] { "org.hibernate.test.instrument" }, null ),
									new FieldFilter() {
										public boolean shouldInstrumentField(String className, String fieldName) {
											return className.startsWith( "org.hibernate.test.instrument.domain" );
										}
										public boolean shouldTransformFieldAccess(String transformingClassName, String fieldOwnerClassName, String fieldName) {
											return fieldOwnerClassName.startsWith( "org.hibernate.test.instrument.domain" )
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

	@Test
	public void testSetFieldInterceptor() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestInjectFieldInterceptorExecutable" );
	}

	@Test
	public void testDirtyCheck() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestDirtyCheckExecutable" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9476" )
	public void testEagerFetchLazyToOne() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestFetchingLazyToOneExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	public void testFetchAll() throws Exception {
		executeExecutable( "org.hibernate.test.instrument.cases.TestFetchAllExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	public void testLazy() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	public void testLazyManyToOne() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyManyToOneExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	public void testPropertyInitialized() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestIsPropertyInitializedExecutable" );
	}

	@Test
	public void testManyToOneProxy() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestManyToOneProxyExecutable" );
	}

	@Test
	public void testLazyPropertyCustomType() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyPropertyCustomTypeExecutable" );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9984")
	@TestForIssue( jiraKey = "HHH-9984")
	public void testLazyBasicFieldAccess() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyBasicFieldAccessExecutable" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5255")
	public void testLazyBasicPropertyAccess() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyBasicPropertyAccessExecutable" );
	}

	@Test
	public void testSharedPKOneToOne() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestSharedPKOneToOneExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	public void testCustomColumnReadAndWrite() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestCustomColumnReadAndWrite" );
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
