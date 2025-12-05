/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.elementCollection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-8529")
@DomainModel( annotatedClasses = { CollectionElementConversionTest.Customer.class, CollectionElementConversionTest.ColorConverter.class } )
@SessionFactory
public class CollectionElementConversionTest {

	@Test
	public void testElementCollectionConversion(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Customer customer = new Customer();
					customer.id = 1;
					customer.set = new HashSet<>();
					customer.set.add(Color.RED);
					customer.set.add(Color.GREEN);
					customer.set.add(Color.BLUE);
					customer.map = new HashMap<>();
					customer.map.put(Color.RED, Status.INACTIVE);
					customer.map.put(Color.GREEN, Status.ACTIVE);
					customer.map.put(Color.BLUE, Status.PENDING);
					session.persist(customer);
				}
		);

		scope.inTransaction(
				(session) -> {
					final Customer customer = session.find( Customer.class, 1 );
					assertEquals( customer.set, customer.set );
					assertEquals( customer.map, customer.map );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "Customer" )
	@Table(name = "Customer")
	public static class Customer {
		@Id
		private Integer id;
		@ElementCollection
		@Column(name = "`set`")
		private Set<Color> set;
		@ElementCollection
		@Enumerated(EnumType.STRING)
		private Map<Color, Status> map;
	}

	public static class Color {
		public static Color RED = new Color(0xFF0000);
		public static Color GREEN = new Color(0x00FF00);
		public static Color BLUE = new Color(0x0000FF);

		private final int rgb;

		public Color(int rgb) {
			this.rgb = rgb;
		}

		@Override
		public int hashCode() {
			return this.rgb;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Color && ((Color) obj).rgb == this.rgb;
		}
	}

	public enum Status {
		ACTIVE,
		INACTIVE,
		PENDING
	}

	@Converter(autoApply = true)
	public static class ColorConverter implements AttributeConverter<Color, String> {
		@Override
		public String convertToDatabaseColumn(Color attribute) {
			return attribute == null ? null : Integer.toString(attribute.rgb, 16);
		}

		@Override
		public Color convertToEntityAttribute(String dbData) {
			return dbData == null ? null : new Color(Integer.parseInt(dbData, 16));
		}
	}
}
