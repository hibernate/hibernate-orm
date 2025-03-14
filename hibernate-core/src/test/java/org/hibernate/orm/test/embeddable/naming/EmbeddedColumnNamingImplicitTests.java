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
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.embeddable.naming.EmbeddedColumnNamingTests.verifyColumnNames;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class EmbeddedColumnNamingImplicitTests {
	@Test
	@DomainModel( annotatedClasses = {Person.class, Address.class} )
	@SessionFactory(exportSchema = false)
	void testNaming(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getMappingMetamodel();
		final EntityPersister persister = mappingMetamodel.getEntityDescriptor( Person.class );
		verifyColumnNames( persister.findAttributeMapping( "homeAddress" ), "homeAddress_" );
		verifyColumnNames( persister.findAttributeMapping( "workAddress" ), "workAddress_" );
	}

	@Entity(name="Person")
	@Table(name="person")
	public static class Person {
		@Id
		private Integer id;
		private String name;

		@Embedded
		@EmbeddedColumnNaming
		private Address homeAddress;

		@Embedded
		@EmbeddedColumnNaming
		private Address workAddress;
	}

	@Embeddable
	public static class Address {
		private String street;
		private String city;
		private String state;
		private String zip;
	}
}
