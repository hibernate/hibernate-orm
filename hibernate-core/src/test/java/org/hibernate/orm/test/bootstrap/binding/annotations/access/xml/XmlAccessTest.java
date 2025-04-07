/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.xml;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AccessType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test verifying that it is possible to configure the access type via xml configuration.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class XmlAccessTest {

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = Tourist.class)
	@SessionFactory
	void testBaseline(SessionFactoryScope factoryScope) {
		// without any xml configuration we have field access
		assertAccessType( factoryScope.getSessionFactory(), Tourist.class, AccessType.FIELD );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = Tourist.class, xmlMappings = "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Tourist.xml")
	@SessionFactory
	public void testAccessOnBasicXmlElement(SessionFactoryScope factoryScope) {
		assertAccessType( factoryScope.getSessionFactory(), Tourist.class, AccessType.PROPERTY );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = Tourist.class, xmlMappings = "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Tourist2.xml")
	@SessionFactory
	public void testAccessOnPersistenceUnitDefaultsXmlElement(SessionFactoryScope factoryScope) {
		assertAccessType( factoryScope.getSessionFactory(), Tourist.class, AccessType.PROPERTY );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = Tourist.class, xmlMappings = "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Tourist3.xml")
	@SessionFactory
	public void testAccessOnEntityMappingsXmlElement(SessionFactoryScope factoryScope) {
		assertAccessType( factoryScope.getSessionFactory(), Tourist.class, AccessType.PROPERTY );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = Tourist.class, xmlMappings = "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Tourist4.xml")
	@SessionFactory
	public void testAccessOnEntityXmlElement(SessionFactoryScope factoryScope) {
		assertAccessType( factoryScope.getSessionFactory(), Tourist.class, AccessType.PROPERTY );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = Waiter.class, xmlMappings = "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Crew.xml")
	@SessionFactory
	public void testAccessOnMappedSuperClassXmlElement(SessionFactoryScope factoryScope) {
		assertAccessType( factoryScope.getSessionFactory(), Waiter.class, AccessType.FIELD );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {RentalCar.class, Driver.class}, xmlMappings = "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/RentalCar.xml")
	@SessionFactory
	public void testAccessOnAssociationXmlElement(SessionFactoryScope factoryScope) {
		assertAccessType( factoryScope.getSessionFactory(), RentalCar.class, AccessType.PROPERTY );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Cook.class, Knive.class}, xmlMappings = "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Cook.xml")
	@SessionFactory
	public void testAccessOnEmbeddedXmlElement(SessionFactoryScope factoryScope) {
		assertAccessType( factoryScope.getSessionFactory(), Cook.class, AccessType.PROPERTY );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = Boy.class, xmlMappings = "org/hibernate/orm/test/bootstrap/binding.annotations.access.xml/Boy.xml")
	@SessionFactory
	public void testAccessOnElementCollectionXmlElement(SessionFactoryScope factoryScope) {
		assertAccessType( factoryScope.getSessionFactory(), Boy.class, AccessType.PROPERTY );
	}


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
