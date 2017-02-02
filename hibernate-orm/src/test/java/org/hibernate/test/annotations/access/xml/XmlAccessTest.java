/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.access.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.AccessType;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.tuple.entity.EntityTuplizer;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Assert;
import org.junit.Test;


/**
 * Test verifying that it is possible to configure the access type via xml configuration.
 *
 * @author Hardy Ferentschik
 */
public class XmlAccessTest extends BaseUnitTestCase {
	@Test
	public void testAccessOnBasicXmlElement() throws Exception {
		Class<?> classUnderTest = Tourist.class;
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add( classUnderTest );
		List<String> configFiles = Collections.emptyList();
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );

		// without any xml configuration we have field access
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
		// now with an additional xml configuration file changing the default access type for Tourist using basic
		configFiles = new ArrayList<String>();
		configFiles.add( "org/hibernate/test/annotations/access/xml/Tourist.xml" );
		factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnPersistenceUnitDefaultsXmlElement() throws Exception {
		Class<?> classUnderTest = Tourist.class;
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add( classUnderTest );
		List<String> configFiles = Collections.emptyList();
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );

		// without any xml configuration we have field access
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
		// now with an additional xml configuration file changing the default access type for Tourist using persitence unit defaults
		configFiles = new ArrayList<String>();
		configFiles.add( "org/hibernate/test/annotations/access/xml/Tourist2.xml" );
		factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnEntityMappingsXmlElement() throws Exception {
		Class<?> classUnderTest = Tourist.class;
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add( classUnderTest );
		List<String> configFiles = Collections.emptyList();
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );

		// without any xml configuration we have field access
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
		// now with an additional xml configuration file changing the default access type for Tourist using default in entity-mappings
		configFiles = new ArrayList<String>();
		configFiles.add( "org/hibernate/test/annotations/access/xml/Tourist3.xml" );
		factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnEntityXmlElement() throws Exception {
		Class<?> classUnderTest = Tourist.class;
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add( classUnderTest );
		List<String> configFiles = Collections.emptyList();
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );

		// without any xml configuration we have field access
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
		// now with an additional xml configuration file changing the default access type for Tourist using entity level config
		configFiles = new ArrayList<String>();
		configFiles.add( "org/hibernate/test/annotations/access/xml/Tourist4.xml" );
		factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnMappedSuperClassXmlElement() throws Exception {
		Class<?> classUnderTest = Waiter.class;
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add( classUnderTest );
		classes.add( Crew.class );
		List<String> configFiles = new ArrayList<String>();
		configFiles.add( "org/hibernate/test/annotations/access/xml/Crew.xml" );
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
	}

	@Test
	public void testAccessOnAssociationXmlElement() throws Exception {
		Class<?> classUnderTest = RentalCar.class;
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add( classUnderTest );
		classes.add( Driver.class );
		List<String> configFiles = new ArrayList<String>();
		configFiles.add( "org/hibernate/test/annotations/access/xml/RentalCar.xml" );
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnEmbeddedXmlElement() throws Exception {
		Class<?> classUnderTest = Cook.class;
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add( classUnderTest );
		classes.add( Knive.class );
		List<String> configFiles = new ArrayList<String>();
		configFiles.add( "org/hibernate/test/annotations/access/xml/Cook.xml" );
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnElementCollectionXmlElement() throws Exception {
		Class<?> classUnderTest = Boy.class;
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add( classUnderTest );
		List<String> configFiles = new ArrayList<String>();
		configFiles.add( "org/hibernate/test/annotations/access/xml/Boy.xml" );
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	private SessionFactoryImplementor buildSessionFactory(List<Class<?>> classesUnderTest, List<String> configFiles) {
		assert classesUnderTest != null;
		assert configFiles != null;
		Configuration cfg = new Configuration();
		for ( Class<?> clazz : classesUnderTest ) {
			cfg.addAnnotatedClass( clazz );
		}
		for ( String configFile : configFiles ) {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( configFile );
			cfg.addInputStream( is );
		}
		return ( SessionFactoryImplementor ) cfg.buildSessionFactory();
	}

	// uses the first getter of the tupelizer for the assertions

	private void assertAccessType(SessionFactoryImplementor factory, Class<?> classUnderTest, AccessType accessType) {
		EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
				.getEntityMetamodel()
				.getTuplizer();
		if ( AccessType.FIELD.equals( accessType ) ) {
			Assert.assertTrue(
					"Field access was expected.",
					tuplizer.getGetter( 0 ) instanceof GetterFieldImpl
			);
		}
		else {
			Assert.assertTrue(
					"Property access was expected.",
					tuplizer.getGetter( 0 ) instanceof GetterMethodImpl
			);
		}
	}
}
