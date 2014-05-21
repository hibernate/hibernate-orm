/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.internal.ClassLoaderAccessImpl;
import org.hibernate.metamodel.source.internal.jandex.EntityMappingsMocker;
import org.hibernate.metamodel.source.internal.jandex.IndexBuilder;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.xml.internal.jaxb.UnifiedMappingBinder;
import org.hibernate.xml.spi.BindResult;
import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.SourceType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

/**
 * @author Strong Liu
 */
public abstract class AbstractMockerTest {
	private static final String ORM1_MAPPING_XSD = "org/hibernate/jpa/orm_1_0.xsd";
	private static final String ORM2_MAPPING_XSD = "org/hibernate/jpa/orm_2_0.xsd";
	private static final String ORM2_1_MAPPING_XSD = "org/hibernate/jpa/orm_2_1.xsd";
	private IndexBuilder indexBuilder;
	private Index index;
	private ServiceRegistry serviceRegistry;
	protected String packagePrefix = getClass().getPackage().getName().replace( '.', '/' ) + '/';

	protected IndexBuilder getIndexBuilder() {
		if ( indexBuilder == null ) {
			indexBuilder = new IndexBuilder( getIndex(), getServiceRegistry() );
		}
		return indexBuilder;

	}

	protected EntityMappingsMocker getEntityMappingsMocker(String... mappingFiles) {
		UnifiedMappingBinder processor = new UnifiedMappingBinder(
				new ClassLoaderAccessImpl(null, getServiceRegistry() ) );
		ClassLoaderService classLoaderService = getServiceRegistry().getService( ClassLoaderService.class );
		List<BindResult<JaxbEntityMappings>> xmlBindings = new ArrayList<BindResult<JaxbEntityMappings>>();
		for ( String fileName : mappingFiles ) {
			BindResult bindResult = new BindResult(
					classLoaderService.locateResourceStream( packagePrefix + fileName ),
					new Origin( SourceType.FILE, packagePrefix + fileName ),
					true
			);
			bindResult.bind( processor );
			xmlBindings.add( bindResult );
		}
		return new EntityMappingsMocker( xmlBindings, getIndex(), getServiceRegistry() );
	}

	protected Index getIndex() {
		if ( index == null ) {
			Indexer indexer = new Indexer();
			for ( Class<?> clazz : getAnnotatedClasses() ) {
				indexClass( indexer, clazz.getName().replace( '.', '/' ) + ".class" );
			}

			// add package-info from the configured packages
			for ( String packageName : getAnnotatedPackages() ) {
				indexClass( indexer, packageName.replace( '.', '/' ) + "/package-info.class" );
			}
			index = indexer.complete();
		}
		return index;

	}

	protected Index getMockedIndex(String ormFileName) {
		EntityMappingsMocker mocker = getEntityMappingsMocker( ormFileName );
		return mocker.mockNewIndex();
	}

	private void indexClass(Indexer indexer, String className) {
		ClassLoaderService classLoaderService = getServiceRegistry().getService( ClassLoaderService.class );
		InputStream stream = classLoaderService.locateResourceStream( className );
		try {
			indexer.index( stream );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to open input stream for class " + className, e );
		}
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[0];
	}

	protected String[] getAnnotatedPackages() {
		return new String[0];
	}

	protected ServiceRegistry getServiceRegistry() {
		if ( serviceRegistry == null ) {
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry();
		}
		return serviceRegistry;
	}

	protected void assertHasNoAnnotation(Index index, DotName className, DotName annName) {
		List<AnnotationInstance> annotationInstanceList = getAnnotationInstances( index, className, annName );
		if ( annotationInstanceList != null ) {
			if ( !annotationInstanceList.isEmpty() ) {
				fail( className + " has Annotation " + annName );
			}
		}
	}
	protected void assertHasAnnotation(Index index,  DotName annName) {
		assertHasAnnotation( index, null, annName, 1 );
	}
	protected void assertHasAnnotation(Index index, DotName className, DotName annName) {
		assertHasAnnotation( index, className, annName, 1 );
	}

	protected void assertHasAnnotation(Index index, DotName className, DotName annName, int size) {
		List<AnnotationInstance> annotationInstanceList = getAnnotationInstances( index, className, annName );
		if ( annotationInstanceList == null || annotationInstanceList.isEmpty() ) {
			fail( "Expected annotation " + annName + " size is " + size + ", but no one can be found in Index" );
		}
		assertEquals(
				"Expected annotation " + annName + " size is " + size + ", but it actually is " + annotationInstanceList
						.size(), size, annotationInstanceList.size()
		);
	}

	protected void assertStringAnnotationValue(String expected, AnnotationValue annotationValue) {
		if ( annotationValue == null ) {
			fail( "Annotation Value is null." );
		}
		assertEquals( expected, annotationValue.asString() );
	}

	protected void assertAnnotationValue(Index index, DotName className, DotName annName, AnnotationValueChecker checker) {
		assertAnnotationValue( index, className, annName, 1, checker );
	}

	protected void assertAnnotationValue(Index index, DotName className, DotName annName, int size, AnnotationValueChecker checker) {
		assertHasAnnotation( index, className, annName, size );
		List<AnnotationInstance> annotationInstanceList = getAnnotationInstances( index,className,annName );
		for ( AnnotationInstance annotationInstance : annotationInstanceList ) {
			checker.check( annotationInstance );
		}
	}

	private List<AnnotationInstance> getAnnotationInstances(Index index, DotName className, DotName annName) {
		if ( className != null ) {
			ClassInfo classInfo = index.getClassByName( className );
			if ( classInfo == null ) {
				fail( "Can't find " + className + " from Index" );
			}
			if ( classInfo.annotations() == null ) {
				fail( classInfo + " doesn't have any annotations defined" );
			}
			return classInfo.annotations().get( annName );
		}
		else {
			return index.getAnnotations( annName );
		}
	}

	static interface AnnotationValueChecker {
		void check(AnnotationInstance annotationInstance);
	}
}
