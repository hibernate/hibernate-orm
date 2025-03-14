/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable.naming;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.embeddable.naming.EmbeddedColumnNamingTests.verifyColumnNames;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class EmbeddedColumnNamingNestedTests {

	/**
	 * Test use of {@code @EmbeddedColumnNaming} one 2 separate embedded attributes
	 */
	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class, Person.class})
	@SessionFactory
	void testNestedNamingPattern(SessionFactoryScope sfScope) {
		final SessionFactoryImplementor sessionFactory = sfScope.getSessionFactory();
		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getMappingMetamodel();
		final EntityPersister persister = mappingMetamodel.getEntityDescriptor( Person.class );

		final EmbeddedAttributeMapping homeAddressMapping = (EmbeddedAttributeMapping) persister.findAttributeMapping( "homeAddress" );
		verifyColumnNames( homeAddressMapping, "home_" );
		final EmbeddedAttributeMapping homeZipMapping = (EmbeddedAttributeMapping) homeAddressMapping.getEmbeddableTypeDescriptor().findAttributeMapping( "zip" );
		verifyColumnNames( homeZipMapping, "home_zip" );

		final EmbeddedAttributeMapping workAddressMapping = (EmbeddedAttributeMapping) persister.findAttributeMapping( "workAddress" );
		verifyColumnNames( workAddressMapping, "work_" );
		final EmbeddedAttributeMapping workZipMapping = (EmbeddedAttributeMapping) workAddressMapping.getEmbeddableTypeDescriptor().findAttributeMapping( "zip" );
		verifyColumnNames( workZipMapping, "work_zip" );
	}

	@Entity(name="Person")
	@Table(name="person")
	public static class Person {
		@Id
		private Integer id;
		private String name;

		@Embedded
		@EmbeddedColumnNaming("home_%s")
		private Address homeAddress;

		@Embedded
		@EmbeddedColumnNaming("work_%s")
		private Address workAddress;
	}

	@Embeddable
	public static class Address {
		private String street;
		private String city;
		private String state;
		@Embedded
		@EmbeddedColumnNaming( "zip_%s" )
		private ZipPlus zip;
	}

	@Embeddable
	public static class ZipPlus {
		private String code;
		private String plus4;
	}
}
