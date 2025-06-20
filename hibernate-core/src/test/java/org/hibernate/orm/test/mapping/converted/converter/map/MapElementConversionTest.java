/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.map;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-8529" )
@DomainModel(
		annotatedClasses = {
				MapElementConversionTest.Customer.class,
				MapElementConversionTest.ColorTypeConverter.class,
		}
)
@SessionFactory
public class MapElementConversionTest {
	@Test
	public void testElementCollectionConversion(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Customer customer = new Customer( 1 );
					customer.colors.put( "eyes", ColorType.BLUE );
					session.persist( customer );
				}
		);

		scope.inTransaction(
				(session) -> {
					assertEquals( 1, session.get( Customer.class, 1 ).colors.size() );
				}
		);

	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "Customer" )
	@Table( name = "CUST" )
	public static class Customer {
		@Id
		private Integer id;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable( name = "cust_color", joinColumns = @JoinColumn( name = "cust_fk" ) )
		@MapKeyColumn( name = "color_key" )
		@Column(name = "color", nullable = false)
		private Map<String,ColorType> colors = new HashMap<String,ColorType>();

		public Customer() {
		}

		public Customer(Integer id) {
			this.id = id;
		}
	}


	// an enum-like class (converters are technically not allowed to apply to enums)
	public static class ColorType {
		public static ColorType BLUE = new ColorType( "blue" );
		public static ColorType RED = new ColorType( "red" );
		public static ColorType YELLOW = new ColorType( "yellow" );

		private final String color;

		public ColorType(String color) {
			this.color = color;
		}

		public String toExternalForm() {
			return color;
		}

		public static ColorType fromExternalForm(String color) {
			if ( BLUE.color.equals( color ) ) {
				return BLUE;
			}
			else if ( RED.color.equals( color ) ) {
				return RED;
			}
			else if ( YELLOW.color.equals( color ) ) {
				return YELLOW;
			}
			else {
				throw new RuntimeException( "Unknown color : " + color );
			}
		}
	}

	@Converter( autoApply = true )
	public static class ColorTypeConverter implements AttributeConverter<ColorType, String> {

		@Override
		public String convertToDatabaseColumn(ColorType attribute) {
			return attribute == null ? null : attribute.toExternalForm();
		}

		@Override
		public ColorType convertToEntityAttribute(String dbData) {
			return ColorType.fromExternalForm( dbData );
		}
	}
}
