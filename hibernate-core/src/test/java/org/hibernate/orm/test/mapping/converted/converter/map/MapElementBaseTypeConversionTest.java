/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.map;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		MapElementBaseTypeConversionTest.Customer.class
})
@SessionFactory
@JiraKey("HHH-15733")
public class MapElementBaseTypeConversionTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Customer customer = new Customer();
			customer.getColors().put( "eyes", "blue" );
			session.persist( customer );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Customer" ).executeUpdate() );
	}

	@Test
	public void testBasicElementCollectionConversion(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Customer customer = session.find( Customer.class, 1 );
			assertEquals( 1, customer.getColors().size() );
			assertEquals( "blue", customer.getColors().get( "eyes" ) );
		} );
	}

	@Entity(name = "Customer")
	public static class Customer {
		@Id
		@GeneratedValue
		private Integer id;

		@ElementCollection(fetch = FetchType.EAGER)
		@Convert(converter = MyStringConverter.class) // note omitted attributeName
		private final Map<String, String> colors = new HashMap<>();

		public Map<String, String> getColors() {
			return colors;
		}
	}

	@Converter
	public static class MyStringConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return new StringBuilder( attribute ).reverse().toString();
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return new StringBuilder( dbData ).reverse().toString();
		}
	}
}
