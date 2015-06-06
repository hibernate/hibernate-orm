/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.javap.JavapTask;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * utility class to use in bytecode enhancement tests
 *
 * @author Luis Barreiro
 */
public abstract class DecompileUtils {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( DecompileUtils.class );

	public static void decompileDumpedClass(String workingDir, String className) {
		try {
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
			fileManager.setLocation(
					StandardLocation.CLASS_OUTPUT,
					Collections.singletonList( new File( workingDir ) )
			);

			JavapTask javapTask = new JavapTask();
			String filename = workingDir + File.separator + getFilenameForClassName( className );
			for ( JavaFileObject jfo : fileManager.getJavaFileObjects( filename ) ) {
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

	private static String getFilenameForClassName(String className) {
		return className.replace( '.', File.separatorChar ) + JavaFileObject.Kind.CLASS.extension;
	}

}
