package org.hibernate.test.instrument.runtime;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.bytecode.InstrumentedClassLoader;
import org.hibernate.bytecode.util.BasicClassFilter;
import org.hibernate.bytecode.util.FieldFilter;
import org.hibernate.junit.AbstractClassLoaderIsolatedTestCase;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTransformingClassLoaderInstrumentTestCase extends AbstractClassLoaderIsolatedTestCase {

	public AbstractTransformingClassLoaderInstrumentTestCase(String string) {
		super( string );
	}

	protected ClassLoader buildIsolatedClassLoader(ClassLoader parent) {
		BytecodeProvider provider = buildBytecodeProvider();
		return new InstrumentedClassLoader(
				parent,
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

	protected void releaseIsolatedClassLoader(ClassLoader isolatedLoader) {
	}

	protected abstract BytecodeProvider buildBytecodeProvider();


	// the tests ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void testSetFieldInterceptor() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestInjectFieldInterceptorExecutable" );
	}

	public void testDirtyCheck() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestDirtyCheckExecutable" );
	}

	public void testFetchAll() throws Exception {
		executeExecutable( "org.hibernate.test.instrument.cases.TestFetchAllExecutable" );
	}

	public void testLazy() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyExecutable" );
	}

	public void testLazyManyToOne() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyManyToOneExecutable" );
	}

	public void testPropertyInitialized() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestIsPropertyInitializedExecutable" );
	}

	public void testManyToOneProxy() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestManyToOneProxyExecutable" );
	}

	public void testLazyPropertyCustomType() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyPropertyCustomTypeExecutable" );
	}

	public void testSharedPKOneToOne() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestSharedPKOneToOneExecutable" );
	}

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
