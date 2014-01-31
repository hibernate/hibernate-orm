/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.instrument.runtime;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.buildtime.spi.BasicClassFilter;
import org.hibernate.bytecode.buildtime.spi.FieldFilter;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.InstrumentedClassLoader;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.SkipForDialect;
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
	@FailureExpectedWithNewMetamodel
	public void testDirtyCheck() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestDirtyCheckExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	@FailureExpectedWithNewMetamodel
	public void testFetchAll() throws Exception {
		executeExecutable( "org.hibernate.test.instrument.cases.TestFetchAllExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	@FailureExpectedWithNewMetamodel
	public void testLazy() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	@FailureExpectedWithNewMetamodel
	public void testLazyManyToOne() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyManyToOneExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	@FailureExpectedWithNewMetamodel
	public void testPropertyInitialized() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestIsPropertyInitializedExecutable" );
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testManyToOneProxy() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestManyToOneProxyExecutable" );
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testLazyPropertyCustomType() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyPropertyCustomTypeExecutable" );
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testSharedPKOneToOne() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestSharedPKOneToOneExecutable" );
	}

	@Test
    @SkipForDialect( value = { MySQLDialect.class, AbstractHANADialect.class }, comment = "wrong sql in mapping, mysql/hana need double type, but it is float type in mapping")
	@FailureExpectedWithNewMetamodel
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
