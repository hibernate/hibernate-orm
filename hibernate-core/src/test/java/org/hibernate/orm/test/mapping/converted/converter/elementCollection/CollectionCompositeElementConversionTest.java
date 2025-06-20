/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.elementCollection;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
				CollectionCompositeElementConversionTest.Disguise.class,
				CollectionCompositeElementConversionTest.ColorTypeConverter.class
		}
)
@SessionFactory
public class CollectionCompositeElementConversionTest {
	@Test
	public void testElementCollectionConversion(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Disguise disguise = new Disguise( 1 );
					disguise.traits.add( new Traits( ColorType.BLUE, ColorType.RED ) );
					session.persist( disguise );
				}
		);

		scope.inTransaction(
				(session) -> {
					assertEquals( 1, session.get( Disguise.class, 1 ).traits.size() );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "Disguise" )
	@Table( name = "DISGUISE" )
	public static class Disguise {
		@Id
		private Integer id;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable(
				name = "DISGUISE_TRAIT",
				joinColumns = @JoinColumn(name = "DISGUISE_FK", nullable = false)
		)
		private Set<Traits> traits = new HashSet<Traits>();

		public Disguise() {
		}

		public Disguise(Integer id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class Traits {
		public ColorType eyeColor;
		public ColorType hairColor;

		public Traits() {
		}

		public Traits(
				ColorType eyeColor,
				ColorType hairColor) {
			this.eyeColor = eyeColor;
			this.hairColor = hairColor;
		}
	}

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
