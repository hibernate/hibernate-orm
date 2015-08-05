/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * utility class to use in bytecode enhancement tests
 *
 * @author Steve Ebersole
 */
public abstract class EnhancerTestUtils extends BaseUnitTestCase {

	private static EnhancementContext enhancementContext = new EnhancerTestContext();

	private static String workingDir = System.getProperty( "java.io.tmpdir" );

	private static final CoreMessageLogger log = CoreLogging.messageLogger( EnhancerTestUtils.class );

	/**
	 * method that performs the enhancement of a class
	 * also checks the signature of enhanced entities methods using 'javap' decompiler
	 */
	public static Class<?> enhanceAndDecompile(Class<?> classToEnhance, ClassLoader cl) throws Exception {
		CtClass entityCtClass = generateCtClassForAnEntity( classToEnhance );

		byte[] original = entityCtClass.toBytecode();
		byte[] enhanced = new Enhancer( enhancementContext ).enhance( entityCtClass.getName(), original );
		assertFalse( "entity was not enhanced", Arrays.equals( original, enhanced ) );
		log.infof( "enhanced entity [%s]", entityCtClass.getName() );

		ClassPool cp = new ClassPool( false );
		cp.appendClassPath( new LoaderClassPath( cl ) );
		CtClass enhancedCtClass = cp.makeClass( new ByteArrayInputStream( enhanced ) );

		enhancedCtClass.debugWriteFile( workingDir );
		DecompileUtils.decompileDumpedClass( workingDir, classToEnhance.getName() );

		Class<?> enhancedClass = enhancedCtClass.toClass( cl, EnhancerTestUtils.class.getProtectionDomain() );
		assertNotNull( enhancedClass );
		return enhancedClass;
	}

	private static CtClass generateCtClassForAnEntity(Class<?> entityClassToEnhance) throws Exception {
		ClassPool cp = new ClassPool( false );
		ClassLoader cl = EnhancerTestUtils.class.getClassLoader();
		return cp.makeClass( cl.getResourceAsStream( entityClassToEnhance.getName().replace( '.', '/' ) + ".class" ) );
	}
	/* --- */

	@SuppressWarnings("unchecked")
	public static void runEnhancerTestTask(final Class<? extends EnhancerTestTask> task) {

		EnhancerTestTask taskObject = null;
		ClassLoader defaultCL = Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader cl = EnhancerTestUtils.getEnhancerClassLoader( task.getPackage().getName() );
			EnhancerTestUtils.setupClassLoader( cl, task );
			EnhancerTestUtils.setupClassLoader( cl, task.newInstance().getAnnotatedClasses() );

			Thread.currentThread().setContextClassLoader( cl );
			taskObject = ( (Class<? extends EnhancerTestTask>) cl.loadClass( task.getName() ) ).newInstance();

			taskObject.prepare();
			taskObject.execute();
		}
		catch (Exception e) {
			throw new HibernateException( "could not execute task", e );
		}
		finally {
			try {
				if ( taskObject != null ) {
					taskObject.complete();
				}
			}
			catch (Throwable ignore) {
			}
			Thread.currentThread().setContextClassLoader( defaultCL );
		}
	}

	private static void setupClassLoader(ClassLoader cl, Class<?>... classesToLoad) {
		for ( Class<?> classToLoad : classesToLoad ) {
			try {
				cl.loadClass( classToLoad.getName() );
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private static ClassLoader getEnhancerClassLoader(final String packageName) {
		return new ClassLoader() {
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				if ( !name.startsWith( packageName ) ) {
					return getParent().loadClass( name );
				}
				final Class c = findLoadedClass( name );
				if ( c != null ) {
					return c;
				}

				final InputStream is = this.getResourceAsStream(  name.replace( '.', '/' ) + ".class" );
				if ( is == null ) {
					throw new ClassNotFoundException( name + " not found" );
				}

				try {
					final byte[] original = new byte[is.available()];
					new BufferedInputStream( is ).read( original );

					final byte[] enhanced = new Enhancer( enhancementContext ).enhance( name, original );

					File f = new File( workingDir + File.separator + name.replace( ".", File.separator ) + ".class" );
					f.getParentFile().mkdirs();
					f.createNewFile();
					FileOutputStream out = new FileOutputStream( f );
					out.write( enhanced );
					out.close();

					return defineClass( name, enhanced, 0, enhanced.length );
				}
				catch (Throwable t) {
					throw new ClassNotFoundException( name + " not found", t );
				} finally {
					try {
						is.close();
					}
					catch (IOException e) { // ignore
					}
				}
			}
		};
	}

	public static Object getFieldByReflection(Object entity, String fieldName) {
		try {
			Field field =  entity.getClass().getDeclaredField( fieldName );
			field.setAccessible( true );
			return field.get( entity );
		}
		catch (NoSuchFieldException e) {
			fail( "Fail to get field '" + fieldName + "' in entity " + entity );
		}
		catch (IllegalAccessException e) {
			fail( "Fail to get field '" + fieldName + "' in entity " + entity );
		}
		return null;
	}

	/**
	 * clears the dirty set for an entity
	 */
	public static void clearDirtyTracking(Object entityInstance) {
		( (SelfDirtinessTracker) entityInstance ).$$_hibernate_clearDirtyAttributes();
	}

	/**
	 * compares the dirty fields of an entity with a set of expected values
	 */
	public static void checkDirtyTracking(Object entityInstance, String... dirtyFields) {
		final SelfDirtinessTracker selfDirtinessTracker = (SelfDirtinessTracker) entityInstance;
		assertEquals( dirtyFields.length > 0, selfDirtinessTracker.$$_hibernate_hasDirtyAttributes() );
		String[] tracked = selfDirtinessTracker.$$_hibernate_getDirtyAttributes();
		assertEquals( dirtyFields.length, tracked.length );
		assertTrue( Arrays.asList( tracked ).containsAll( Arrays.asList( dirtyFields ) ) );
	}

	public static EntityEntry makeEntityEntry() {
		return MutableEntityEntryFactory.INSTANCE.createEntityEntry(
				Status.MANAGED,
				null,
				null,
				1,
				null,
				LockMode.NONE,
				false,
				null,
				false,
				false,
				null
		);
	}

}
