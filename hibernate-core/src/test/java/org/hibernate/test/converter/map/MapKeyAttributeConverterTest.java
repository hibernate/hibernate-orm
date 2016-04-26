/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.map;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Janario Oliveira
 */
public class MapKeyAttributeConverterTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				MapEntity.class, MapValue.class,

				ColorTypeConverter.class,

				CustomColorTypeConverter.class,
				ImplicitEnumMapKeyConverter.class,
				ExplicitEnumMapKeyConverter.class,
				ImplicitEnumMapKeyOverridedConverter.class
		};
	}

	@Test
	public void testImplicitType() {
		MapValue mapValue = create();
		mapValue.implicitType = ColorType.BLUE;
		mapValue.mapEntity.implicitType.put( mapValue.implicitType, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.implicitType.size() );
		MapValue foundValue = found.implicitType.get( ColorType.BLUE );
		assertEquals( ColorType.BLUE, foundValue.implicitType );

		assertEquals( "blue", findDatabaseValue( foundValue, "implicitType" ) );
		getSession().close();
	}

	@Test
	public void testExplicitType() {
		MapValue mapValue = create();
		mapValue.explicitType = ColorType.RED;
		mapValue.mapEntity.explicitType.put( mapValue.explicitType, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.explicitType.size() );
		MapValue foundValue = found.explicitType.get( ColorType.RED );
		assertEquals( ColorType.RED, foundValue.explicitType );

		assertEquals( "COLOR-red", findDatabaseValue( foundValue, "explicitType" ) );
		getSession().close();
	}

	@Test
	public void testEnumDefaultType() {
		MapValue mapValue = create();
		mapValue.enumDefault = EnumMapKey.VALUE_1;
		mapValue.mapEntity.enumDefaultType.put( mapValue.enumDefault, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.enumDefaultType.size() );
		MapValue foundValue = found.enumDefaultType.get( EnumMapKey.VALUE_1 );
		assertEquals( EnumMapKey.VALUE_1, foundValue.enumDefault );

		assertEquals( 0, ((Number) findDatabaseValue( foundValue, "enumDefault" )).intValue() );
		getSession().close();
	}

	@Test
	public void testEnumExplicitOrdinalType() {
		MapValue mapValue = create();
		mapValue.enumExplicitOrdinal = EnumMapKey.VALUE_2;
		mapValue.mapEntity.enumExplicitOrdinalType.put( mapValue.enumExplicitOrdinal, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.enumExplicitOrdinalType.size() );
		MapValue foundValue = found.enumExplicitOrdinalType.get( EnumMapKey.VALUE_2 );
		assertEquals( EnumMapKey.VALUE_2, foundValue.enumExplicitOrdinal );

		assertEquals( 1, ((Number) findDatabaseValue( foundValue, "enumExplicitOrdinal" )).intValue() );
		getSession().close();
	}

	@Test
	public void testEnumExplicitStringType() {
		MapValue mapValue = create();
		mapValue.enumExplicitString = EnumMapKey.VALUE_1;
		mapValue.mapEntity.enumExplicitStringType.put( mapValue.enumExplicitString, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.enumExplicitStringType.size() );
		MapValue foundValue = found.enumExplicitStringType.get( EnumMapKey.VALUE_1 );
		assertEquals( EnumMapKey.VALUE_1, foundValue.enumExplicitString );

		assertEquals( "VALUE_1", findDatabaseValue( foundValue, "enumExplicitString" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitType() {
		MapValue mapValue = create();
		mapValue.enumExplicit = EnumMapKey.VALUE_2;
		mapValue.mapEntity.enumExplicitType.put( mapValue.enumExplicit, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.enumExplicitType.size() );
		MapValue foundValue = found.enumExplicitType.get( EnumMapKey.VALUE_2 );
		assertEquals( EnumMapKey.VALUE_2, foundValue.enumExplicit );

		assertEquals( "2", findDatabaseValue( foundValue, "enumExplicit" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitType() {
		MapValue mapValue = create();
		mapValue.enumImplicit = ImplicitEnumMapKey.VALUE_2;
		mapValue.mapEntity.enumImplicitType.put( mapValue.enumImplicit, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.enumImplicitType.size() );
		MapValue foundValue = found.enumImplicitType.get( ImplicitEnumMapKey.VALUE_2 );
		assertEquals( ImplicitEnumMapKey.VALUE_2, foundValue.enumImplicit );

		assertEquals( "I2", findDatabaseValue( foundValue, "enumImplicit" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitOverrideOrdinalType() {
		MapValue mapValue = create();
		mapValue.enumImplicitOverrideOrdinal = ImplicitEnumMapKey.VALUE_1;
		mapValue.mapEntity.enumImplicitOverrideOrdinalType.put( mapValue.enumImplicitOverrideOrdinal, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.enumImplicitOverrideOrdinalType.size() );
		MapValue foundValue = found.enumImplicitOverrideOrdinalType.get( ImplicitEnumMapKey.VALUE_1 );
		assertEquals( ImplicitEnumMapKey.VALUE_1, foundValue.enumImplicitOverrideOrdinal );

		assertEquals( 0, ((Number) findDatabaseValue( foundValue, "enumImplicitOverrideOrdinal" )).intValue() );
		getSession().close();
	}

	@Test
	public void testEnumImplicitOverrideStringType() {
		MapValue mapValue = create();
		mapValue.enumImplicitOverrideString = ImplicitEnumMapKey.VALUE_2;
		mapValue.mapEntity.enumImplicitOverrideStringType.put( mapValue.enumImplicitOverrideString, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.enumImplicitOverrideStringType.size() );
		MapValue foundValue = found.enumImplicitOverrideStringType.get( ImplicitEnumMapKey.VALUE_2 );
		assertEquals( ImplicitEnumMapKey.VALUE_2, foundValue.enumImplicitOverrideString );

		assertEquals( "VALUE_2", findDatabaseValue( foundValue, "enumImplicitOverrideString" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitOverridedType() {
		MapValue mapValue = create();
		mapValue.enumImplicitOverrided = ImplicitEnumMapKey.VALUE_1;
		mapValue.mapEntity.enumImplicitOverridedType.put( mapValue.enumImplicitOverrided, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.enumImplicitOverridedType.size() );
		MapValue foundValue = found.enumImplicitOverridedType.get( ImplicitEnumMapKey.VALUE_1 );
		assertEquals( ImplicitEnumMapKey.VALUE_1, foundValue.enumImplicitOverrided );

		assertEquals( "O1", findDatabaseValue( foundValue, "enumImplicitOverrided" ) );
		getSession().close();
	}


	private MapValue create() {
		MapEntity mapEntity = new MapEntity();
		return new MapValue( mapEntity );
	}

	private MapEntity persist(MapEntity mapEntity) {
		Transaction tx = openSession().getTransaction();
		tx.begin();
		mapEntity = (MapEntity) getSession().merge( mapEntity );

		tx.commit();
		getSession().close();

		mapEntity = openSession().get( MapEntity.class, mapEntity.id );
		return mapEntity;
	}

	private Object findDatabaseValue(MapValue mapValue, String column) {
		return getSession()
				.createSQLQuery( "select mv." + column + " from map_value mv where mv.id=:id" )
				.setParameter( "id", mapValue.id )
				.uniqueResult();
	}

	@Entity
	@Table(name = "map_entity")
	public static class MapEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Integer id;

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "implicitType")
		private Map<ColorType, MapValue> implicitType = new HashMap<ColorType, MapValue>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "explicitType")
		private Map<ColorType, MapValue> explicitType = new HashMap<ColorType, MapValue>();

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumDefault")
		private Map<EnumMapKey, MapValue> enumDefaultType = new HashMap<EnumMapKey, MapValue>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumExplicitOrdinal")
		private Map<EnumMapKey, MapValue> enumExplicitOrdinalType = new HashMap<EnumMapKey, MapValue>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumExplicitString")
		private Map<EnumMapKey, MapValue> enumExplicitStringType = new HashMap<EnumMapKey, MapValue>();

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumExplicit")
		private Map<EnumMapKey, MapValue> enumExplicitType = new HashMap<EnumMapKey, MapValue>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicit")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitType = new HashMap<ImplicitEnumMapKey, MapValue>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicitOverrideOrdinal")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitOverrideOrdinalType = new HashMap<ImplicitEnumMapKey, MapValue>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicitOverrideString")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitOverrideStringType = new HashMap<ImplicitEnumMapKey, MapValue>();

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicitOverrided")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitOverridedType = new HashMap<ImplicitEnumMapKey, MapValue>();
	}

	@Entity
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
		private ImplicitEnumMapKey enumImplicitOverrideOrdinal;
		@Enumerated(EnumType.STRING)
		private ImplicitEnumMapKey enumImplicitOverrideString;

		@Convert(converter = ImplicitEnumMapKeyOverridedConverter.class)
		private ImplicitEnumMapKey enumImplicitOverrided;

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
	public static class ImplicitEnumMapKeyOverridedConverter implements AttributeConverter<ImplicitEnumMapKey, String> {
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
