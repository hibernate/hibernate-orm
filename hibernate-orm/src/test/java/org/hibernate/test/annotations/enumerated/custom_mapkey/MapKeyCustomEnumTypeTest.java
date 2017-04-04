/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.enumerated.custom_mapkey;

import java.io.Serializable;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.type.EnumType;
import org.hibernate.type.Type;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.annotations.enumerated.custom_types.FirstLetterType;
import org.hibernate.test.annotations.enumerated.custom_types.LastNumberType;
import org.hibernate.test.annotations.enumerated.enums.Common;
import org.hibernate.test.annotations.enumerated.enums.FirstLetter;
import org.hibernate.test.annotations.enumerated.enums.LastNumber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test as in EnumeratedTypeTest but with MapKey
 *
 * @author Janario Oliveira
 */
public class MapKeyCustomEnumTypeTest extends BaseNonConfigCoreFunctionalTestCase {
	private Type getMapKeyType(Property prop) {
		Value value = prop.getValue();
		assertEquals( Map.class, value.getClass() );

		Map map = (Map) value;
		return map.getIndex().getType();
	}

	private void assetTypeDefinition(
			Property property,
			Class expectedReturnedClass, Class expectedType) {
		Type type = getMapKeyType( property );
		assertEquals( expectedReturnedClass, type.getReturnedClass() );
		assertEquals( expectedType.getName(), type.getName() );
	}

	@Test
	public void testTypeDefinition() {
		PersistentClass pc = metadata().getEntityBinding( EntityMapEnum.class.getName() );

		// ordinal default of EnumType
		assetTypeDefinition( pc.getProperty( "ordinalMap" ), Common.class, EnumType.class );

		// string defined by Enumerated(STRING)
		assetTypeDefinition( pc.getProperty( "stringMap" ), Common.class, EnumType.class );

		// explicit defined by @Type
		assetTypeDefinition( pc.getProperty( "firstLetterMap" ), FirstLetter.class, FirstLetterType.class );

		// implicit defined by @TypeDef in somewhere
		assetTypeDefinition( pc.getProperty( "lastNumberMap" ), LastNumber.class, LastNumberType.class );

		// implicit defined by @TypeDef in anywhere, but overrided by Enumerated(STRING)
		assetTypeDefinition( pc.getProperty( "explicitOverridingImplicitMap" ), LastNumber.class, EnumType.class );
	}

	private void assetEntityMapEnumEquals(EntityMapEnum expected, EntityMapEnum found) {
		assertEquals( expected.id, found.id );
		assertEquals( expected.ordinalMap, found.ordinalMap );
		assertEquals( expected.stringMap, found.stringMap );
		assertEquals( expected.firstLetterMap, found.firstLetterMap );
		assertEquals( expected.lastNumberMap, found.lastNumberMap );
		assertEquals( expected.explicitOverridingImplicitMap, found.explicitOverridingImplicitMap );
	}

	private EntityMapEnum assertFindEntityMapEnum(
			EntityMapEnum expected,
			String query,
			String queryWithParam, Object param,
			String nativeQueryCheck) {

		assertNotEquals( 0, expected.id );
		assertNotNull( param );

		Session session = openNewSession();
		EntityMapEnum found = (EntityMapEnum) session.createQuery( query ).uniqueResult();

		//find
		assetEntityMapEnumEquals( expected, found );

		// find with parameter
		found = (EntityMapEnum) session.createQuery( queryWithParam )
				.setParameter( "param", param ).uniqueResult();
		assetEntityMapEnumEquals( expected, found );

		//native query check
		SQLQuery sqlQuery = session.createSQLQuery( nativeQueryCheck );
		sqlQuery.setParameter( "idEntityMapEnum", expected.id );
		Object o = sqlQuery.uniqueResult();
		assertNotNull( o );

		return found;
	}

	@Test
	public void testQuery() {
		// ordinal
		EntityMapEnum entityMapEnum = new EntityMapEnum();
		entityMapEnum.ordinalMap.put( Common.A2, "Common.A2" );
		Serializable id = save( entityMapEnum );

		EntityMapEnum found = assertFindEntityMapEnum(
				entityMapEnum, "from EntityMapEnum ee where key(ee.ordinalMap)=1",
				"from EntityMapEnum ee where key(ee.ordinalMap)=:param", Common.A2,
				"select 1 from EntityMapEnum_ordinalMap where EntityMapEnum_id=:idEntityMapEnum and ordinalMap_KEY=1"
		);
		assertFalse( found.ordinalMap.isEmpty() );
		delete( id );

		// **************
		// string
		entityMapEnum = new EntityMapEnum();
		entityMapEnum.stringMap.put( Common.B1, "Common.B2" );
		id = save( entityMapEnum );

		found = assertFindEntityMapEnum(
				entityMapEnum,
				"from EntityMapEnum ee where key(ee.stringMap)='B1'",
				"from EntityMapEnum ee where key(ee.stringMap)=:param",
				Common.B1,
				"select 1 from EntityMapEnum_stringMap where EntityMapEnum_id=:idEntityMapEnum and stringMap_KEY='B1'"
		);
		assertFalse( found.stringMap.isEmpty() );
		delete( id );

		// **************
		// custom local type (FirstLetterType)
		entityMapEnum = new EntityMapEnum();
		entityMapEnum.firstLetterMap.put( FirstLetter.C_LETTER, "FirstLetter.C_LETTER" );
		id = save( entityMapEnum );

		found = assertFindEntityMapEnum(
				entityMapEnum,
				"from EntityMapEnum ee where key(ee.firstLetterMap)='C'",
				"from EntityMapEnum ee where key(ee.firstLetterMap)=:param",
				FirstLetter.C_LETTER,
				"select 1 from EntityMapEnum_firstLetterMap where EntityMapEnum_id=:idEntityMapEnum and firstLetterMap_KEY='C'"
		);
		assertFalse( found.firstLetterMap.isEmpty() );
		delete( id );

		// **************
		// custom global type(LastNumberType)
		entityMapEnum = new EntityMapEnum();
		entityMapEnum.lastNumberMap.put( LastNumber.NUMBER_1, "LastNumber.NUMBER_1c" );
		id = save( entityMapEnum );

		found = assertFindEntityMapEnum(
				entityMapEnum,
				"from EntityMapEnum ee where key(ee.lastNumberMap)='1'",
				"from EntityMapEnum ee where key(ee.lastNumberMap)=:param",
				LastNumber.NUMBER_1,
				"select 1 from EntityMapEnum_lastNumberMap where EntityMapEnum_id=:idEntityMapEnum and lastNumberMap_KEY='1'"
		);
		assertFalse( found.lastNumberMap.isEmpty() );
		delete( id );

		// **************
		// override global type
		entityMapEnum = new EntityMapEnum();
		entityMapEnum.explicitOverridingImplicitMap.put(
				LastNumber.NUMBER_2, "LastNumber.NUMBER_2a"
		);
		id = save( entityMapEnum );

		found = assertFindEntityMapEnum(
				entityMapEnum,
				"from EntityMapEnum ee where key(ee.explicitOverridingImplicitMap)='NUMBER_2'",
				"from EntityMapEnum ee where key(ee.explicitOverridingImplicitMap)=:param",
				LastNumber.NUMBER_2,
				"select 1 from overridingMap where EntityMapEnum_id=:idEntityMapEnum and overridingMap_key='NUMBER_2'"
		);
		assertFalse( found.explicitOverridingImplicitMap.isEmpty() );
		delete( id );
	}

	private EntityMapEnum assertFindCriteria(
			EntityMapEnum expected,
			String mapPath, Object param) {
		assertNotEquals( 0, expected.id );

		Session session = openNewSession();
		session.beginTransaction();
		EntityMapEnum found = (EntityMapEnum) session.createCriteria( EntityMapEnum.class )
				.createCriteria( mapPath, "m" )
				.add( Restrictions.eq( "indices", param ) )
				.uniqueResult();
		//find
		assetEntityMapEnumEquals( expected, found );
		session.getTransaction().commit();
		session.close();
		return found;
	}

	@Test
	public void testCriteria() {
		// ordinal
		EntityMapEnum entityMapEnum = new EntityMapEnum();
		entityMapEnum.ordinalMap.put( Common.A1, "Common.A1" );
		Serializable id = save( entityMapEnum );

		EntityMapEnum found = assertFindCriteria(
				entityMapEnum,
				"ordinalMap", Common.A1
		);
		assertFalse( found.ordinalMap.isEmpty() );
		delete( id );

		// **************
		// string
		entityMapEnum = new EntityMapEnum();
		entityMapEnum.stringMap.put( Common.B2, "Common.B2" );
		id = save( entityMapEnum );

		found = assertFindCriteria(
				entityMapEnum,
				"stringMap", Common.B2
		);
		assertFalse( found.stringMap.isEmpty() );
		delete( id );

		// **************
		// custom local type (FirstLetterType)
		entityMapEnum = new EntityMapEnum();
		entityMapEnum.firstLetterMap.put( FirstLetter.A_LETTER, "FirstLetter.A_LETTER" );
		id = save( entityMapEnum );

		found = assertFindCriteria(
				entityMapEnum,
				"firstLetterMap", FirstLetter.A_LETTER
		);
		assertFalse( found.firstLetterMap.isEmpty() );
		delete( id );

		// **************
		// custom global type(LastNumberType)
		entityMapEnum = new EntityMapEnum();
		entityMapEnum.lastNumberMap.put( LastNumber.NUMBER_3, "LastNumber.NUMBER_3" );
		id = save( entityMapEnum );

		found = assertFindCriteria(
				entityMapEnum,
				"lastNumberMap", LastNumber.NUMBER_3
		);
		assertFalse( found.lastNumberMap.isEmpty() );
		delete( id );

		// **************
		// override global type
		entityMapEnum = new EntityMapEnum();
		entityMapEnum.explicitOverridingImplicitMap.put(
				LastNumber.NUMBER_2, "LastNumber.NUMBER_2b"
		);
		id = save( entityMapEnum );

		found = assertFindCriteria(
				entityMapEnum,
				"explicitOverridingImplicitMap", LastNumber.NUMBER_2
		);
		assertFalse( found.explicitOverridingImplicitMap.isEmpty() );
		delete( id );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {EntityMapEnum.class};
	}

	private Session openNewSession() {
		if ( getSession() != null && getSession().isOpen() ) {
			getSession().close();
		}
		return openSession();
	}

	private Serializable save(Object o) {
		Session session = openNewSession();
		session.getTransaction().begin();

		Serializable id = session.save( o );

		session.getTransaction().commit();
		session.close();

		return id;
	}

	private void delete(Serializable id) {
		Session session = openNewSession();
		session.getTransaction().begin();

		Object o = session.get( EntityMapEnum.class, id );
		session.delete( o );

		session.getTransaction().commit();
		session.close();
	}
}
