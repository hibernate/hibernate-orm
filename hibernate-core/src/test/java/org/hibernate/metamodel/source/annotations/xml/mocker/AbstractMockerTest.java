package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.testing.ServiceRegistryBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Strong Liu
 */
public abstract class AbstractMockerTest {
	private static final String ORM1_MAPPING_XSD = "org/hibernate/ejb/orm_1_0.xsd";
	private static final String ORM2_MAPPING_XSD = "org/hibernate/ejb/orm_2_0.xsd";
	private IndexBuilder indexBuilder;
	private Index index;
	private ServiceRegistry serviceRegistry;
	protected String packagePrefix = "org/hibernate/metamodel/source/annotations/xml/mocker/";

	protected IndexBuilder getIndexBuilder() {
		if ( indexBuilder == null ) {
			indexBuilder = new IndexBuilder( getIndex(), getServiceRegistry() );
		}
		return indexBuilder;

	}

	protected EntityMappingsMocker getEntityMappingsMocker(String... mappingFiles) {
		ClassLoaderService classLoaderService = getServiceRegistry().getService( ClassLoaderService.class );
		List<XMLEntityMappings> xmlEntityMappingsList = new ArrayList<XMLEntityMappings>();
		for ( String fileName : mappingFiles ) {
			XMLEntityMappings entityMappings;
			try {
				entityMappings = XmlHelper.unmarshallXml(
						packagePrefix + fileName, ORM2_MAPPING_XSD, XMLEntityMappings.class, classLoaderService
				).getRoot();
			}
			catch ( JAXBException orm2Exception ) {
				// if we cannot parse against orm_2_0.xsd we try orm_1_0.xsd for backwards compatibility
				try {
					entityMappings = XmlHelper.unmarshallXml(
							packagePrefix + fileName, ORM1_MAPPING_XSD, XMLEntityMappings.class, classLoaderService
					).getRoot();
				}
				catch ( JAXBException orm1Exception ) {
					throw new AnnotationException( "Unable to parse xml configuration.", orm1Exception );
				}
			}
			xmlEntityMappingsList.add( entityMappings );
		}
		return new EntityMappingsMocker( xmlEntityMappingsList, getIndex(), getServiceRegistry() );
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
	protected Index getMockedIndex(String ormFileName){
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

	protected void assertHasAnnotation(Index index, DotName className, DotName annName) {
		assertHasAnnotation( index, className, annName, 1 );
	}

	protected void assertHasNoAnnotation(Index index, DotName className, DotName annName) {
		ClassInfo classInfo = index.getClassByName( className );
		if ( classInfo == null ) {
			fail( "Can't find " + className + " from Index" );
		}
		if ( classInfo.annotations() != null ) {
			List<AnnotationInstance> annotationInstanceList = classInfo.annotations().get( annName );
			if ( annotationInstanceList != null ) {
				if(!annotationInstanceList.isEmpty()){
					fail( className+" has Annotation "+annName );
				}
			}
		}
	}

	protected void assertHasAnnotation(Index index, DotName className, DotName annName, int size) {
		ClassInfo classInfo = index.getClassByName( className );
		if ( classInfo == null ) {
			fail( "Can't find " + className + " from Index" );
		}
		if ( classInfo.annotations() == null ) {
			fail( classInfo + " doesn't have any annotations defined" );
		}
		List<AnnotationInstance> annotationInstanceList = classInfo.annotations().get( annName );
		if ( annotationInstanceList == null || annotationInstanceList.isEmpty() ) {
			fail( classInfo + " doesn't have annotation " + annName );
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
		ClassInfo classInfo = index.getClassByName( className );
		List<AnnotationInstance> annotationInstanceList = classInfo.annotations().get( annName );
		for ( AnnotationInstance annotationInstance : annotationInstanceList ) {
			checker.check( annotationInstance );
		}
	}

	static interface AnnotationValueChecker {
		void check(AnnotationInstance annotationInstance);
	}
}
