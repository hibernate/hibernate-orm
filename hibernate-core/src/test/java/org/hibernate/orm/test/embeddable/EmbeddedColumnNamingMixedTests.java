/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.MappingException;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.embeddable.EmbeddedColumnNamingTests.verifyColumnNames;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class EmbeddedColumnNamingMixedTests {

	/**
	 * Test use of {@code @EmbeddedColumnNaming} one 2 separate embedded attributes with
	 * a nested embeddable using explicit column names
	 */
	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class, Person.class})
	@SessionFactory
	void smokeTest(SessionFactoryScope sfScope) {
		// atm this mixing is not supported / implemented - tbd
		try (SessionFactoryImplementor sessionFactory = sfScope.getSessionFactory()) {
			fail( "Expecting an exception" );
		}
		catch (RuntimeException e) {
			assertThat( e ).hasCauseExactlyInstanceOf( MappingException.class );
			assertThat( e ).cause().hasMessageStartingWith( "Column 'zip_code' is duplicated in mapping for entity" );
		}
	}

	/**
	 * Test use of {@code @EmbeddedColumnNaming} one 2 separate embedded attributes with
	 * a nested embeddable using explicit column names
	 */
	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class, Person.class})
	@SessionFactory
	@NotImplementedYet( reason = "this mixing is not supported / implemented - tbd" )
	void testMixedNamingPattern(SessionFactoryScope sfScope) {
		final SessionFactoryImplementor sessionFactory = sfScope.getSessionFactory();
		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getMappingMetamodel();
		final EntityPersister persister = mappingMetamodel.getEntityDescriptor( Person.class );
		verifyColumnNames( persister.findAttributeMapping( "homeAddress" ), "home_" );
		verifyColumnNames( persister.findAttributeMapping( "workAddress" ), "work_" );
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
		private ZipPlus zip;
	}

	@Embeddable
	public static class ZipPlus {
		@Column(name="zip_code")
		private String code;
		@Column(name="zip_plus4")
		private String plus4;
	}
}
