/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.hibernate.orm.test.hql.Address;
import org.hibernate.orm.test.hql.Human;
import org.hibernate.orm.test.hql.Mammal;
import org.hibernate.orm.test.hql.Name;
import org.hibernate.orm.test.hql.StateProvince;
import org.hibernate.orm.test.hql.Zoo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests AST parser processing of ORDER BY clauses.
 *
 * @author Gail Badner
 */
public class OrderByTests extends BaseCoreFunctionalTestCase {
	StateProvince stateProvince;
	private Zoo zoo1;
	private Zoo zoo2;
	private Zoo zoo3;
	private Zoo zoo4;
	Set<Zoo> zoosWithSameName;
	Set<Zoo> zoosWithSameAddress;
	Mammal zoo1Mammal1;
	Mammal zoo1Mammal2;
	Human zoo2Director1;
	Human zoo2Director2;

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"hql/Animal.hbm.xml",
		};
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, false );
		cfg.setProperty( Environment.GENERATE_STATISTICS, true );
	}

	private void createData() {
		stateProvince = new StateProvince();
		stateProvince.setName( "IL" );

		zoo1 = new Zoo();
		zoo1.setName( "Zoo" );
		Address address1 = new Address();
		address1.setStreet( "1313 Mockingbird Lane" );
		address1.setCity( "Anywhere" );
		address1.setStateProvince( stateProvince );
		address1.setPostalCode( "12345" );
		address1.setCountry( "USA" );
		zoo1.setAddress( address1 );
		zoo1Mammal1 = new Mammal();
		zoo1Mammal1.setDescription( "zoo1Mammal1" );
		zoo1Mammal1.setZoo( zoo1 );
		zoo1.getMammals().put( "type1", zoo1Mammal1);
		zoo1Mammal2 = new Mammal();
		zoo1Mammal2.setDescription( "zoo1Mammal2" );
		zoo1Mammal2.setZoo( zoo1 );
		zoo1.getMammals().put( "type1", zoo1Mammal2);

		zoo2 = new Zoo();
		zoo2.setName( "A Zoo" );
		Address address2 = new Address();
		address2.setStreet( "1313 Mockingbird Lane" );
		address2.setCity( "Anywhere" );
		address2.setPostalCode( "12345" );
		address2.setStateProvince( stateProvince );
		address2.setCountry( "USA" );
		zoo2.setAddress( address2 );
		zoo2Director1 = new Human();
		zoo2Director1.setName( new Name( "Duh", 'A', "Man" ) );
		zoo2Director2 = new Human();
		zoo2Director2.setName( new Name( "Fat", 'A', "Cat" ) );
		zoo2.getDirectors().put( "Head Honcho", zoo2Director1 );
		zoo2.getDirectors().put( "Asst. Head Honcho", zoo2Director2 );

		zoo3 = new Zoo();
		zoo3.setName( "Zoo" );
		Address address3 = new Address();
		address3.setStreet( "1312 Mockingbird Lane" );
		address3.setCity( "Anywhere" );
		address3.setStateProvince( stateProvince );
		address3.setPostalCode( "12345" );
		address3.setCountry( "USA" );
		zoo3.setAddress( address3 );

		zoo4 = new Zoo();
		zoo4.setName( "Duh Zoo" );
		Address address4 = new Address();
		address4.setStreet( "1312 Mockingbird Lane" );
		address4.setCity( "Nowhere" );
		address4.setStateProvince( stateProvince );
		address4.setPostalCode( "12345" );
		address4.setCountry( "USA" );
		zoo4.setAddress( address4 );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( stateProvince );
		s.persist( zoo1Mammal1 );
		s.persist( zoo1Mammal2 );
		s.persist( zoo1 );
		s.persist( zoo2Director1 );
		s.persist( zoo2Director2 );
		s.persist( zoo2 );
		s.persist( zoo3 );
		s.persist( zoo4 );
		t.commit();
		s.close();

		zoosWithSameName = new HashSet<Zoo>( 2 );
		zoosWithSameName.add( zoo1 );
		zoosWithSameName.add( zoo3 );
		zoosWithSameAddress = new HashSet<Zoo>( 2 );
		zoosWithSameAddress.add( zoo1 );
		zoosWithSameAddress.add( zoo2 );
	}

	private void cleanupData() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		if ( zoo1 != null ) {
			s.remove( zoo1 );
			zoo1 = null;
		}
		if ( zoo2 != null ) {
			s.remove( zoo2 );
			zoo2 = null;
		}
		if ( zoo3 != null ) {
			s.remove( zoo3 );
			zoo3 = null;
		}
		if ( zoo4 != null ) {
			s.remove( zoo4 );
			zoo4 = null;
		}
		if ( zoo1Mammal1 != null ) {
			s.remove( zoo1Mammal1 );
			zoo1Mammal1 = null;
		}
		if ( zoo1Mammal2 != null ) {
			s.remove( zoo1Mammal2 );
			zoo1Mammal2 = null;
		}
		if ( zoo2Director1 != null ) {
			s.remove( zoo2Director1 );
			zoo2Director1 = null;
		}
		if ( zoo2Director2 != null ) {
			s.remove( zoo2Director2 );
			zoo2Director2 = null;
		}
		if ( stateProvince != null ) {
			s.remove( stateProvince );
		}
		t.commit();
		s.close();
	}

	@Test
	public void testOrderByOnJoinedSubclassPropertyWhoseColumnIsNotInDrivingTable() {
		// this is simply a syntax check
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( "from Human h order by h.bodyWeight" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testOrderByNoSelectAliasRef() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by name, address:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by name, address",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.name, z.address",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z2.name, z2.address from Zoo z2 where z2.name in ( select name from Zoo ) order by z2.name, z2.address",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		// using ASC
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by name ASC, address ASC",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.name ASC, z.address ASC",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z2.name, z2.address from Zoo z2 where z2.name in ( select name from Zoo ) order by z2.name ASC, z2.address ASC",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);

		// ordered by address, name:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA

		// NOTE (6.0) : the above illustration is based on 5.x code.  but the outcome
		// 		is ultimately based on the order in which we render the attributes of
		//		the Address component.  In 6.0 this has been made consistent and easily
		//		explainable, whereas that was not previously the case.
//		checkTestOrderByResults(
//				s.createQuery(
//						"select z.name, z.address from Zoo z order by z.address, z.name"
//				).list(),
//				zoo3, zoo4, zoo2, zoo1, null
//		);

		// NOTE (6.0) - continued : this is functionally equivalent to the 5.x case
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address " +
								"from Zoo z " +
								"order by z.address.street," +
								"    z.address.city," +
								"    z.address.postalCode, " +
								"    z.address.country," +
								"    z.name",
						Object[].class
				).list(),
				zoo3, zoo4, zoo2, zoo1, null
		);

		// NOTE (6.0) - continued 2 : this is the 6.x functionally
		// ordered by address, name:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.address, z.name",
						Object[].class
				).list(),
				zoo3, zoo2, zoo1, zoo4, null
		);

		// NOTE (6.0) - continued 3 : and the functionally equiv "full ordering"
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.address.city, z.address.country, z.address.stateProvince, z.address.street, z.name",
						Object[].class
				).list(),
				zoo3, zoo2, zoo1, zoo4, null
		);

		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by address, name",
						Object[].class
				).list(),
				zoo3, zoo2, zoo1, zoo4, null
		);

		// ordered by address:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		// unordered:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//
		// NOTE 6.0 : these results are not well defined.  Because we primarily order
		//		based on city zoo1, zoo2 and zoo3 are all sorted "above" zoo4.  however
		// 		the order between zoo1 and zoo2 is completely undefined because they
		//		have the same address
		//
		//		and honestly, not even sure what this assertion is even trying to test.
		//		but changed the query to what you'd need to get those results - which I guess
		//		is the order used by 5.x
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.address.street, z.address.city",
						Object[].class
				).list(),
				zoo3, zoo4, null, null, zoosWithSameAddress
		);
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by address.street, address.city",
						Object[].class
				).list(),
				zoo3, zoo4, null, null, zoosWithSameAddress
		);

		// ordered by name:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		// unordered:
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.name",
						Object[].class
				).list(),
				zoo2, zoo4, null, null, zoosWithSameName
		);
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by name",
						Object[].class
				).list(),
				zoo2, zoo4, null, null, zoosWithSameName
		);
		t.commit();
		s.close();

		cleanupData();
	}

	@Test
	public void testOrderByComponentDescNoSelectAliasRef() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by address DESC, name DESC:
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.address DESC, z.name DESC",
						Object[].class
				).list(),
				zoo4, zoo1, zoo2, zoo3, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by address DESC, name DESC",
						Object[].class
				).list(),
				zoo4, zoo1, zoo2, zoo3, null
		);
		t.commit();
		s.close();
		cleanupData();
	}

	@Test
	public void testOrderBySelectAliasRef() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by name, address:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z2.name as zname, z2.address as zooAddress from Zoo z2 where z2.name in ( select name from Zoo ) order by zname, zooAddress",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as name, z.address as address from Zoo z order by name, address",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooName, zooAddress",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by z.name, name",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by z.name, name",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		// using ASC
		checkTestOrderByResults(
				s.createQuery(
						"select z2.name as zname, z2.address as zooAddress from Zoo z2 where z2.name in ( select name from Zoo ) order by zname ASC, zooAddress ASC",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as name, z.address as address from Zoo z order by name ASC, address ASC",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooName ASC, zooAddress ASC",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by z.name ASC, name ASC",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by z.name ASC, name ASC",
						Object[].class
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);

		// ordered by address, name:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//
		// NOTE (6.0) : another case of different handling for the component attributes
		//		causing a "failure"
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as address, z.address as name from Zoo z order by name, address",
						Object[].class
				).list(),
				zoo3, zoo2, zoo1, zoo4, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by name, z.name",
						Object[].class
				).list(),
				zoo3, zoo2, zoo1, zoo4, null
		);
		// using ASC
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as address, z.address as name from Zoo z order by name ASC, address ASC",
						Object[].class
				).list(),
				zoo3, zoo2, zoo1, zoo4, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by name ASC, z.name ASC",
						Object[].class
				).list(),
				zoo3, zoo2, zoo1, zoo4, null
		);

		// ordered by address:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		// unordered:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//
		// NOTE (6.0) : another one where the result order would be undefined
//		checkTestOrderByResults(
//				s.createQuery(
//						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooAddress"
//				).list(),
//				zoo3, zoo4, null, null, zoosWithSameAddress
//		);

		// NOTE (6.0) : another undefined case
//		checkTestOrderByResults(
//				s.createQuery(
//						"select z.name as zooName, z.address as name from Zoo z order by name"
//				).list(),
//				zoo3, zoo4, null, null, zoosWithSameAddress
//		);

		// ordered by name:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		// unordered:
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooName",
						Object[].class
				).list(),
				zoo2, zoo4, null, null, zoosWithSameName
		);

		checkTestOrderByResults(
				s.createQuery(
						"select z.name as address, z.address as name from Zoo z order by address",
						Object[].class
				).list(),
				zoo2, zoo4, null, null, zoosWithSameName
		);
		t.commit();
		s.close();

		cleanupData();
	}

	@Test
	public void testOrderByComponentDescSelectAliasRef() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by address desc, name desc:
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		// using DESC
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooAddress DESC, zooName DESC",
						Object[].class
				).list(),
				zoo4, zoo1, zoo2, zoo3, null
		);

		t.commit();
		s.close();

		cleanupData();
	}

	@Test
	public void testOrderByEntityWithFetchJoinedCollection() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by address desc, name desc:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		// using DESC
		List<Zoo> list = s.createQuery( "from Zoo z join fetch z.mammals", Zoo.class ).list();

		t.commit();
		s.close();

		cleanupData();
	}

	@Test
	public void testOrderBySelectNewArgAliasRef() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by name, address:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		List<Zoo> list =
				s.createQuery(
						"select new Zoo( z.name as zname, z.address as zaddress) from Zoo z order by zname, zaddress",
						Zoo.class
				).list();
		assertEquals( 4, list.size() );
		assertEquals( zoo2, list.get( 0 ) );
		assertEquals( zoo4, list.get( 1 ) );
		assertEquals( zoo3, list.get( 2 ) );
		assertEquals( zoo1, list.get( 3 ) );

		// ordered by address, name:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		list =
				s.createQuery(
						"select new Zoo( z.name as zname, z.address as zaddress) from Zoo z order by zaddress, zname",
						Zoo.class
				).list();
		assertEquals( 4, list.size() );
		assertEquals( zoo3, list.get( 0 ) );
		assertEquals( zoo2, list.get( 1 ) );
		assertEquals( zoo1, list.get( 2 ) );
		assertEquals( zoo4, list.get( 3 ) );


		t.commit();
		s.close();

		cleanupData();
	}

	@Test(timeout = 5 * 60 * 1000)
	public void testOrderBySelectNewMapArgAliasRef() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by name, address:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		List<Map> list =
				s.createQuery(
						"select new map( z.name as zname, z.address as zaddress ) from Zoo z left join z.mammals m order by zname, zaddress",
						Map.class
				).list();
		assertEquals( 4, list.size() );
		assertEquals( zoo2.getName(), list.get( 0 ).get( "zname" ) );
		assertEquals( zoo2.getAddress(), list.get( 0 ).get( "zaddress" ) );
		assertEquals( zoo4.getName(), list.get( 1 ).get( "zname" ) );
		assertEquals( zoo4.getAddress(), list.get( 1 ).get( "zaddress" ) );
		assertEquals( zoo3.getName(), list.get( 2 ).get( "zname" ) );
		assertEquals( zoo3.getAddress(), list.get( 2 ).get( "zaddress" ) );
		assertEquals( zoo1.getName(), list.get( 3 ).get( "zname" ) );
		assertEquals( zoo1.getAddress(), list.get( 3 ).get( "zaddress" ) );

		// ordered by address, name:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		list =
				s.createQuery(
						"select new map( z.name as zname, z.address as zaddress ) from Zoo z left join z.mammals m order by zaddress, zname",
						Map.class
				).list();
		assertEquals( 4, list.size() );

		assertEquals( zoo3.getName(), list.get( 0 ).get( "zname" ) );
		assertEquals( zoo3.getAddress(), list.get( 0 ).get( "zaddress" ) );

		assertEquals( zoo2.getName(), list.get( 1 ).get( "zname" ) );
		assertEquals( zoo2.getAddress(), list.get( 1 ).get( "zaddress" ) );

		assertEquals( zoo1.getName(), list.get( 2 ).get( "zname" ) );
		assertEquals( zoo1.getAddress(), list.get( 2 ).get( "zaddress" ) );

		assertEquals( zoo4.getName(), list.get( 3 ).get( "zname" ) );
		assertEquals( zoo4.getAddress(), list.get( 3 ).get( "zaddress" ) );
		t.commit();
		s.close();

		cleanupData();
	}

	@Test
	public void testOrderByAggregatedArgAliasRef() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by name, address:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		List<Object[]> list =
				s.createQuery(
						"select z.name as zname, count(*) as cnt from Zoo z group by z.name order by cnt desc, zname",
						Object[].class
				).list();
		assertEquals( 3, list.size() );
		assertEquals( zoo3.getName(), list.get( 0 )[ 0 ] );
		assertEquals(2L, list.get( 0 )[ 1 ] );
		assertEquals( zoo2.getName(), list.get( 1 )[ 0 ] );
		assertEquals(1L, list.get( 1 )[ 1 ] );
		assertEquals( zoo4.getName(), list.get( 2 )[ 0 ] );
		assertEquals(1L, list.get( 2 )[ 1 ] );
		t.commit();
		s.close();
		cleanupData();
	}

	private void checkTestOrderByResults(
			List results,
			Zoo zoo1,
			Zoo zoo2,
			Zoo zoo3,
			Zoo zoo4,
			Set<Zoo> zoosUnordered) {
		assertEquals( 4, results.size() );
		Set<Zoo> zoosUnorderedCopy = ( zoosUnordered == null ? null : new HashSet<Zoo>( zoosUnordered ) );
		checkTestOrderByResult( results.get( 0 ), zoo1, zoosUnorderedCopy );
		checkTestOrderByResult( results.get( 1 ), zoo2, zoosUnorderedCopy );
		checkTestOrderByResult( results.get( 2 ), zoo3, zoosUnorderedCopy );
		checkTestOrderByResult( results.get( 3 ), zoo4, zoosUnorderedCopy );
		if ( zoosUnorderedCopy != null ) {
			assertTrue( zoosUnorderedCopy.isEmpty() );
		}
	}

	private void checkTestOrderByResult(Object result,
										Zoo zooExpected,
										Set<Zoo> zoosUnordered) {
		assertTrue( result instanceof Object[] );
		Object[] resultArray = ( Object[] ) result;
		assertEquals( 2,  resultArray.length );
		Hibernate.initialize( ( ( Address ) resultArray[ 1 ] ).getStateProvince() );
		if ( zooExpected == null ) {
			Zoo zooResult = new Zoo();
			zooResult.setName( ( String ) resultArray[ 0 ] );
			zooResult.setAddress( ( Address ) resultArray[ 1 ] );
			assertTrue( zoosUnordered.remove( zooResult ) );
		}
		else {
			assertEquals( zooExpected.getAddress(), ( ( Object[] ) result )[ 1 ] );
			assertEquals( zooExpected.getName(), ( ( Object[] ) result )[ 0 ] );
		}
	}
}
