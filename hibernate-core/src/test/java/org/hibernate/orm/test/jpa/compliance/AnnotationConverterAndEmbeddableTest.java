/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				AnnotationConverterAndEmbeddableTest.Person.class
		},
		useCollectingStatementInspector = true
)
@JiraKey( "HHH-18028" )
public class AnnotationConverterAndEmbeddableTest {

	private static final String EXPECTED_ERROR_MESSAGE = "Exception was thrown by IntegerToStringConverter";

	@Test
	public void testConverterIsCorrectlyApplied(EntityManagerFactoryScope scope) {
		SQLStatementInspector sqlStatementInspector = (SQLStatementInspector) scope.getStatementInspector();
		sqlStatementInspector.clear();
		scope.inTransaction(
				entityManager -> {
					Person b = new Person(
							1,
							"and n.",
							new Address( "Localita S. Egidio n. 5", "Gradoli" )
					);
					entityManager.persist( b );
				}
		);
		List<String> sqlQueries = sqlStatementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 1 );
		sqlStatementInspector.assertIsInsert( 0 );
		String query = sqlQueries.get( 0 );
		assertThat( query.contains( "Localita S. Egidio # 5" ) );
		assertThat( query.contains( "and #" ) );
	}


	@Entity(name = "Person")
	@Converts(
			value = {
					@Convert(attributeName = "address.street", converter = AnnotationConverterAndEmbeddableTest.StreetConverter.class),
					@Convert(attributeName = "name", converter = AnnotationConverterAndEmbeddableTest.StreetConverter.class),
			}
	)
	public static class Person {

		protected Integer id;

		protected String name;


		protected Address address;

		public Person() {
		}

		public Person(Integer id, String name, Address address) {
			this.id = id;
			this.name = name;
			this.address = address;
		}

		@Id
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

		@Embedded
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
			return attribute.replace( "n.", "#" );
		}

		public String convertToEntityAttribute(String dbData) {
			return dbData.replace( "#", "n." );
		}
	}

}
