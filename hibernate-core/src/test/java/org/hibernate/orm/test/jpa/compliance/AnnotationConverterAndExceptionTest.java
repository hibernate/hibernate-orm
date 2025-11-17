/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Converts;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(
		annotatedClasses = {
				AnnotationConverterAndExceptionTest.Person.class
		}
)
@JiraKey("HHH-18028")
public class AnnotationConverterAndExceptionTest {

	private static final String EXPECTED_ERROR_MESSAGE = "Exception was thrown by IntegerToStringConverter";

	@Test
	public void testExceptionThrownByConverterIsPropagated(EntityManagerFactoryScope scope) {
		PersistenceException persistenceException = assertThrows( PersistenceException.class, () ->
				scope.inEntityManager(
						entityManager -> {
							entityManager.getTransaction().begin();
							try {
								Person b = new Person(
										1,
										"and",
										new Address( "Localita S. Egidio n. 5", "Gradoli" )
								);
								entityManager.persist( b );
								entityManager.flush();
								fail( "Persistence exception expected" );
							}
							catch (PersistenceException pe) {
								assertTrue( entityManager.getTransaction().getRollbackOnly() );
								throw pe;
							}
							finally {
								if ( entityManager.getTransaction().isActive() ) {
									entityManager.getTransaction().rollback();
								}
							}
						}
				)
		);
		assertThat( persistenceException.getMessage() ).contains( EXPECTED_ERROR_MESSAGE );
	}


	@Entity(name = "Person")
	public static class Person {

		@Id
		protected Integer id;

		protected String name;

		@Embedded
		@Converts(
				value = {
						@Convert(attributeName = "street", converter = StreetConverter.class),
				}
		)
		protected Address address;

		public Person() {
		}

		public Person(Integer id, String name, Address address) {
			this.id = id;
			this.name = name;
			this.address = address;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Embeddable
	@Access(AccessType.PROPERTY)
	public static class Address {

		protected String street;

		protected String city;

		public Address() {
		}

		public Address(String street, String city) {
			this.street = street;
			this.city = city;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

	}

	@Converter
	public static class StreetConverter implements AttributeConverter<String, String> {

		public String convertToDatabaseColumn(String attribute) {
			throw new RuntimeException( EXPECTED_ERROR_MESSAGE );
		}

		public String convertToEntityAttribute(String dbData) {
			return dbData.replace( "#", "n." );
		}
	}

}
