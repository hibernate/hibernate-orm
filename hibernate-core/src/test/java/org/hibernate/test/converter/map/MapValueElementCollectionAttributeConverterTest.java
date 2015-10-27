package org.hibernate.test.converter.map;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.Table;

import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Janario Oliveira
 */
public class MapValueElementCollectionAttributeConverterTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				MapValueTable.class,
				ColorTypeConverter.class,
				ImplicitEnumMapKeyConverter.class,
				ImplicitEnumMapKeyOverridedConverter.class
		};
	}

	private MapValueTable persist(MapValueTable mapEntity) {
		Transaction tx = openSession().getTransaction();
		tx.begin();
		mapEntity = (MapValueTable) getSession().merge( mapEntity );

		tx.commit();
		getSession().close();

		mapEntity = openSession().get( MapValueTable.class, mapEntity.id );
		return mapEntity;
	}

	private Object findDatabaseValue(MapValueTable mapKey, String prop) {
		return getSession()
				.createSQLQuery( "select mk." + prop + " from map_value_table_" + prop + " mk where mk.map_value_table_id=:id" )
				.setParameter( "id", mapKey.id )
				.uniqueResult();
	}

	@Test
	public void testKeyClassImplicit() {
		ColorType value = ColorType.RED;
		String key = "value-red";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyImplicitClass.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyImplicitClass.size() );
		assertEquals( value, mapValue.stringKeyImplicitClass.get( key ) );

		assertEquals( "red", findDatabaseValue( mapValue, "stringKeyImplicitClass" ) );
		getSession().close();
	}

	@Test
	public void testKeyClassImplicitEnumValue() {
		ColorType value = ColorType.YELLOW;
		SomeEnum key = SomeEnum.B;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyImplicitClass.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyImplicitClass.size() );
		assertEquals( value, mapValue.enumKeyImplicitClass.get( key ) );

		assertEquals( "yellow", findDatabaseValue( mapValue, "enumKeyImplicitClass" ) );
		getSession().close();
	}

	@Test
	public void testClassExplicitStringValue() {
		ColorType value = ColorType.BLUE;
		String key = "value";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyExplicitClass.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyExplicitClass.size() );
		assertEquals( value, mapValue.stringKeyExplicitClass.get( key ) );

		assertEquals( "COLOR-blue", findDatabaseValue( mapValue, "stringKeyExplicitClass" ) );
		getSession().close();
	}

	@Test
	public void testClassExplicitEnumValue() {
		ColorType value = ColorType.BLUE;
		SomeEnum key = SomeEnum.A;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyExplicitClass.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyExplicitClass.size() );
		assertEquals( value, mapValue.enumKeyExplicitClass.get( key ) );

		assertEquals( "COLOR-blue", findDatabaseValue( mapValue, "enumKeyExplicitClass" ) );
		getSession().close();
	}

	@Test
	public void testEnumDefaultStringValue() {
		EnumMapKey value = EnumMapKey.VALUE_1;
		String key = "value";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyDefaultEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyDefaultEnum.size() );
		assertEquals( value, mapValue.stringKeyDefaultEnum.get( key ) );

		assertEquals( 0, findDatabaseValue( mapValue, "stringKeyDefaultEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumDefaultEnumValue() {
		EnumMapKey value = EnumMapKey.VALUE_2;
		SomeEnum key = SomeEnum.A;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyDefaultEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyDefaultEnum.size() );
		assertEquals( value, mapValue.enumKeyDefaultEnum.get( key ) );

		assertEquals( 1, findDatabaseValue( mapValue, "enumKeyDefaultEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitOrdinalStringValue() {
		EnumMapKey value = EnumMapKey.VALUE_2;
		String key = "value";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyExplicitOrdinalEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyExplicitOrdinalEnum.size() );
		assertEquals( value, mapValue.stringKeyExplicitOrdinalEnum.get( key ) );

		assertEquals( 1, findDatabaseValue( mapValue, "stringKeyExplicitOrdinalEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitOrdinalEnumValue() {
		EnumMapKey value = EnumMapKey.VALUE_1;
		SomeEnum key = SomeEnum.B;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyExplicitOrdinalEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyExplicitOrdinalEnum.size() );
		assertEquals( value, mapValue.enumKeyExplicitOrdinalEnum.get( key ) );

		assertEquals( 0, findDatabaseValue( mapValue, "enumKeyExplicitOrdinalEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitStringStringValue() {
		EnumMapKey value = EnumMapKey.VALUE_1;
		String key = "value";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyExplicitStringEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyExplicitStringEnum.size() );
		assertEquals( value, mapValue.stringKeyExplicitStringEnum.get( key ) );

		assertEquals( "VALUE_1", findDatabaseValue( mapValue, "stringKeyExplicitStringEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitStringEnumValue() {
		EnumMapKey value = EnumMapKey.VALUE_2;
		SomeEnum key = SomeEnum.A;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyExplicitStringEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyExplicitStringEnum.size() );
		assertEquals( value, mapValue.enumKeyExplicitStringEnum.get( key ) );

		assertEquals( "VALUE_2", findDatabaseValue( mapValue, "enumKeyExplicitStringEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitConverterStringValue() {
		EnumMapKey value = EnumMapKey.VALUE_1;
		String key = "value";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyExplicitConverterEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyExplicitConverterEnum.size() );
		assertEquals( value, mapValue.stringKeyExplicitConverterEnum.get( key ) );

		assertEquals( "1", findDatabaseValue( mapValue, "stringKeyExplicitConverterEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitConverterEnumValue() {
		EnumMapKey value = EnumMapKey.VALUE_2;
		SomeEnum key = SomeEnum.A;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyExplicitConverterEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyExplicitConverterEnum.size() );
		assertEquals( value, mapValue.enumKeyExplicitConverterEnum.get( key ) );

		assertEquals( "2", findDatabaseValue( mapValue, "enumKeyExplicitConverterEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitStringValue() {
		ImplicitEnumMapKey value = ImplicitEnumMapKey.VALUE_1;
		String key = "value";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyImplicitEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyImplicitEnum.size() );
		assertEquals( value, mapValue.stringKeyImplicitEnum.get( key ) );

		assertEquals( "I1", findDatabaseValue( mapValue, "stringKeyImplicitEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitEnumValue() {
		ImplicitEnumMapKey value = ImplicitEnumMapKey.VALUE_2;
		SomeEnum key = SomeEnum.A;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyImplicitEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyImplicitEnum.size() );
		assertEquals( value, mapValue.enumKeyImplicitEnum.get( key ) );

		assertEquals( "I2", findDatabaseValue( mapValue, "enumKeyImplicitEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitOrdinalStringValue() {
		ImplicitEnumMapKey value = ImplicitEnumMapKey.VALUE_1;
		String key = "value";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyImplicitOrdinalEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyImplicitOrdinalEnum.size() );
		assertEquals( value, mapValue.stringKeyImplicitOrdinalEnum.get( key ) );

		assertEquals( 0, findDatabaseValue( mapValue, "stringKeyImplicitOrdinalEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitOrdinalEnumValue() {
		ImplicitEnumMapKey value = ImplicitEnumMapKey.VALUE_2;
		SomeEnum key = SomeEnum.A;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyImplicitOrdinalEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyImplicitOrdinalEnum.size() );
		assertEquals( value, mapValue.enumKeyImplicitOrdinalEnum.get( key ) );

		assertEquals( 1, findDatabaseValue( mapValue, "enumKeyImplicitOrdinalEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitStringStringValue() {
		ImplicitEnumMapKey value = ImplicitEnumMapKey.VALUE_1;
		String key = "value";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyImplicitStringEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyImplicitStringEnum.size() );
		assertEquals( value, mapValue.stringKeyImplicitStringEnum.get( key ) );

		assertEquals( "VALUE_1", findDatabaseValue( mapValue, "stringKeyImplicitStringEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitStringEnumValue() {
		ImplicitEnumMapKey value = ImplicitEnumMapKey.VALUE_2;
		SomeEnum key = SomeEnum.A;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyImplicitStringEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyImplicitStringEnum.size() );
		assertEquals( value, mapValue.enumKeyImplicitStringEnum.get( key ) );

		assertEquals( "VALUE_2", findDatabaseValue( mapValue, "enumKeyImplicitStringEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitConverterStringValue() {
		ImplicitEnumMapKey value = ImplicitEnumMapKey.VALUE_1;
		String key = "value";

		MapValueTable mapValue = new MapValueTable();
		mapValue.stringKeyImplicitConverterEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.stringKeyImplicitConverterEnum.size() );
		assertEquals( value, mapValue.stringKeyImplicitConverterEnum.get( key ) );

		assertEquals( "O1", findDatabaseValue( mapValue, "stringKeyImplicitConverterEnum" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitConverterEnumValue() {
		ImplicitEnumMapKey value = ImplicitEnumMapKey.VALUE_2;
		SomeEnum key = SomeEnum.A;

		MapValueTable mapValue = new MapValueTable();
		mapValue.enumKeyImplicitConverterEnum.put( key, value );

		mapValue = persist( mapValue );

		assertEquals( 1, mapValue.enumKeyImplicitConverterEnum.size() );
		assertEquals( value, mapValue.enumKeyImplicitConverterEnum.get( key ) );

		assertEquals( "O2", findDatabaseValue( mapValue, "enumKeyImplicitConverterEnum" ) );
		getSession().close();
	}

	@Entity
	@Table(name = "map_value_table")
	public static class MapValueTable {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		@ElementCollection
		@CollectionTable
		private Map<String, ColorType> stringKeyImplicitClass = new HashMap<String, ColorType>();
		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, ColorType> enumKeyImplicitClass = new HashMap<SomeEnum, ColorType>();

		@ElementCollection
		@CollectionTable
		@Convert(converter = CustomColorTypeConverter.class, attributeName = "value")
		private Map<String, ColorType> stringKeyExplicitClass = new HashMap<String, ColorType>();
		@ElementCollection
		@CollectionTable
		@Convert(converter = CustomColorTypeConverter.class, attributeName = "value")
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, ColorType> enumKeyExplicitClass = new HashMap<SomeEnum, ColorType>();

		@ElementCollection
		@CollectionTable
		private Map<String, EnumMapKey> stringKeyDefaultEnum = new HashMap<String, EnumMapKey>();
		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, EnumMapKey> enumKeyDefaultEnum = new HashMap<SomeEnum, EnumMapKey>();

		@ElementCollection
		@CollectionTable
		@Enumerated
		private Map<String, EnumMapKey> stringKeyExplicitOrdinalEnum = new HashMap<String, EnumMapKey>();
		@ElementCollection
		@CollectionTable
		@Enumerated
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, EnumMapKey> enumKeyExplicitOrdinalEnum = new HashMap<SomeEnum, EnumMapKey>();

		@ElementCollection
		@CollectionTable
		@Enumerated(EnumType.STRING)
		private Map<String, EnumMapKey> stringKeyExplicitStringEnum = new HashMap<String, EnumMapKey>();
		@ElementCollection
		@CollectionTable
		@Enumerated(EnumType.STRING)
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, EnumMapKey> enumKeyExplicitStringEnum = new HashMap<SomeEnum, EnumMapKey>();

		@ElementCollection
		@CollectionTable
		@Convert(converter = ExplicitEnumMapKeyConverter.class, attributeName = "value")
		private Map<String, EnumMapKey> stringKeyExplicitConverterEnum = new HashMap<String, EnumMapKey>();
		@ElementCollection
		@CollectionTable
		@Convert(converter = ExplicitEnumMapKeyConverter.class, attributeName = "value")
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, EnumMapKey> enumKeyExplicitConverterEnum = new HashMap<SomeEnum, EnumMapKey>();

		@ElementCollection
		@CollectionTable
		private Map<String, ImplicitEnumMapKey> stringKeyImplicitEnum = new HashMap<String, ImplicitEnumMapKey>();
		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, ImplicitEnumMapKey> enumKeyImplicitEnum = new HashMap<SomeEnum, ImplicitEnumMapKey>();
		@ElementCollection
		@CollectionTable
		@Enumerated
		private Map<String, ImplicitEnumMapKey> stringKeyImplicitOrdinalEnum = new HashMap<String, ImplicitEnumMapKey>();
		@ElementCollection
		@CollectionTable
		@Enumerated
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, ImplicitEnumMapKey> enumKeyImplicitOrdinalEnum = new HashMap<SomeEnum, ImplicitEnumMapKey>();

		@ElementCollection
		@CollectionTable
		@Enumerated(EnumType.STRING)
		private Map<String, ImplicitEnumMapKey> stringKeyImplicitStringEnum = new HashMap<String, ImplicitEnumMapKey>();
		@ElementCollection
		@CollectionTable
		@Enumerated(EnumType.STRING)
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, ImplicitEnumMapKey> enumKeyImplicitStringEnum = new HashMap<SomeEnum, ImplicitEnumMapKey>();

		@ElementCollection
		@CollectionTable
		@Convert(converter = ImplicitEnumMapKeyOverridedConverter.class, attributeName = "value")
		private Map<String, ImplicitEnumMapKey> stringKeyImplicitConverterEnum = new HashMap<String, ImplicitEnumMapKey>();
		@ElementCollection
		@CollectionTable
		@Convert(converter = ImplicitEnumMapKeyOverridedConverter.class, attributeName = "value")
		@MapKeyEnumerated(EnumType.STRING)
		private Map<SomeEnum, ImplicitEnumMapKey> enumKeyImplicitConverterEnum = new HashMap<SomeEnum, ImplicitEnumMapKey>();
	}

}
