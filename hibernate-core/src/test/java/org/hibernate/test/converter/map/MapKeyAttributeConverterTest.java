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

				MapKeyConversionTest.ColorTypeConverter.class,

				CustomColorTypeConverter.class,
				ImplicitEnumMapKeyConverter.class,
				ExplicitEnumMapKeyConverter.class,
				ImplicitEnumMapKeyOverridedConverter.class
		};
	}

	@Test
	public void testImplicitType() {
		MapValue mapValue = create();
		mapValue.implicitType = MapKeyConversionTest.ColorType.BLUE;
		mapValue.mapEntity.implicitType.put( mapValue.implicitType, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.implicitType.size() );
		MapValue foundValue = found.implicitType.get( MapKeyConversionTest.ColorType.BLUE );
		assertEquals( MapKeyConversionTest.ColorType.BLUE, foundValue.implicitType );

		assertEquals( "blue", findDatabaseValue( foundValue, "implicitType" ) );
		getSession().close();
	}

	@Test
	public void testExplicitType() {
		MapValue mapValue = create();
		mapValue.explicitType = MapKeyConversionTest.ColorType.RED;
		mapValue.mapEntity.explicitType.put( mapValue.explicitType, mapValue );

		MapEntity found = persist( mapValue.mapEntity );

		assertEquals( 1, found.explicitType.size() );
		MapValue foundValue = found.explicitType.get( MapKeyConversionTest.ColorType.RED );
		assertEquals( MapKeyConversionTest.ColorType.RED, foundValue.explicitType );

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

		assertEquals( 0, findDatabaseValue( foundValue, "enumDefault" ) );
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
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "implicitType")
		private Map<MapKeyConversionTest.ColorType, MapValue> implicitType = new HashMap<MapKeyConversionTest.ColorType, MapValue>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "explicitType")
		private Map<MapKeyConversionTest.ColorType, MapValue> explicitType = new HashMap<MapKeyConversionTest.ColorType, MapValue>();

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumDefault")
		private Map<EnumMapKey, MapValue> enumDefaultType = new HashMap<EnumMapKey, MapValue>();

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumExplicit")
		private Map<EnumMapKey, MapValue> enumExplicitType = new HashMap<EnumMapKey, MapValue>();
		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicit")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitType = new HashMap<ImplicitEnumMapKey, MapValue>();

		@OneToMany(mappedBy = "mapEntity", cascade = CascadeType.ALL)
		@MapKey(name = "enumImplicitOverrided")
		private Map<ImplicitEnumMapKey, MapValue> enumImplicitOverridedType = new HashMap<ImplicitEnumMapKey, MapValue>();
	}

	@Entity
	@Table(name = "map_value")
	public static class MapValue {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;
		@ManyToOne
		@JoinColumn(name = "map_entity_id")
		private MapEntity mapEntity;

		private MapKeyConversionTest.ColorType implicitType;
		@Convert(converter = CustomColorTypeConverter.class)
		private MapKeyConversionTest.ColorType explicitType;

		private EnumMapKey enumDefault;
		@Convert(converter = ExplicitEnumMapKeyConverter.class)
		private EnumMapKey enumExplicit;
		private ImplicitEnumMapKey enumImplicit;

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
	public static class CustomColorTypeConverter implements AttributeConverter<MapKeyConversionTest.ColorType, String> {
		@Override
		public String convertToDatabaseColumn(MapKeyConversionTest.ColorType attribute) {
			return attribute == null ? null : "COLOR-" + attribute.toExternalForm();
		}

		@Override
		public MapKeyConversionTest.ColorType convertToEntityAttribute(String dbData) {
			return dbData == null ? null : MapKeyConversionTest.ColorType.fromExternalForm( dbData.substring( 6 ) );
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
