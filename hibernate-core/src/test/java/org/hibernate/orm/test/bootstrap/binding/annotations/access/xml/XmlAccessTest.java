/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AccessType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test verifying that it is possible to configure the access type via xml configuration.
 *
 * @author Hardy Ferentschik
 */
public class XmlAccessTest {

	@Test
	public void testAccessOnBasicXmlElement() {
		Class<?> classUnderTest = Tourist.class;
		List<Class<?>> classes = new ArrayList<>();
		classes.add( classUnderTest );
		List<String> configFiles = Collections.emptyList();
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );

		// without any xml configuration we have field access
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
		// now with an additional xml configuration file changing the default access type for Tourist using basic
		configFiles = new ArrayList<>();
		configFiles.add( "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Tourist.xml" );
		factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnPersistenceUnitDefaultsXmlElement() {
		Class<?> classUnderTest = Tourist.class;
		List<Class<?>> classes = new ArrayList<>();
		classes.add( classUnderTest );
		List<String> configFiles = Collections.emptyList();
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );

		// without any xml configuration we have field access
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
		// now with an additional xml configuration file changing the default access type for Tourist using persitence unit defaults
		configFiles = new ArrayList<>();
		configFiles.add( "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Tourist2.xml" );
		factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnEntityMappingsXmlElement() {
		Class<?> classUnderTest = Tourist.class;
		List<Class<?>> classes = new ArrayList<>();
		classes.add( classUnderTest );
		List<String> configFiles = Collections.emptyList();
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );

		// without any xml configuration we have field access
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
		// now with an additional xml configuration file changing the default access type for Tourist using default in entity-mappings
		configFiles = new ArrayList<>();
		configFiles.add( "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Tourist3.xml" );
		factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnEntityXmlElement() {
		Class<?> classUnderTest = Tourist.class;
		List<Class<?>> classes = new ArrayList<>();
		classes.add( classUnderTest );
		List<String> configFiles = Collections.emptyList();
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );

		// without any xml configuration we have field access
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
		// now with an additional xml configuration file changing the default access type for Tourist using entity level config
		configFiles = new ArrayList<>();
		configFiles.add( "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Tourist4.xml" );
		factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnMappedSuperClassXmlElement() {
		Class<?> classUnderTest = Waiter.class;
		List<Class<?>> classes = new ArrayList<>();
		classes.add( classUnderTest );
		classes.add( Crew.class );
		List<String> configFiles = new ArrayList<>();
		configFiles.add( "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Crew.xml" );
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.FIELD );
		factory.close();
	}

	@Test
	public void testAccessOnAssociationXmlElement() {
		Class<?> classUnderTest = RentalCar.class;
		List<Class<?>> classes = new ArrayList<>();
		classes.add( classUnderTest );
		classes.add( Driver.class );
		List<String> configFiles = new ArrayList<>();
		configFiles.add( "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/RentalCar.xml" );
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnEmbeddedXmlElement() {
		Class<?> classUnderTest = Cook.class;
		List<Class<?>> classes = new ArrayList<>();
		classes.add( classUnderTest );
		classes.add( Knive.class );
		List<String> configFiles = new ArrayList<>();
		configFiles.add( "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Cook.xml" );
		SessionFactoryImplementor factory = buildSessionFactory( classes, configFiles );
		assertAccessType( factory, classUnderTest, AccessType.PROPERTY );
		factory.close();
	}

	@Test
	public void testAccessOnElementCollectionXmlElement() {
		Class<?> classUnderTest = Boy.class;
		List<Class<?>> classes = new ArrayList<>();
		classes.add( classUnderTest );
		List<String> configFiles = new ArrayList<>();
		configFiles.add( "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Boy.xml" );
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
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
		for ( String configFile : configFiles ) {
			try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( configFile )) {
				cfg.addInputStream( is );
			}
			catch (IOException e) {
				throw new IllegalArgumentException( e );
			}
		}
		return (SessionFactoryImplementor) cfg.buildSessionFactory();
	}

	// uses the first getter of the tupelizer for the assertions

	private void assertAccessType(SessionFactoryImplementor factory, Class<?> classUnderTest, AccessType accessType) {
		final EntityPersister entityDescriptor = factory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findEntityDescriptor( classUnderTest.getName() );
		final AttributeMappingsList attributeMappings = entityDescriptor.getAttributeMappings();
		final AttributeMapping attributeMapping = attributeMappings.get( 0 );

		final Getter accessGetter = attributeMapping.getPropertyAccess().getGetter();

		if ( AccessType.FIELD.equals( accessType ) ) {
			assertThat( accessGetter )
					.withFailMessage( "FIELD access was expected." )
					.isInstanceOf( GetterFieldImpl.class );
		}
		else {
			assertThat( accessGetter )
					.withFailMessage( "PROPERTY (method) access was expected." )
					.isInstanceOf( GetterMethodImpl.class );
		}
	}
}
