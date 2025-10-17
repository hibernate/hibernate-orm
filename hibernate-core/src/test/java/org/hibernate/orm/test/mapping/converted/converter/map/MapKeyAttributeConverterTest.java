/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.map;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Janario Oliveira
 */
@DomainModel(
		annotatedClasses = {
				MapKeyAttributeConverterTest.MapEntity.class,
				MapKeyAttributeConverterTest.MapValue.class,

				ColorTypeConverter.class,

				MapKeyAttributeConverterTest.CustomColorTypeConverter.class,
				MapKeyAttributeConverterTest.ImplicitEnumMapKeyConverter.class,
				MapKeyAttributeConverterTest.ExplicitEnumMapKeyConverter.class,
				MapKeyAttributeConverterTest.ImplicitEnumMapKeyOverriddenConverter.class
		}
)
@SessionFactory
public class MapKeyAttributeConverterTest {

	@Test
	public void testImplicitType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.implicitType = ColorType.BLUE;
			mapValue.mapEntity.implicitType.put( mapValue.implicitType, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {
			assertEquals( 1, found.implicitType.size() );
			MapValue foundValue = found.implicitType.get( ColorType.BLUE );
			assertEquals( ColorType.BLUE, foundValue.implicitType );

			assertEquals( "blue", findDatabaseValue( session, foundValue.id, "implicitType", String.class ) );
		} );
	}

	@Test
	public void testExplicitType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.explicitType = ColorType.RED;
			mapValue.mapEntity.explicitType.put( mapValue.explicitType, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {

			assertEquals( 1, found.explicitType.size() );
			MapValue foundValue = found.explicitType.get( ColorType.RED );
			assertEquals( ColorType.RED, foundValue.explicitType );

			assertEquals( "COLOR-red", findDatabaseValue( session, foundValue.id, "explicitType", String.class ) );
		} );
	}

	@Test
	public void testEnumDefaultType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.enumDefault = EnumMapKey.VALUE_1;
			mapValue.mapEntity.enumDefaultType.put( mapValue.enumDefault, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {
			assertEquals( 1, found.enumDefaultType.size() );
			MapValue foundValue = found.enumDefaultType.get( EnumMapKey.VALUE_1 );
			assertEquals( EnumMapKey.VALUE_1, foundValue.enumDefault );

			assertEquals( 0, findDatabaseValue( session, foundValue.id, "enumDefault", Integer.class ) );
		} );
	}

	@Test
	public void testEnumExplicitOrdinalType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.enumExplicitOrdinal = EnumMapKey.VALUE_2;
			mapValue.mapEntity.enumExplicitOrdinalType.put( mapValue.enumExplicitOrdinal, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {
			assertEquals( 1, found.enumExplicitOrdinalType.size() );
			MapValue foundValue = found.enumExplicitOrdinalType.get( EnumMapKey.VALUE_2 );
			assertEquals( EnumMapKey.VALUE_2, foundValue.enumExplicitOrdinal );

			assertEquals( 1, findDatabaseValue( session, foundValue.id, "enumExplicitOrdinal", Integer.class ) );
		} );
	}

	@Test
	public void testEnumExplicitStringType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.enumExplicitString = EnumMapKey.VALUE_1;
			mapValue.mapEntity.enumExplicitStringType.put( mapValue.enumExplicitString, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {
			assertEquals( 1, found.enumExplicitStringType.size() );
			MapValue foundValue = found.enumExplicitStringType.get( EnumMapKey.VALUE_1 );
			assertEquals( EnumMapKey.VALUE_1, foundValue.enumExplicitString );

			assertEquals( "VALUE_1", findDatabaseValue( session, foundValue.id, "enumExplicitString", String.class ) );
		} );
	}

	@Test
	public void testEnumExplicitType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.enumExplicit = EnumMapKey.VALUE_2;
			mapValue.mapEntity.enumExplicitType.put( mapValue.enumExplicit, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {
			assertEquals( 1, found.enumExplicitType.size() );
			MapValue foundValue = found.enumExplicitType.get( EnumMapKey.VALUE_2 );
			assertEquals( EnumMapKey.VALUE_2, foundValue.enumExplicit );

			assertEquals( "2", findDatabaseValue( session, foundValue.id, "enumExplicit", String.class ) );
		} );
	}

	@Test
	public void testEnumImplicitType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.enumImplicit = ImplicitEnumMapKey.VALUE_2;
			mapValue.mapEntity.enumImplicitType.put( mapValue.enumImplicit, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {
			assertEquals( 1, found.enumImplicitType.size() );
			MapValue foundValue = found.enumImplicitType.get( ImplicitEnumMapKey.VALUE_2 );
			assertEquals( ImplicitEnumMapKey.VALUE_2, foundValue.enumImplicit );

			assertEquals( "I2", findDatabaseValue( session, foundValue.id, "enumImplicit", String.class ) );
		} );
	}

	@Test
	public void testEnumImplicitOverrideOrdinalType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.enumImplicitOverrideOrdinal = ImplicitEnumMapKey.VALUE_1;
			mapValue.mapEntity.enumImplicitOverrideOrdinalType.put( mapValue.enumImplicitOverrideOrdinal, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {
			assertEquals( 1, found.enumImplicitOverrideOrdinalType.size() );
			MapValue foundValue = found.enumImplicitOverrideOrdinalType.get( ImplicitEnumMapKey.VALUE_1 );
			assertEquals( ImplicitEnumMapKey.VALUE_1, foundValue.enumImplicitOverrideOrdinal );

			assertEquals( 0, findDatabaseValue( session, foundValue.id, "enumImplicitOverrideOrdinal", Integer.class ) );
		} );
	}

	@Test
	public void testEnumImplicitOverrideStringType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.enumImplicitOverrideString = ImplicitEnumMapKey.VALUE_2;
			mapValue.mapEntity.enumImplicitOverrideStringType.put( mapValue.enumImplicitOverrideString, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {
			assertEquals( 1, found.enumImplicitOverrideStringType.size() );
			MapValue foundValue = found.enumImplicitOverrideStringType.get( ImplicitEnumMapKey.VALUE_2 );
			assertEquals( ImplicitEnumMapKey.VALUE_2, foundValue.enumImplicitOverrideString );

			assertEquals( "VALUE_2", findDatabaseValue( session, foundValue.id, "enumImplicitOverrideString", String.class ) );
		} );
	}

	@Test
	public void testEnumImplicitOverriddenType(SessionFactoryScope scope) {
		MapEntity found = scope.fromTransaction( session -> {
			MapValue mapValue = create();
			mapValue.enumImplicitOverridden = ImplicitEnumMapKey.VALUE_1;
			mapValue.mapEntity.enumImplicitOverriddenType.put( mapValue.enumImplicitOverridden, mapValue );

			return persist( session, mapValue.mapEntity );
		} );

		scope.inSession( session -> {
			assertEquals( 1, found.enumImplicitOverriddenType.size() );
			MapValue foundValue = found.enumImplicitOverriddenType.get( ImplicitEnumMapKey.VALUE_1 );
			assertEquals( ImplicitEnumMapKey.VALUE_1, foundValue.enumImplicitOverridden );

			assertEquals( "O1", findDatabaseValue( session, foundValue.id, "enumImplicitOverridden", String.class ) );
		} );
	}

	private MapValue create() {
		MapEntity mapEntity = new MapEntity();
		return new MapValue( mapEntity );
	}

	private MapEntity persist(Session session, final MapEntity mapEntity) {
		session.persist(mapEntity);

		return session.find(MapEntity.class, mapEntity.id);
	}

	private Object findDatabaseValue(Session session, Integer id, String columnName, Class<?> expectedResultType) {
		return session.createNativeQuery(
				"select mv." + columnName + " from map_value mv where mv.id=:id", expectedResultType )
						.setParameter( "id", id )
						.uniqueResult();
	}

	@Entity( name = "MapEntity" )
	@Table( name = "map_entity" )
	public static class MapEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Integer id;

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@MapKey(name = "implicitType")
		private Map<ColorType, MapValue> implicitType = new HashMap<>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "explicitType")
		private Map<ColorType, MapValue> explicitType = new HashMap<>();

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumDefault")
		private Map<EnumMapKey, MapValue> enumDefaultType = new HashMap<>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumExplicitOrdinal")
		private Map<EnumMapKey, MapValue> enumExplicitOrdinalType = new HashMap<>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumExplicitString")
		private Map<EnumMapKey, MapValue> enumExplicitStringType = new HashMap<>();

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumExplicit")
		private Map<EnumMapKey, MapValue> enumExplicitType = new HashMap<>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicit")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitType = new HashMap<>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicitOverrideOrdinal")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitOverrideOrdinalType = new HashMap<>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicitOverrideString")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitOverrideStringType = new HashMap<>();

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicitOverridden")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitOverriddenType = new HashMap<>();
	}

	@Entity( name = "MapValue" )
	@Table(name = "map_value")
	public static class MapValue {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Integer id;
		@ManyToOne
		@JoinColumn(name = "map_entity_id")
		private MapEntity mapEntity;

		private ColorType implicitType;
		@Convert(converter = CustomColorTypeConverter.class)
		private ColorType explicitType;

		private EnumMapKey enumDefault;
		@Enumerated
		private EnumMapKey enumExplicitOrdinal;
		@Enumerated(EnumType.STRING)
		private EnumMapKey enumExplicitString;
		@Convert(converter = ExplicitEnumMapKeyConverter.class)
		private EnumMapKey enumExplicit;

		private ImplicitEnumMapKey enumImplicit;
		@Enumerated
		@Convert(disableConversion = true)
		private ImplicitEnumMapKey enumImplicitOverrideOrdinal;
		@Enumerated(EnumType.STRING)
		@Convert(disableConversion = true)
		private ImplicitEnumMapKey enumImplicitOverrideString;

		@Convert(converter = ImplicitEnumMapKeyOverriddenConverter.class)
		private ImplicitEnumMapKey enumImplicitOverridden;

		protected MapValue() {
		}

		public MapValue(MapEntity mapEntity) {
			this.mapEntity = mapEntity;
		}
	}

	public enum EnumMapKey {
		VALUE_1,
		VALUE_2
	}

	public enum ImplicitEnumMapKey {
		VALUE_1,
		VALUE_2
	}


	@Converter
	public static class CustomColorTypeConverter implements AttributeConverter<ColorType, String> {
		@Override
		public String convertToDatabaseColumn(ColorType attribute) {
			return attribute == null ? null : "COLOR-" + attribute.toExternalForm();
		}

		@Override
		public ColorType convertToEntityAttribute(String dbData) {
			return dbData == null ? null : ColorType.fromExternalForm( dbData.substring( 6 ) );
		}
	}

	@Converter
	public static class ExplicitEnumMapKeyConverter implements AttributeConverter<EnumMapKey, String> {
		@Override
		public String convertToDatabaseColumn(EnumMapKey attribute) {
			return attribute == null ? null : attribute.name().substring( attribute.name().length() - 1 );
		}

		@Override
		public EnumMapKey convertToEntityAttribute(String dbData) {
			return dbData == null ? null : EnumMapKey.valueOf( "VALUE_" + dbData );
		}
	}

	@Converter(autoApply = true)
	public static class ImplicitEnumMapKeyConverter implements AttributeConverter<ImplicitEnumMapKey, String> {
		@Override
		public String convertToDatabaseColumn(ImplicitEnumMapKey attribute) {
			return attribute == null ? null : "I" + attribute.name().substring( attribute.name().length() - 1 );
		}

		@Override
		public ImplicitEnumMapKey convertToEntityAttribute(String dbData) {
			return dbData == null ? null : ImplicitEnumMapKey.valueOf( "VALUE_" + dbData.substring( 1 ) );
		}
	}


	@Converter
	public static class ImplicitEnumMapKeyOverriddenConverter implements AttributeConverter<ImplicitEnumMapKey, String> {
		@Override
		public String convertToDatabaseColumn(ImplicitEnumMapKey attribute) {
			return attribute == null ? null :
					( "O" + attribute.name().substring( attribute.name().length() - 1 ) );
		}

		@Override
		public ImplicitEnumMapKey convertToEntityAttribute(String dbData) {
			return dbData == null ? null : ImplicitEnumMapKey.valueOf( "VALUE_" + dbData.substring( 1 ) );
		}
	}
}
