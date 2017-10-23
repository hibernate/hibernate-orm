/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.queryplan;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;

import org.hibernate.LockMode;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryCollectionReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests equals() and hashCode() for NativeSQLQueryReturn implementations.
 *
 * @author Gail Badner
 */
public class NativeSQLQueryReturnEqualsAndHashCodeTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {};
	}

	@Test
	public void testNativeSQLQueryScalarReturn() {
		NativeSQLQueryScalarReturn typeNoAlias = new NativeSQLQueryScalarReturn( null,sessionFactory().getTypeResolver().basic( "int" ) );
		NativeSQLQueryScalarReturn aliasNoType = new NativeSQLQueryScalarReturn( "abc", null );
		NativeSQLQueryScalarReturn aliasTypeInt = new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( "int" ) );
		NativeSQLQueryScalarReturn aliasTypeLong =  new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( "long" ) );
		NativeSQLQueryScalarReturn aliasTypeLongClass =  new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( Long.class.getName() ) );
		NativeSQLQueryScalarReturn aliasTypeString =  new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( "string" ) );
		NativeSQLQueryScalarReturn aliasTypeStringClass =  new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( String.class.getName() ) );

		check( false, typeNoAlias, aliasNoType );
		check( false, typeNoAlias, aliasTypeInt );
		check( false, typeNoAlias, aliasTypeLong );
		check( false, typeNoAlias, aliasTypeLongClass );
		check( false, typeNoAlias, aliasTypeString );
		check( false, typeNoAlias, aliasTypeStringClass );

		check( false, aliasNoType, aliasTypeInt );
		check( false, aliasNoType, aliasTypeLong );
		check( false, aliasNoType, aliasTypeLongClass );
		check( false, aliasNoType, aliasTypeString );
		check( false, aliasNoType, aliasTypeStringClass );

		check( false, aliasTypeInt, aliasTypeLong );
		check( false, aliasTypeInt, aliasTypeLongClass );
		check( false, aliasTypeInt, aliasTypeString );
		check( false, aliasTypeInt, aliasTypeStringClass );

		check( true, aliasTypeLong, aliasTypeLongClass );
		check( false, aliasTypeLong, aliasTypeString );
		check( false, aliasTypeLong, aliasTypeStringClass );

		check( false, aliasTypeLongClass, aliasTypeString );
		check( false, aliasTypeLongClass, aliasTypeStringClass );

		check( true, aliasTypeString, aliasTypeStringClass );

		check( true, typeNoAlias, new NativeSQLQueryScalarReturn( null,sessionFactory().getTypeResolver().basic( "int" ) ) );
		check( true, aliasNoType, new NativeSQLQueryScalarReturn( "abc", null ) );
		check( true, aliasTypeInt, new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( "int" ) ) );
		check( true, aliasTypeLong, new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( "long" ) ) );
		check( true, aliasTypeLongClass,  new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( Long.class.getName() ) ) );
		check( true, aliasTypeString, new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( "string" ) ) );
		check( true, aliasTypeStringClass, new NativeSQLQueryScalarReturn( "abc",sessionFactory().getTypeResolver().basic( String.class.getName() ) ) );
	}

	@Test
	public void testNativeSQLQueryRootReturn() {
		NativeSQLQueryRootReturn alias = new NativeSQLQueryRootReturn( "abc", null, null);
		NativeSQLQueryRootReturn diffAlias = new NativeSQLQueryRootReturn( "def", null, null);
		NativeSQLQueryRootReturn aliasEntityName = new NativeSQLQueryRootReturn( "abc", "Person", null);
		NativeSQLQueryRootReturn aliasDiffEntityName = new NativeSQLQueryRootReturn( "abc", "Customer", null);
		NativeSQLQueryRootReturn aliasEntityNameLockMode = new NativeSQLQueryRootReturn( "abc", "Person", LockMode.NONE );
		NativeSQLQueryRootReturn aliasEntityNameDiffLockMode = new NativeSQLQueryRootReturn( "abc", "Person", LockMode.OPTIMISTIC );

		check( false, alias, diffAlias );
		check( false, alias, aliasEntityName );
		check( false, alias, aliasDiffEntityName );
		check( false, alias, aliasEntityNameLockMode );
		check( false, alias, aliasEntityNameDiffLockMode );

		check( false, diffAlias, aliasEntityName );
		check( false, diffAlias, aliasDiffEntityName );
		check( false, diffAlias, aliasEntityNameLockMode );
		check( false, diffAlias, aliasEntityNameDiffLockMode );

		check( false, aliasEntityName, aliasDiffEntityName );
		check( false, aliasEntityName, aliasEntityNameLockMode );
		check( false, aliasEntityName, aliasEntityNameDiffLockMode );

		check( false, aliasDiffEntityName, aliasEntityNameLockMode );
		check( false, aliasDiffEntityName, aliasEntityNameDiffLockMode );

		check( false, aliasEntityNameLockMode, aliasEntityNameDiffLockMode );

		check( true, alias, new NativeSQLQueryRootReturn( "abc", null, null) );
		check( true, diffAlias, new NativeSQLQueryRootReturn( "def", null, null) );
		check( true, aliasEntityName, new NativeSQLQueryRootReturn( "abc", "Person", null) );
		check( true, aliasDiffEntityName, new NativeSQLQueryRootReturn( "abc", "Customer", null) );
		check( true, aliasEntityNameLockMode, new NativeSQLQueryRootReturn( "abc", "Person", LockMode.NONE ) );
		check( true, aliasEntityNameDiffLockMode, new NativeSQLQueryRootReturn( "abc", "Person", LockMode.OPTIMISTIC ) );
	}

	@Test
	public void testNativeSQLQueryJoinReturn() {
		NativeSQLQueryJoinReturn r1 = new NativeSQLQueryJoinReturn( "a", "b", "c", null, null);
		NativeSQLQueryJoinReturn r2 = new NativeSQLQueryJoinReturn( "a", "c", "b", null, null);
		NativeSQLQueryJoinReturn r3NullMap = new NativeSQLQueryJoinReturn( "b", "c", "a", null, null);
		NativeSQLQueryJoinReturn r3EmptyMap= new NativeSQLQueryJoinReturn( "b", "c", "a", new HashMap(), null);
		NativeSQLQueryJoinReturn r4 = new NativeSQLQueryJoinReturn( "b", "c", "a", Collections.singletonMap( "key", "value" ), null);
		NativeSQLQueryJoinReturn r5 = new NativeSQLQueryJoinReturn( "b", "c", "a", Collections.singletonMap( "otherkey", "othervalue" ), null);
		NativeSQLQueryJoinReturn r6 = new NativeSQLQueryJoinReturn( "b", "c", "a", Collections.singletonMap( "key", "value" ), LockMode.NONE );
		NativeSQLQueryJoinReturn r7 = new NativeSQLQueryJoinReturn( "b", "c", "a", null, LockMode.NONE );

		check( false, r1, r2 );
		check( false, r1, r3NullMap );
		check( false, r1, r3EmptyMap );
		check( false, r1, r4 );
		check( false, r1, r5 );
		check( false, r1, r6 );
		check( false, r1, r7 );

		check( false, r2, r3NullMap );
		check( false, r2, r3EmptyMap );
		check( false, r2, r4 );
		check( false, r2, r5 );
		check( false, r2, r6 );
		check( false, r2, r7 );

		check( true, r3NullMap, r3EmptyMap );
		check( false, r3NullMap, r4 );
		check( false, r3NullMap, r5 );
		check( false, r3NullMap, r6 );
		check( false, r3NullMap, r7 );

		check( false, r3EmptyMap, r4 );
		check( false, r3EmptyMap, r5 );
		check( false, r3EmptyMap, r6 );
		check( false, r3EmptyMap, r7 );

		check( false, r4, r5 );
		check( false, r4, r6 );
		check( false, r4, r7 );

		check( false, r5, r6 );
		check( false, r5, r7 );

		check( false, r6, r7 );

		check( true, r1, new NativeSQLQueryJoinReturn( "a", "b", "c", null, null) );
		check( true, r2, new NativeSQLQueryJoinReturn( "a", "c", "b", null, null) );
		check( true, r3NullMap, new NativeSQLQueryJoinReturn( "b", "c", "a", null, null) );
		check( true, r3EmptyMap, new NativeSQLQueryJoinReturn( "b", "c", "a", new HashMap(), null) );
		check( true, r4, new NativeSQLQueryJoinReturn( "b", "c", "a", Collections.singletonMap( "key", "value" ), null) );
		check( true, r5, new NativeSQLQueryJoinReturn( "b", "c", "a", Collections.singletonMap( "otherkey", "othervalue" ), null) );
		check( true, r6, new NativeSQLQueryJoinReturn( "b", "c", "a", Collections.singletonMap( "key", "value" ), LockMode.NONE ) );
		check( true, r7, new NativeSQLQueryJoinReturn( "b", "c", "a", null, LockMode.NONE ) );
	}

	@Test
	public void testNativeSQLQueryCollectionReturn() {
		NativeSQLQueryCollectionReturn r1 = new NativeSQLQueryCollectionReturn( "a", "b", "c", null, null);
		NativeSQLQueryCollectionReturn r2 = new NativeSQLQueryCollectionReturn( "a", "c", "b", null, null);
		NativeSQLQueryCollectionReturn r3NullMap = new NativeSQLQueryCollectionReturn( "b", "c", "a", null, null);
		NativeSQLQueryCollectionReturn r3EmptyMap= new NativeSQLQueryCollectionReturn( "b", "c", "a", new HashMap(), null);
		NativeSQLQueryCollectionReturn r4 = new NativeSQLQueryCollectionReturn( "b", "c", "a", Collections.singletonMap( "key", "value" ), null);
		NativeSQLQueryCollectionReturn r5 = new NativeSQLQueryCollectionReturn( "b", "c", "a", Collections.singletonMap( "otherkey", "othervalue" ), null);
		NativeSQLQueryCollectionReturn r6 = new NativeSQLQueryCollectionReturn( "b", "c", "a", Collections.singletonMap( "key", "value" ), LockMode.NONE );
		NativeSQLQueryCollectionReturn r7 = new NativeSQLQueryCollectionReturn( "b", "c", "a", null, LockMode.NONE );

		check( false, r1, r2 );
		check( false, r1, r3NullMap );
		check( false, r1, r3EmptyMap );
		check( false, r1, r4 );
		check( false, r1, r5 );
		check( false, r1, r6 );
		check( false, r1, r7 );

		check( false, r2, r3NullMap );
		check( false, r2, r3EmptyMap );
		check( false, r2, r4 );
		check( false, r2, r5 );
		check( false, r2, r6 );
		check( false, r2, r7 );

		check( true, r3NullMap, r3EmptyMap );
		check( false, r3NullMap, r4 );
		check( false, r3NullMap, r5 );
		check( false, r3NullMap, r6 );
		check( false, r3NullMap, r7 );

		check( false, r3EmptyMap, r4 );
		check( false, r3EmptyMap, r5 );
		check( false, r3EmptyMap, r6 );
		check( false, r3EmptyMap, r7 );

		check( false, r4, r5 );
		check( false, r4, r6 );
		check( false, r4, r7 );

		check( false, r5, r6 );
		check( false, r5, r7 );

		check( false, r6, r7 );

		check( true, r1, new NativeSQLQueryCollectionReturn( "a", "b", "c", null, null) );
		check( true, r2, new NativeSQLQueryCollectionReturn( "a", "c", "b", null, null) );
		check( true, r3NullMap, new NativeSQLQueryCollectionReturn( "b", "c", "a", null, null) );
		check( true, r3EmptyMap, new NativeSQLQueryCollectionReturn( "b", "c", "a", new HashMap(), null) );
		check( true, r4, new NativeSQLQueryCollectionReturn( "b", "c", "a", Collections.singletonMap( "key", "value" ), null) );
		check( true, r5, new NativeSQLQueryCollectionReturn( "b", "c", "a", Collections.singletonMap( "otherkey", "othervalue" ), null) );
		check( true, r6, new NativeSQLQueryCollectionReturn( "b", "c", "a", Collections.singletonMap( "key", "value" ), LockMode.NONE ) );
		check( true, r7, new NativeSQLQueryCollectionReturn( "b", "c", "a", null, LockMode.NONE ) );
	}

	@Test
	public void testNativeSQLQueryReturnTypes() {
		NativeSQLQueryScalarReturn r1 = new NativeSQLQueryScalarReturn( "a",sessionFactory().getTypeResolver().basic( "int" ) );
		NativeSQLQueryRootReturn r2 = new NativeSQLQueryRootReturn( "a", "b", LockMode.NONE );
		NativeSQLQueryJoinReturn r3 = new NativeSQLQueryJoinReturn( "a", "b", "c", Collections.singletonMap( "key", "value" ), LockMode.NONE );
		NativeSQLQueryCollectionReturn r4 = new NativeSQLQueryCollectionReturn( "a", "b", "c", Collections.singletonMap( "key", "value" ), LockMode.NONE);

		check( false, r1, r2 );
		check( false, r1, r3 );
		check( false, r1, r4 );

		check( false, r2, r3 );
		check( false, r2, r4 );

		check( false, r3, r4 );
	}

	private void check(boolean expectedEquals, NativeSQLQueryReturn queryReturn1, NativeSQLQueryReturn queryReturn2) {
		if ( expectedEquals ) {
			assertTrue( queryReturn1.equals( queryReturn2 ) );
			assertTrue( queryReturn2.equals( queryReturn1 ) );
			assertTrue( queryReturn1.hashCode() == queryReturn2.hashCode() );
		}
		else {
			assertFalse( queryReturn1.equals( queryReturn2 ) );
			assertFalse( queryReturn2.equals( queryReturn1 ) );
			assertFalse( queryReturn1.hashCode() == queryReturn2.hashCode() );
		}
	}
}
