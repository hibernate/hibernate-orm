/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.jpa;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.internal.BasicEntityIdentifierMappingImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.descriptor.java.spi.JdbcTypeRecommendationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Tests verifying the correct behaviour for the usage of {@code @jakarta.persistence.Access}.
 *
 * @author Hardy Ferentschik
 */
@BaseUnitTest
public class AccessMappingTest {
	private ServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testInconsistentAnnotationPlacement() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Course1.class );
		cfg.addAnnotatedClass( Student.class );
		try (SessionFactory sf = cfg.buildSessionFactory( serviceRegistry )) {
			fail( "@Id and @OneToMany are not placed consistently in test entities. SessionFactory creation should fail." );
		}
		catch (MappingException | JdbcTypeRecommendationException e) {
			// success
		}
	}

	@Test
	public void testFieldAnnotationPlacement() {
		Configuration cfg = new Configuration();
		Class<?> classUnderTest = Course6.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );
		try (SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg
				.buildSessionFactory( serviceRegistry )) {
			final EntityPersister entityPersister = factory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( classUnderTest.getName() );
			final BasicEntityIdentifierMappingImpl identifierMapping = (BasicEntityIdentifierMappingImpl) entityPersister.getIdentifierMapping();

			assertThat( identifierMapping.getPropertyAccess().getGetter() )
					.describedAs( "Field access should be used." )
					.isInstanceOf( GetterFieldImpl.class );
		}
	}

	@Test
	public void testPropertyAnnotationPlacement() {
		Configuration cfg = new Configuration();
		Class<?> classUnderTest = Course7.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );

		try (SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg
				.buildSessionFactory( serviceRegistry )) {
			final EntityPersister entityPersister = factory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( classUnderTest.getName() );
			final BasicEntityIdentifierMappingImpl identifierMapping = (BasicEntityIdentifierMappingImpl) entityPersister.getIdentifierMapping();

			assertThat( identifierMapping.getPropertyAccess().getGetter() )
					.describedAs( "Property access should be used." )
					.isInstanceOf( GetterMethodImpl.class );
		}
	}

	@Test
	public void testExplicitPropertyAccessAnnotationsOnProperty() {
		Configuration cfg = new Configuration();
		Class<?> classUnderTest = Course2.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );
		try (SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg
				.buildSessionFactory( serviceRegistry )) {
			final EntityPersister entityPersister = factory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( classUnderTest.getName() );
			final BasicEntityIdentifierMappingImpl identifierMapping = (BasicEntityIdentifierMappingImpl) entityPersister.getIdentifierMapping();

			assertThat( identifierMapping.getPropertyAccess().getGetter() )
					.describedAs( "Property access should be used." )
					.isInstanceOf( GetterMethodImpl.class );
		}
	}

	@Test
	public void testExplicitPropertyAccessAnnotationsOnField() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Course4.class );
		cfg.addAnnotatedClass( Student.class );
		try (SessionFactory sf = cfg.buildSessionFactory( serviceRegistry )) {
			fail( "@Id and @OneToMany are not placed consistently in test entities. SessionFactory creation should fail." );
		}
		catch (MappingException e) {
			// success
		}
	}

	@Test
	public void testExplicitPropertyAccessAnnotationsWithHibernateStyleOverride() {
		Configuration cfg = new Configuration();
		Class<?> classUnderTest = Course3.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );
		try (SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg
				.buildSessionFactory( serviceRegistry )) {
			final EntityPersister entityPersister = factory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( classUnderTest.getName() );
			final BasicEntityIdentifierMappingImpl identifierMapping = (BasicEntityIdentifierMappingImpl) entityPersister.getIdentifierMapping();

			assertThat( identifierMapping.getPropertyAccess().getGetter() )
					.describedAs( "Field access should be used." )
					.isInstanceOf( GetterFieldImpl.class );

			assertThat( entityPersister.getAttributeMapping( 0 ).getPropertyAccess().getGetter() )
					.describedAs( "Property access should be used." )
					.isInstanceOf( GetterMethodImpl.class );
		}
	}

	@Test
	public void testExplicitPropertyAccessAnnotationsWithJpaStyleOverride() {
		Configuration cfg = new Configuration();
		Class<?> classUnderTest = Course5.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Student.class );
		try (SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg
				.buildSessionFactory( serviceRegistry )) {
			final EntityPersister entityPersister = factory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( classUnderTest.getName() );
			final BasicEntityIdentifierMappingImpl identifierMapping = (BasicEntityIdentifierMappingImpl) entityPersister.getIdentifierMapping();

			assertThat(

					identifierMapping.getPropertyAccess().getGetter()
			)
					.describedAs( "Field access should be used." )
					.isInstanceOf( GetterFieldImpl.class );

			assertThat( entityPersister.getAttributeMapping( 0 ).getPropertyAccess().getGetter() )
					.describedAs( "Property access should be used." )
					.isInstanceOf( GetterMethodImpl.class );
		}
	}

	@Test
	public void testDefaultFieldAccessIsInherited() {
		Configuration cfg = new Configuration();
		Class<?> classUnderTest = User.class;
		cfg.addAnnotatedClass( classUnderTest );
		cfg.addAnnotatedClass( Person.class );
		cfg.addAnnotatedClass( Being.class );
		try (SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg
				.buildSessionFactory( serviceRegistry )) {
			final EntityPersister entityPersister = factory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( classUnderTest.getName() );
			final BasicEntityIdentifierMappingImpl identifierMapping = (BasicEntityIdentifierMappingImpl) entityPersister.getIdentifierMapping();

			assertThat(
					identifierMapping.getPropertyAccess().getGetter() )
					.describedAs( "Field access should be used since the default access mode gets inherited" )
					.isInstanceOf( GetterFieldImpl.class );
			;
		}
	}

	@Test
	public void testDefaultPropertyAccessIsInherited() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Horse.class );
		cfg.addAnnotatedClass( Animal.class );

		try (SessionFactoryImplementor factory = (SessionFactoryImplementor) cfg
				.buildSessionFactory( serviceRegistry )) {
			EntityPersister entityPersister = factory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( Animal.class.getName() );
			final BasicEntityIdentifierMappingImpl identifierMapping = (BasicEntityIdentifierMappingImpl) entityPersister.getIdentifierMapping();

			assertThat( identifierMapping.getPropertyAccess().getGetter() )
					.describedAs( "Property access should be used since explicity configured via @Access" )
					.isInstanceOf( GetterMethodImpl.class );

			entityPersister = factory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( Horse.class.getName() );

			assertThat( entityPersister.getAttributeMapping( 0 ).getPropertyAccess().getGetter() )
					.describedAs( "Field access should be used since the default access mode gets inherited" )
					.isInstanceOf( GetterFieldImpl.class );
		}
	}

	@JiraKey(value = "HHH-5004")
	@Test
	public void testAccessOnClassAndId() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Course8.class );
		cfg.addAnnotatedClass( Student.class );
		cfg.buildSessionFactory( serviceRegistry ).close();
	}
}
