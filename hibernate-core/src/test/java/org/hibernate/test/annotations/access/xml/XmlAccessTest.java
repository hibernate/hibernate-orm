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
package org.hibernate.test.annotations.access.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.AccessType;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.tuple.entity.EntityTuplizer;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
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
	@FailureExpectedWithNewMetamodel(
			message = "The problem here is that XML is attempting to apply PU-wide default AccessType, " +
					"but mocker only understands (and overrides) attributes that are explicitly listed in the " +
					"XML.  Likely we need to link the XML defaults with the local-binding-context defaults as we" +
					"start to process Jandex"
	)
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

		MetadataSources metadataSources = new MetadataSources();
		for ( Class<?> clazz : classesUnderTest ) {
			metadataSources.addAnnotatedClass( clazz );
		}
		for ( String configFile : configFiles ) {
			metadataSources.addResource( configFile );
		}
		return ( SessionFactoryImplementor ) metadataSources.buildMetadata().buildSessionFactory();
	}

	// uses the first getter of the tupelizer for the assertions

	private void assertAccessType(SessionFactoryImplementor factory, Class<?> classUnderTest, AccessType accessType) {
		EntityTuplizer tuplizer = factory.getEntityPersister( classUnderTest.getName() )
				.getEntityMetamodel()
				.getTuplizer();
		if ( AccessType.FIELD.equals( accessType ) ) {
			Assert.assertTrue(
					"Field access was expected.",
					tuplizer.getGetter( 0 ) instanceof DirectPropertyAccessor.DirectGetter
			);
		}
		else {
			Assert.assertTrue(
					"Property access was expected.",
					tuplizer.getGetter( 0 ) instanceof BasicPropertyAccessor.BasicGetter
			);
		}
	}
}
