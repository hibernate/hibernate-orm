/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.archive.scan.spi.ClassDescriptor;
import org.hibernate.metamodel.archive.scan.spi.JandexInitializer;
import org.hibernate.metamodel.archive.scan.spi.PackageDescriptor;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.ClassLoaderAccess;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

/**
 * Manages the steps needed to get Jandex properly initialized for later use.
 *
 * @author Steve Ebersole
 */
public class JandexInitManager implements JandexInitializer {
	private static final Logger log = Logger.getLogger( JandexInitManager.class );

	private final DotName OBJECT_DOT_NAME = DotName.createSimple( Object.class.getName() );

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// supplied index
	private final IndexView suppliedIndexView;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// indexer
	private final Indexer indexer;
	private final Map<DotName, ClassInfo> inflightClassInfoMap;
	private final ClassLoaderAccess classLoaderAccess;
	private final boolean autoIndexMembers;

	public JandexInitManager(
			IndexView suppliedIndexView,
			ClassLoaderAccess classLoaderAccess,
			boolean autoIndexMembers) {

		this.suppliedIndexView = suppliedIndexView;
		this.classLoaderAccess = classLoaderAccess;
		this.autoIndexMembers = autoIndexMembers;

		if ( suppliedIndexView == null ) {
			this.indexer = new Indexer();
			this.inflightClassInfoMap = new HashMap<DotName, ClassInfo>();
		}
		else {
			this.indexer = null;
			this.inflightClassInfoMap = null;
		}
	}

	/**
	 * INTENDED FOR TESTING ONLY
	 */
	public JandexInitManager() {
		this( null, TestingOnlyClassLoaderAccess.INSTANCE, false );
	}

	private static class TestingOnlyClassLoaderAccess implements ClassLoaderAccess {
		public static final TestingOnlyClassLoaderAccess INSTANCE = new TestingOnlyClassLoaderAccess();

		@Override
		@SuppressWarnings("unchecked")
		public <T> Class<T> classForName(String name) {
			try {
				return (Class<T>) getClass().getClassLoader().loadClass( name );
			}
			catch ( Exception e ) {
				throw new ClassLoadingException( "Could not load class by name : " + name );
			}
		}

		@Override
		public URL locateResource(String resourceName) {
			return getClass().getClassLoader().getResource( resourceName );
		}
	}

	public boolean wasIndexSupplied() {
		return suppliedIndexView != null;
	}

	@Override
	public ClassInfo handle(ClassDescriptor classFileDescriptor) {
		if ( suppliedIndexView != null ) {
			return suppliedIndexView.getClassByName( DotName.createSimple( classFileDescriptor.getName() ) );
		}
		else {
			return index( classFileDescriptor );
		}
	}

	private ClassInfo index(ClassDescriptor classDescriptor) {
		InputStream stream = classDescriptor.getStreamAccess().accessInputStream();
		try {
			return index( stream, classDescriptor.getName() );
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException ignore) {
			}
		}
	}

	protected ClassInfo index(InputStream stream, String name) {
		try {
			final ClassInfo classInfo = indexer.index( stream );
			inflightClassInfoMap.put( classInfo.name(), classInfo );
			furtherProcess( classInfo );
			return classInfo;
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to index from resource stream [" + name + "]", e );
		}
	}

	private void furtherProcess(ClassInfo classInfo) {
		final List<AnnotationInstance> entityListenerAnnotations = classInfo.annotations().get( JPADotNames.ENTITY_LISTENERS );
		if ( entityListenerAnnotations != null ) {
			for ( AnnotationInstance entityListenerAnnotation : entityListenerAnnotations ) {
				final Type[] entityListenerClassTypes = entityListenerAnnotation.value().asClassArray();
				for ( Type entityListenerClassType : entityListenerClassTypes ) {
					indexClassName( entityListenerClassType.name() );
				}
			}
		}

		// todo : others?
	}


	public void indexClassName(DotName classDotName) {
		if ( !needsIndexing( classDotName ) ) {
			return;
		}

		ClassInfo classInfo = indexResource( classDotName.toString().replace( '.', '/' ) + ".class" );
		inflightClassInfoMap.put( classInfo.name(), classInfo );
		if ( classInfo.superName() != null ) {
			indexClassName( classInfo.superName() );
		}
	}

	protected boolean needsIndexing(DotName classDotName) {
		if ( classDotName == null || OBJECT_DOT_NAME.equals( classDotName ) ) {
			return false;
		}

		if ( suppliedIndexView != null ) {
			// we do not build an index if one is supplied to us.
			return false;
		}

		// otherwise, return true if the map does not contain this class
		return !inflightClassInfoMap.containsKey( classDotName );
	}


	public ClassInfo indexResource(String resourceName) {
		final URL resourceUrl = classLoaderAccess.locateResource( resourceName );

		if ( resourceUrl == null ) {
			throw new IllegalArgumentException( "Could not locate resource [" + resourceName + "]" );
		}

		try {
			final InputStream stream = resourceUrl.openStream();
			try {
				return index( stream, resourceName );
			}
			finally {
				try {
					stream.close();
				}
				catch ( IOException e ) {
					log.debug( "Unable to close resource stream [" + resourceName + "] : " + e.getMessage() );
				}
			}
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to open input stream for resource [" + resourceName + "]", e );
		}
	}

	@Override
	public ClassInfo handle(PackageDescriptor packageInfoFileDescriptor) {
		if ( suppliedIndexView != null ) {
			return suppliedIndexView.getClassByName( DotName.createSimple( packageInfoFileDescriptor.getName() ) );
		}
		else {
			return index( packageInfoFileDescriptor );
		}
	}

	private ClassInfo index(PackageDescriptor packageDescriptor) {
		InputStream stream = packageDescriptor.getStreamAccess().accessInputStream();
		try {
			return index( stream, packageDescriptor.getName() );
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException ignore) {
			}
		}
	}

	public void indexLoadedClass(Class loadedClass) {
		assert !wasIndexSupplied() : "We are not indexing";

		if ( loadedClass == null ) {
			return;
		}

		final DotName classDotName = DotName.createSimple( loadedClass.getName() );
		if ( !needsIndexing( classDotName ) ) {
			return;
		}

		// index super type first
		indexLoadedClass( loadedClass.getSuperclass() );

		// index any inner classes
		for ( Class innerClass : loadedClass.getDeclaredClasses() ) {
			indexLoadedClass( innerClass );
		}

		// then index the class itself
		ClassInfo classInfo = indexResource( loadedClass.getName().replace( '.', '/' ) + ".class" );
		inflightClassInfoMap.put( classDotName, classInfo );

		if ( !autoIndexMembers ) {
			return;
		}

		for ( Class<?> fieldType : ReflectHelper.getMemberTypes( loadedClass ) ) {
			if ( !fieldType.isPrimitive() && fieldType != Object.class ) {
				indexLoadedClass( fieldType );
			}
		}

		// Also check for classes within a @Target annotation.
		// 		[steve] - not so sure about this.  target would name an entity, which should be
		// 			known to us somehow
		for ( AnnotationInstance targetAnnotation : JandexHelper.getAnnotations( classInfo, HibernateDotNames.TARGET ) ) {
			String targetClassName = targetAnnotation.value().asClass().name().toString();
			Class<?> targetClass = classLoaderAccess.classForName( targetClassName );
			indexLoadedClass( targetClass );
		}
	}

	public IndexView buildIndex() {
		if ( suppliedIndexView != null ) {
			return suppliedIndexView;
		}
		else {
			final Index jandexIndex = indexer.complete();
			if ( log.isTraceEnabled() ) {
				jandexIndex.printSubclasses();
				jandexIndex.printAnnotations();
			}
			return jandexIndex;
		}
	}
}
