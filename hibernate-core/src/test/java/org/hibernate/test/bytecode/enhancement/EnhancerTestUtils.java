/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.javap.JavapTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * utility class to use in bytecode enhancement tests
 *
 * @author Steve Ebersole
 */
public abstract class EnhancerTestUtils extends BaseUnitTestCase {

	private static EnhancementContext enhancementContext = new DefaultEnhancementContext();

	private static String workingDir = System.getProperty( "java.io.tmpdir" );

	private static final CoreMessageLogger log = CoreLogging.messageLogger( EnhancerTestUtils.class );

	/**
	 * method that performs the enhancement of a class
	 * also checks the signature of enhanced entities methods using 'javap' decompiler
	 */
	static Class<?> enhanceAndDecompile(Class<?> classToEnhance, ClassLoader cl) throws Exception {
		CtClass entityCtClass = generateCtClassForAnEntity( classToEnhance );

		byte[] original = entityCtClass.toBytecode();
		byte[] enhanced = new Enhancer( enhancementContext ).enhance( entityCtClass.getName(), original );
		assertFalse( "entity was not enhanced", Arrays.equals( original, enhanced ) );
		log.infof( "enhanced entity [%s]", entityCtClass.getName() );

		ClassPool cp = new ClassPool( false );
		cp.appendClassPath( new LoaderClassPath( cl ) );
		CtClass enhancedCtClass = cp.makeClass( new ByteArrayInputStream( enhanced ) );

		enhancedCtClass.debugWriteFile( workingDir );
		decompileDumpedClass( classToEnhance.getName() );

		Class<?> enhancedClass = enhancedCtClass.toClass( cl, EnhancerTestUtils.class.getProtectionDomain() );
		assertNotNull( enhancedClass );
		return enhancedClass;
	}

	private static void decompileDumpedClass(String className) {
		try {
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
			fileManager.setLocation(
					StandardLocation.CLASS_OUTPUT,
					Collections.singletonList( new File( workingDir ) )
			);

			JavapTask javapTask = new JavapTask();
			for ( JavaFileObject jfo : fileManager.getJavaFileObjects(
					workingDir + File.separator + getFilenameForClassName(
							className
					)
			) ) {
				try {
					Set<String> interfaceNames = new HashSet<String>();
					Set<String> fieldNames = new HashSet<String>();
					Set<String> methodNames = new HashSet<String>();

					JavapTask.ClassFileInfo info = javapTask.read( jfo );

					log.infof( "decompiled class [%s]", info.cf.getName() );

					for ( int i : info.cf.interfaces ) {
						interfaceNames.add( info.cf.constant_pool.getClassInfo( i ).getName() );
						log.debugf( "declared iFace  = ", info.cf.constant_pool.getClassInfo( i ).getName() );
					}
					for ( com.sun.tools.classfile.Field f : info.cf.fields ) {
						fieldNames.add( f.getName( info.cf.constant_pool ) );
						log.debugf( "declared field  = ", f.getName( info.cf.constant_pool ) );
					}
					for ( com.sun.tools.classfile.Method m : info.cf.methods ) {
						methodNames.add( m.getName( info.cf.constant_pool ) );
						log.debugf( "declared method = ", m.getName( info.cf.constant_pool ) );
					}

					// checks signature against known interfaces
					if ( interfaceNames.contains( PersistentAttributeInterceptor.class.getName() ) ) {
						assertTrue( fieldNames.contains( EnhancerConstants.INTERCEPTOR_FIELD_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.INTERCEPTOR_GETTER_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.INTERCEPTOR_SETTER_NAME ) );
					}
					if ( interfaceNames.contains( ManagedEntity.class.getName() ) ) {
						assertTrue( methodNames.contains( EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME ) );

						assertTrue( fieldNames.contains( EnhancerConstants.ENTITY_ENTRY_FIELD_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.ENTITY_ENTRY_GETTER_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.ENTITY_ENTRY_SETTER_NAME ) );

						assertTrue( fieldNames.contains( EnhancerConstants.PREVIOUS_FIELD_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.PREVIOUS_GETTER_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.PREVIOUS_SETTER_NAME ) );

						assertTrue( fieldNames.contains( EnhancerConstants.NEXT_FIELD_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.NEXT_GETTER_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.NEXT_SETTER_NAME ) );
					}
					if ( interfaceNames.contains( SelfDirtinessTracker.class.getName() ) ) {
						assertTrue( fieldNames.contains( EnhancerConstants.TRACKER_FIELD_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.TRACKER_GET_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.TRACKER_CLEAR_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.TRACKER_HAS_CHANGED_NAME ) );
					}
					if ( interfaceNames.contains( CompositeTracker.class.getName() ) ) {
						assertTrue( fieldNames.contains( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER ) );
						assertTrue( methodNames.contains( EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER ) );
					}
					if ( interfaceNames.contains( CompositeOwner.class.getName() ) ) {
						assertTrue( fieldNames.contains( EnhancerConstants.TRACKER_CHANGER_NAME ) );
						assertTrue( methodNames.contains( EnhancerConstants.TRACKER_CHANGER_NAME ) );
					}
				}
				catch (ConstantPoolException e) {
					e.printStackTrace();
				}
			}
		}
		catch (IOException ioe) {
			assertNull( "Failed to open class file", ioe );
		}
		catch (RuntimeException re) {
			log.warnf( re, "WARNING: UNABLE DECOMPILE DUE TO %s", re.getMessage() );
		}
	}

	private static CtClass generateCtClassForAnEntity(Class<?> entityClassToEnhance) throws Exception {
		ClassPool cp = new ClassPool( false );
		return cp.makeClass(
				EnhancerTestUtils.class.getClassLoader().getResourceAsStream(
						getFilenameForClassName(
								entityClassToEnhance.getName()
						)
				)
		);
	}

	private static String getFilenameForClassName(String className) {
		return className.replace( '.', File.separatorChar ) + JavaFileObject.Kind.CLASS.extension;
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

	static EntityEntry makeEntityEntry() {
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

	public static class LocalPersistentAttributeInterceptor implements PersistentAttributeInterceptor {

		@Override
		public boolean readBoolean(Object obj, String name, boolean oldValue) {
			log.infof( "Reading boolean [%s]", name );
			return oldValue;
		}

		@Override
		public boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue) {
			log.infof( "Writing boolean []", name );
			return newValue;
		}

		@Override
		public byte readByte(Object obj, String name, byte oldValue) {
			log.infof( "Reading byte [%s]", name );
			return oldValue;
		}

		@Override
		public byte writeByte(Object obj, String name, byte oldValue, byte newValue) {
			log.infof( "Writing byte [%s]", name );
			return newValue;
		}

		@Override
		public char readChar(Object obj, String name, char oldValue) {
			log.infof( "Reading char [%s]", name );
			return oldValue;
		}

		@Override
		public char writeChar(Object obj, String name, char oldValue, char newValue) {
			log.infof( "Writing char [%s]", name );
			return newValue;
		}

		@Override
		public short readShort(Object obj, String name, short oldValue) {
			log.infof( "Reading short [%s]", name );
			return oldValue;
		}

		@Override
		public short writeShort(Object obj, String name, short oldValue, short newValue) {
			log.infof( "Writing short [%s]", name );
			return newValue;
		}

		@Override
		public int readInt(Object obj, String name, int oldValue) {
			log.infof( "Reading int [%s]", name );
			return oldValue;
		}

		@Override
		public int writeInt(Object obj, String name, int oldValue, int newValue) {
			log.infof( "Writing int [%s]", name );
			return newValue;
		}

		@Override
		public float readFloat(Object obj, String name, float oldValue) {
			log.infof( "Reading float [%s]", name );
			return oldValue;
		}

		@Override
		public float writeFloat(Object obj, String name, float oldValue, float newValue) {
			log.infof( "Writing float [%s]", name );
			return newValue;
		}

		@Override
		public double readDouble(Object obj, String name, double oldValue) {
			log.infof( "Reading double [%s]", name );
			return oldValue;
		}

		@Override
		public double writeDouble(Object obj, String name, double oldValue, double newValue) {
			log.infof( "Writing double [%s]", name );
			return newValue;
		}

		@Override
		public long readLong(Object obj, String name, long oldValue) {
			log.infof( "Reading long [%s]", name );
			return oldValue;
		}

		@Override
		public long writeLong(Object obj, String name, long oldValue, long newValue) {
			log.infof( "Writing long [%s]", name );
			return newValue;
		}

		@Override
		public Object readObject(Object obj, String name, Object oldValue) {
			log.infof( "Reading Object [%s]", name );
			return oldValue;
		}

		@Override
		public Object writeObject(Object obj, String name, Object oldValue, Object newValue) {
			log.infof( "Writing Object [%s]", name );
			return newValue;
		}
	}

}
