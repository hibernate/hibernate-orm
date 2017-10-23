/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;
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
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests AST parser processing of ORDER BY clauses.
 *
 * @author Gail Badner
 */
public class ASTParserLoadingOrderByTest extends BaseCoreFunctionalTestCase {
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
	public String[] getMappings() {
		return new String[] {
				"hql/Animal.hbm.xml",
		};
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "false" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.QUERY_TRANSLATOR, ASTQueryTranslatorFactory.class.getName() );
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
		address3.setCountry( "USA" );
		zoo3.setAddress( address3 );

		zoo4 = new Zoo();
		zoo4.setName( "Duh Zoo" );
		Address address4 = new Address();
		address4.setStreet( "1312 Mockingbird Lane" );
		address4.setCity( "Nowhere" );
		address4.setStateProvince( stateProvince );
		address4.setCountry( "USA" );
		zoo4.setAddress( address4 );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.save( stateProvince );
		s.save( zoo1Mammal1 );
		s.save( zoo1Mammal2 );
		s.save( zoo1 );
		s.save( zoo2Director1 );
		s.save( zoo2Director2 );
		s.save( zoo2 );
		s.save( zoo3 );
		s.save( zoo4 );
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
			s.delete( zoo1 );
			zoo1 = null;
		}
		if ( zoo2 != null ) {
			s.delete( zoo2 );
			zoo2 = null;
		}
		if ( zoo3 != null ) {
			s.delete( zoo3 );
			zoo3 = null;
		}
		if ( zoo4 != null ) {
			s.delete( zoo4 );
			zoo4 = null;
		}
		if ( zoo1Mammal1 != null ) {
			s.delete( zoo1Mammal1 );
			zoo1Mammal1 = null;
		}
		if ( zoo1Mammal2 != null ) {
			s.delete( zoo1Mammal2 );
			zoo1Mammal2 = null;
		}
		if ( zoo2Director1 != null ) {
			s.delete( zoo2Director1 );
			zoo2Director1 = null;
		}
		if ( zoo2Director2 != null ) {
			s.delete( zoo2Director2 );
			zoo2Director2 = null;			
		}
		if ( stateProvince != null ) {
			s.delete( stateProvince );
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
						"select name, address from Zoo order by name, address"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.name, z.address"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z2.name, z2.address from Zoo z2 where z2.name in ( select name from Zoo ) order by z2.name, z2.address"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		// using ASC
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by name ASC, address ASC"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.name ASC, z.address ASC"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z2.name, z2.address from Zoo z2 where z2.name in ( select name from Zoo ) order by z2.name ASC, z2.address ASC"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);

		// ordered by address, name:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.address, z.name"
				).list(),
				zoo3, zoo4, zoo2, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by address, name"
				).list(),
				zoo3, zoo4, zoo2, zoo1, null
		);

		// ordered by address:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		// unordered:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.address"
				).list(),
				zoo3, zoo4, null, null, zoosWithSameAddress
		);
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by address"
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
						"select z.name, z.address from Zoo z order by z.name"
				).list(),
				zoo2, zoo4, null, null, zoosWithSameName
		);
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by name"
				).list(),
				zoo2, zoo4, null, null, zoosWithSameName
		);
		t.commit();
		s.close();

		cleanupData();
	}

	@Test
	@FailureExpected( jiraKey = "unknown" )
	public void testOrderByComponentDescNoSelectAliasRef() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by address DESC, name DESC:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address from Zoo z order by z.address DESC, z.name DESC"
				).list(),
				zoo1, zoo2, zoo4, zoo3, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select name, address from Zoo order by address DESC, name DESC"
				).list(),
				zoo1, zoo2, zoo4, zoo3, null
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
						"select z2.name as zname, z2.address as zooAddress from Zoo z2 where z2.name in ( select name from Zoo ) order by zname, zooAddress"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as name, z.address as address from Zoo z order by name, address"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooName, zooAddress"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by z.name, name"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by z.name, name"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		// using ASC
		checkTestOrderByResults(
				s.createQuery(
						"select z2.name as zname, z2.address as zooAddress from Zoo z2 where z2.name in ( select name from Zoo ) order by zname ASC, zooAddress ASC"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as name, z.address as address from Zoo z order by name ASC, address ASC"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooName ASC, zooAddress ASC"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by z.name ASC, name ASC"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by z.name ASC, name ASC"
				).list(),
				zoo2, zoo4, zoo3, zoo1, null
		);

		// ordered by address, name:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as address, z.address as name from Zoo z order by name, address"
				).list(),
				zoo3, zoo4, zoo2, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by name, z.name"
				).list(),
				zoo3, zoo4, zoo2, zoo1, null
		);
		// using ASC
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as address, z.address as name from Zoo z order by name ASC, address ASC"
				).list(),
				zoo3, zoo4, zoo2, zoo1, null
		);
		checkTestOrderByResults(
				s.createQuery(
						"select z.name, z.address as name from Zoo z order by name ASC, z.name ASC"
				).list(),
				zoo3, zoo4, zoo2, zoo1, null
		);

		// ordered by address:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		// unordered:
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooAddress"
				).list(),
				zoo3, zoo4, null, null, zoosWithSameAddress
		);

		checkTestOrderByResults(
				s.createQuery(
						"select z.name as zooName, z.address as name from Zoo z order by name"
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
						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooName"
				).list(),
				zoo2, zoo4, null, null, zoosWithSameName
		);

		checkTestOrderByResults(
				s.createQuery(
						"select z.name as address, z.address as name from Zoo z order by address"
				).list(),
				zoo2, zoo4, null, null, zoosWithSameName
		);
		t.commit();
		s.close();

		cleanupData();
	}

	@Test
	@FailureExpected( jiraKey = "unknown")
	public void testOrderByComponentDescSelectAliasRefFailureExpected() {
		createData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		// ordered by address desc, name desc:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		// using DESC
		checkTestOrderByResults(
				s.createQuery(
						"select z.name as zooName, z.address as zooAddress from Zoo z order by zooAddress DESC, zooName DESC"
				).list(),
				zoo1, zoo2, zoo4, zoo3, null
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
		List list = s.createQuery( "from Zoo z join fetch z.mammals" ).list();

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
		List list =
				s.createQuery(
						"select new Zoo( z.name as zname, z.address as zaddress) from Zoo z order by zname, zaddress"
				).list();
		assertEquals( 4, list.size() );
		assertEquals( zoo2, list.get( 0 ) );
		assertEquals( zoo4, list.get( 1 ) );
		assertEquals( zoo3, list.get( 2 ) );
		assertEquals( zoo1, list.get( 3 ) );

		// ordered by address, name:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		list =
				s.createQuery(
						"select new Zoo( z.name as zname, z.address as zaddress) from Zoo z order by zaddress, zname"
				).list();
		assertEquals( 4, list.size() );
		assertEquals( zoo3, list.get( 0 ) );
		assertEquals( zoo4, list.get( 1 ) );
		assertEquals( zoo2, list.get( 2 ) );
		assertEquals( zoo1, list.get( 3 ) );


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
		List list =
				s.createQuery(
						"select new map( z.name as zname, z.address as zaddress ) from Zoo z left join z.mammals m order by zname, zaddress"
				).list();
		assertEquals( 4, list.size() );
		assertEquals( zoo2.getName(), ( ( Map ) list.get( 0 ) ).get( "zname" ) );
		assertEquals( zoo2.getAddress(), ( ( Map ) list.get( 0 ) ).get( "zaddress" ) );
		assertEquals( zoo4.getName(), ( ( Map ) list.get( 1 ) ).get( "zname" ) );
		assertEquals( zoo4.getAddress(), ( ( Map ) list.get( 1 ) ).get( "zaddress" ) );
		assertEquals( zoo3.getName(), ( ( Map ) list.get( 2 ) ).get( "zname" ) );
		assertEquals( zoo3.getAddress(), ( ( Map ) list.get( 2 ) ).get( "zaddress" ) );
		assertEquals( zoo1.getName(), ( ( Map ) list.get( 3 ) ).get( "zname" ) );
		assertEquals( zoo1.getAddress(), ( ( Map ) list.get( 3 ) ).get( "zaddress" ) );

		// ordered by address, name:
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		list =
				s.createQuery(
						"select new map( z.name as zname, z.address as zaddress ) from Zoo z left join z.mammals m order by zaddress, zname"
				).list();
		assertEquals( 4, list.size() );
		assertEquals( zoo3.getName(), ( ( Map ) list.get( 0 ) ).get( "zname" ) );
		assertEquals( zoo3.getAddress(), ( ( Map ) list.get( 0 ) ).get( "zaddress" ) );
		assertEquals( zoo4.getName(), ( ( Map ) list.get( 1 ) ).get( "zname" ) );
		assertEquals( zoo4.getAddress(), ( ( Map ) list.get( 1 ) ).get( "zaddress" ) );
		assertEquals( zoo2.getName(), ( ( Map ) list.get( 2 ) ).get( "zname" ) );
		assertEquals( zoo2.getAddress(), ( ( Map ) list.get( 2 ) ).get( "zaddress" ) );
		assertEquals( zoo1.getName(), ( ( Map ) list.get( 3 ) ).get( "zname" ) );
		assertEquals( zoo1.getAddress(), ( ( Map ) list.get( 3 ) ).get( "zaddress" ) );
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
		List list =
				s.createQuery(
						"select z.name as zname, count(*) as cnt from Zoo z group by z.name order by cnt desc, zname"
				).list();
		assertEquals( 3, list.size() );
		assertEquals( zoo3.getName(), ( ( Object[] ) list.get( 0 ) )[ 0 ] );
		assertEquals( Long.valueOf( 2 ), ( ( Object[] ) list.get( 0 ) )[ 1 ] );
		assertEquals( zoo2.getName(), ( ( Object[] ) list.get( 1 ) )[ 0 ] );
		assertEquals( Long.valueOf( 1 ), ( ( Object[] ) list.get( 1 ) )[ 1 ] );
		assertEquals( zoo4.getName(), ( ( Object[] ) list.get( 2 ) )[ 0 ] );
		assertEquals( Long.valueOf( 1 ), ( ( Object[] ) list.get( 2 ) )[ 1 ] );
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
			assertEquals( zooExpected.getName(), ( ( Object[] ) result )[ 0 ] );
			assertEquals( zooExpected.getAddress(), ( ( Object[] ) result )[ 1 ] );
		}
	}
}
