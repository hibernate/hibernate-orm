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
public class MapKeyElementCollectionAttributeConverterTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				MapKeyTable.class,
				ColorTypeConverter.class,
				ImplicitEnumMapKeyConverter.class,
				ImplicitEnumMapKeyOverridedConverter.class
		};
	}


	private MapKeyTable persist(MapKeyTable mapEntity) {
		Transaction tx = openSession().getTransaction();
		tx.begin();
		mapEntity = (MapKeyTable) getSession().merge( mapEntity );

		tx.commit();
		getSession().close();

		mapEntity = openSession().get( MapKeyTable.class, mapEntity.id );
		return mapEntity;
	}

	private Object findDatabaseValue(MapKeyTable mapKey, String prop) {
		return getSession()
				.createSQLQuery( "select mk." + prop + "_KEY from map_key_table_" + prop + " mk where mk.map_key_table_id=:id" )
				.setParameter( "id", mapKey.id )
				.uniqueResult();
	}

	@Test
	public void testClassImplicitStringValue() {
		ColorType key = ColorType.RED;
		String value = "value-red";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.classImplicitStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.classImplicitStringValue.size() );
		assertEquals( value, mapKey.classImplicitStringValue.get( key ) );

		assertEquals( "red", findDatabaseValue( mapKey, "classImplicitStringValue" ) );
		getSession().close();
	}

	@Test
	public void testClassImplicitEnumValue() {
		ColorType key = ColorType.YELLOW;
		SomeEnum value = SomeEnum.B;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.classImplicitEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.classImplicitEnumValue.size() );
		assertEquals( value, mapKey.classImplicitEnumValue.get( key ) );

		assertEquals( "yellow", findDatabaseValue( mapKey, "classImplicitEnumValue" ) );
		getSession().close();
	}

	@Test
	public void testClassExplicitStringValue() {
		ColorType key = ColorType.BLUE;
		String value = "value";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.classExplicitStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.classExplicitStringValue.size() );
		assertEquals( value, mapKey.classExplicitStringValue.get( key ) );

		assertEquals( "COLOR-blue", findDatabaseValue( mapKey, "classExplicitStringValue" ) );
		getSession().close();
	}

	@Test
	public void testClassExplicitEnumValue() {
		ColorType key = ColorType.BLUE;
		SomeEnum value = SomeEnum.A;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.classExplicitEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.classExplicitEnumValue.size() );
		assertEquals( value, mapKey.classExplicitEnumValue.get( key ) );

		assertEquals( "COLOR-blue", findDatabaseValue( mapKey, "classExplicitEnumValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumDefaultStringValue() {
		EnumMapKey key = EnumMapKey.VALUE_1;
		String value = "value";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumDefaultStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumDefaultStringValue.size() );
		assertEquals( value, mapKey.enumDefaultStringValue.get( key ) );

		assertEquals( 0, findDatabaseValue( mapKey, "enumDefaultStringValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumDefaultEnumValue() {
		EnumMapKey key = EnumMapKey.VALUE_2;
		SomeEnum value = SomeEnum.A;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumDefaultEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumDefaultEnumValue.size() );
		assertEquals( value, mapKey.enumDefaultEnumValue.get( key ) );

		assertEquals( 1, findDatabaseValue( mapKey, "enumDefaultEnumValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitOrdinalStringValue() {
		EnumMapKey key = EnumMapKey.VALUE_2;
		String value = "value";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumExplicitOrdinalStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumExplicitOrdinalStringValue.size() );
		assertEquals( value, mapKey.enumExplicitOrdinalStringValue.get( key ) );

		assertEquals( 1, findDatabaseValue( mapKey, "enumExplicitOrdinalStringValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitOrdinalEnumValue() {
		EnumMapKey key = EnumMapKey.VALUE_1;
		SomeEnum value = SomeEnum.B;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumExplicitOrdinalEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumExplicitOrdinalEnumValue.size() );
		assertEquals( value, mapKey.enumExplicitOrdinalEnumValue.get( key ) );

		assertEquals( 0, findDatabaseValue( mapKey, "enumExplicitOrdinalEnumValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitStringStringValue() {
		EnumMapKey key = EnumMapKey.VALUE_1;
		String value = "value";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumExplicitStringStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumExplicitStringStringValue.size() );
		assertEquals( value, mapKey.enumExplicitStringStringValue.get( key ) );

		assertEquals( "VALUE_1", findDatabaseValue( mapKey, "enumExplicitStringStringValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitStringEnumValue() {
		EnumMapKey key = EnumMapKey.VALUE_2;
		SomeEnum value = SomeEnum.A;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumExplicitStringEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumExplicitStringEnumValue.size() );
		assertEquals( value, mapKey.enumExplicitStringEnumValue.get( key ) );

		assertEquals( "VALUE_2", findDatabaseValue( mapKey, "enumExplicitStringEnumValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitConverterStringValue() {
		EnumMapKey key = EnumMapKey.VALUE_1;
		String value = "value";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumExplicitConverterStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumExplicitConverterStringValue.size() );
		assertEquals( value, mapKey.enumExplicitConverterStringValue.get( key ) );

		assertEquals( "1", findDatabaseValue( mapKey, "enumExplicitConverterStringValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumExplicitConverterEnumValue() {
		EnumMapKey key = EnumMapKey.VALUE_2;
		SomeEnum value = SomeEnum.A;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumExplicitConverterEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumExplicitConverterEnumValue.size() );
		assertEquals( value, mapKey.enumExplicitConverterEnumValue.get( key ) );

		assertEquals( "2", findDatabaseValue( mapKey, "enumExplicitConverterEnumValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitStringValue() {
		ImplicitEnumMapKey key = ImplicitEnumMapKey.VALUE_1;
		String value = "value";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumImplicitStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumImplicitStringValue.size() );
		assertEquals( value, mapKey.enumImplicitStringValue.get( key ) );

		assertEquals( "I1", findDatabaseValue( mapKey, "enumImplicitStringValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitEnumValue() {
		ImplicitEnumMapKey key = ImplicitEnumMapKey.VALUE_2;
		SomeEnum value = SomeEnum.A;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumImplicitEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumImplicitEnumValue.size() );
		assertEquals( value, mapKey.enumImplicitEnumValue.get( key ) );

		assertEquals( "I2", findDatabaseValue( mapKey, "enumImplicitEnumValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitOrdinalStringValue() {
		ImplicitEnumMapKey key = ImplicitEnumMapKey.VALUE_1;
		String value = "value";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumImplicitOrdinalStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumImplicitOrdinalStringValue.size() );
		assertEquals( value, mapKey.enumImplicitOrdinalStringValue.get( key ) );

		assertEquals( 0, findDatabaseValue( mapKey, "enumImplicitOrdinalStringValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitOrdinalEnumValue() {
		ImplicitEnumMapKey key = ImplicitEnumMapKey.VALUE_2;
		SomeEnum value = SomeEnum.A;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumImplicitOrdinalEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumImplicitOrdinalEnumValue.size() );
		assertEquals( value, mapKey.enumImplicitOrdinalEnumValue.get( key ) );

		assertEquals( 1, findDatabaseValue( mapKey, "enumImplicitOrdinalEnumValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitStringStringValue() {
		ImplicitEnumMapKey key = ImplicitEnumMapKey.VALUE_1;
		String value = "value";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumImplicitStringStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumImplicitStringStringValue.size() );
		assertEquals( value, mapKey.enumImplicitStringStringValue.get( key ) );

		assertEquals( "VALUE_1", findDatabaseValue( mapKey, "enumImplicitStringStringValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitStringEnumValue() {
		ImplicitEnumMapKey key = ImplicitEnumMapKey.VALUE_2;
		SomeEnum value = SomeEnum.A;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumImplicitStringEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumImplicitStringEnumValue.size() );
		assertEquals( value, mapKey.enumImplicitStringEnumValue.get( key ) );

		assertEquals( "VALUE_2", findDatabaseValue( mapKey, "enumImplicitStringEnumValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitConverterStringValue() {
		ImplicitEnumMapKey key = ImplicitEnumMapKey.VALUE_1;
		String value = "value";

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumImplicitConverterStringValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumImplicitConverterStringValue.size() );
		assertEquals( value, mapKey.enumImplicitConverterStringValue.get( key ) );

		assertEquals( "O1", findDatabaseValue( mapKey, "enumImplicitConverterStringValue" ) );
		getSession().close();
	}

	@Test
	public void testEnumImplicitConverterEnumValue() {
		ImplicitEnumMapKey key = ImplicitEnumMapKey.VALUE_2;
		SomeEnum value = SomeEnum.A;

		MapKeyTable mapKey = new MapKeyTable();
		mapKey.enumImplicitConverterEnumValue.put( key, value );

		mapKey = persist( mapKey );

		assertEquals( 1, mapKey.enumImplicitConverterEnumValue.size() );
		assertEquals( value, mapKey.enumImplicitConverterEnumValue.get( key ) );

		assertEquals( "O2", findDatabaseValue( mapKey, "enumImplicitConverterEnumValue" ) );
		getSession().close();
	}

	@Entity
	@Table(name = "map_key_table")
	public static class MapKeyTable {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		@ElementCollection
		@CollectionTable
		private Map<ColorType, String> classImplicitStringValue = new HashMap<ColorType, String>();
		@ElementCollection
		@CollectionTable
		@Enumerated(EnumType.STRING)
		private Map<ColorType, SomeEnum> classImplicitEnumValue = new HashMap<ColorType, SomeEnum>();

		@ElementCollection
		@CollectionTable
		@Convert(converter = CustomColorTypeConverter.class, attributeName = "key")
		private Map<ColorType, String> classExplicitStringValue = new HashMap<ColorType, String>();
		@ElementCollection
		@CollectionTable
		@Convert(converter = CustomColorTypeConverter.class, attributeName = "key")
		@Enumerated(EnumType.STRING)
		private Map<ColorType, SomeEnum> classExplicitEnumValue = new HashMap<ColorType, SomeEnum>();

		@ElementCollection
		@CollectionTable
		private Map<EnumMapKey, String> enumDefaultStringValue = new HashMap<EnumMapKey, String>();
		@ElementCollection
		@CollectionTable
		@Enumerated(EnumType.STRING)
		private Map<EnumMapKey, SomeEnum> enumDefaultEnumValue = new HashMap<EnumMapKey, SomeEnum>();

		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated
		private Map<EnumMapKey, String> enumExplicitOrdinalStringValue = new HashMap<EnumMapKey, String>();
		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated
		@Enumerated(EnumType.STRING)
		private Map<EnumMapKey, SomeEnum> enumExplicitOrdinalEnumValue = new HashMap<EnumMapKey, SomeEnum>();

		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated(EnumType.STRING)
		private Map<EnumMapKey, String> enumExplicitStringStringValue = new HashMap<EnumMapKey, String>();
		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated(EnumType.STRING)
		@Enumerated(EnumType.STRING)
		private Map<EnumMapKey, SomeEnum> enumExplicitStringEnumValue = new HashMap<EnumMapKey, SomeEnum>();

		@ElementCollection
		@CollectionTable
		@Convert(converter = ExplicitEnumMapKeyConverter.class, attributeName = "key")
		private Map<EnumMapKey, String> enumExplicitConverterStringValue = new HashMap<EnumMapKey, String>();
		@ElementCollection
		@CollectionTable
		@Convert(converter = ExplicitEnumMapKeyConverter.class, attributeName = "key")
		@Enumerated(EnumType.STRING)
		private Map<EnumMapKey, SomeEnum> enumExplicitConverterEnumValue = new HashMap<EnumMapKey, SomeEnum>();

		@ElementCollection
		@CollectionTable
		private Map<ImplicitEnumMapKey, String> enumImplicitStringValue = new HashMap<ImplicitEnumMapKey, String>();
		@ElementCollection
		@CollectionTable
		@Enumerated(EnumType.STRING)
		private Map<ImplicitEnumMapKey, SomeEnum> enumImplicitEnumValue = new HashMap<ImplicitEnumMapKey, SomeEnum>();
		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated
		private Map<ImplicitEnumMapKey, String> enumImplicitOrdinalStringValue = new HashMap<ImplicitEnumMapKey, String>();
		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated
		@Enumerated(EnumType.STRING)
		private Map<ImplicitEnumMapKey, SomeEnum> enumImplicitOrdinalEnumValue = new HashMap<ImplicitEnumMapKey, SomeEnum>();

		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated(EnumType.STRING)
		private Map<ImplicitEnumMapKey, String> enumImplicitStringStringValue = new HashMap<ImplicitEnumMapKey, String>();
		@ElementCollection
		@CollectionTable
		@MapKeyEnumerated(EnumType.STRING)
		@Enumerated(EnumType.STRING)
		private Map<ImplicitEnumMapKey, SomeEnum> enumImplicitStringEnumValue = new HashMap<ImplicitEnumMapKey, SomeEnum>();

		@ElementCollection
		@CollectionTable
		@Convert(converter = ImplicitEnumMapKeyOverridedConverter.class, attributeName = "key")
		private Map<ImplicitEnumMapKey, String> enumImplicitConverterStringValue = new HashMap<ImplicitEnumMapKey, String>();
		@ElementCollection
		@CollectionTable
		@Convert(converter = ImplicitEnumMapKeyOverridedConverter.class, attributeName = "key")
		@Enumerated(EnumType.STRING)
		private Map<ImplicitEnumMapKey, SomeEnum> enumImplicitConverterEnumValue = new HashMap<ImplicitEnumMapKey, SomeEnum>();
	}
}
