/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.orm.test.hql.Address;
import org.hibernate.orm.test.hql.Human;
import org.hibernate.orm.test.hql.Mammal;
import org.hibernate.orm.test.hql.Name;
import org.hibernate.orm.test.hql.StateProvince;
import org.hibernate.orm.test.hql.Zoo;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests AST parser processing of ORDER BY clauses.
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/hql/Animal.hbm.xml"
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_QUERY_CACHE, value = "false"),
		}
)
public class OrderByTests {
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


	@BeforeEach
	private void createData(SessionFactoryScope scope) {
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
		zoo1.getMammals().put( "type1", zoo1Mammal1 );
		zoo1Mammal2 = new Mammal();
		zoo1Mammal2.setDescription( "zoo1Mammal2" );
		zoo1Mammal2.setZoo( zoo1 );
		zoo1.getMammals().put( "type1", zoo1Mammal2 );

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

		scope.inTransaction(
				session -> {
					session.persist( stateProvince );
					session.persist( zoo1Mammal1 );
					session.persist( zoo1Mammal2 );
					session.persist( zoo1 );
					session.persist( zoo2Director1 );
					session.persist( zoo2Director2 );
					session.persist( zoo2 );
					session.persist( zoo3 );
					session.persist( zoo4 );
				}
		);

		zoosWithSameName = new HashSet<>( 2 );
		zoosWithSameName.add( zoo1 );
		zoosWithSameName.add( zoo3 );
		zoosWithSameAddress = new HashSet<>( 2 );
		zoosWithSameAddress.add( zoo1 );
		zoosWithSameAddress.add( zoo2 );
	}

	@AfterEach
	private void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testOrderByOnJoinedSubclassPropertyWhoseColumnIsNotInDrivingTable(SessionFactoryScope scope) {
		// this is simply a syntax check
		scope.inTransaction(
				session ->
						session.createQuery( "from Human h order by h.bodyWeight" ).list()
		);
	}

	@Test
	public void testOrderByNoSelectAliasRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered by name, address:
					//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
					//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
					//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
					checkTestOrderByResults(
							session.createQuery(
									"select name, address from Zoo order by name, address",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name, z.address from Zoo z order by z.name, z.address",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z2.name, z2.address from Zoo z2 where z2.name in ( select name from Zoo ) order by z2.name, z2.address",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					// using ASC
					checkTestOrderByResults(
							session.createQuery(
									"select name, address from Zoo order by name ASC, address ASC",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name, z.address from Zoo z order by z.name ASC, z.address ASC",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
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
							session.createQuery(
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
							session.createQuery(
									"select z.name, z.address from Zoo z order by z.address, z.name",
									Object[].class
							).list(),
							zoo3, zoo2, zoo1, zoo4, null
					);

					// NOTE (6.0) - continued 3 : and the functionally equiv "full ordering"
					checkTestOrderByResults(
							session.createQuery(
									"select z.name, z.address from Zoo z order by z.address.city, z.address.country, z.address.stateProvince, z.address.street, z.name",
									Object[].class
							).list(),
							zoo3, zoo2, zoo1, zoo4, null
					);

					checkTestOrderByResults(
							session.createQuery(
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
							session.createQuery(
									"select z.name, z.address from Zoo z order by z.address.street, z.address.city",
									Object[].class
							).list(),
							zoo3, zoo4, null, null, zoosWithSameAddress
					);
					checkTestOrderByResults(
							session.createQuery(
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
							session.createQuery(
									"select z.name, z.address from Zoo z order by z.name",
									Object[].class
							).list(),
							zoo2, zoo4, null, null, zoosWithSameName
					);
					checkTestOrderByResults(
							session.createQuery(
									"select name, address from Zoo order by name",
									Object[].class
							).list(),
							zoo2, zoo4, null, null, zoosWithSameName
					);
				}
		);
	}

	@Test
	public void testOrderByComponentDescNoSelectAliasRef(SessionFactoryScope scope) {
		// ordered by address DESC, name DESC:
		//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
		//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
		//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
		scope.inTransaction(
				session -> {
					checkTestOrderByResults(
							session.createQuery(
									"select z.name, z.address from Zoo z order by z.address DESC, z.name DESC",
									Object[].class
							).list(),
							zoo4, zoo1, zoo2, zoo3, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select name, address from Zoo order by address DESC, name DESC",
									Object[].class
							).list(),
							zoo4, zoo1, zoo2, zoo3, null
					);
				}
		);
	}

	@Test
	public void testOrderBySelectAliasRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					// ordered by name, address:
					//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
					//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
					//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
					checkTestOrderByResults(
							session.createQuery(
									"select z2.name as zname, z2.address as zooAddress from Zoo z2 where z2.name in ( select name from Zoo ) order by zname, zooAddress",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name as name, z.address as address from Zoo z order by name, address",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name as zooName, z.address as zooAddress from Zoo z order by zooName, zooAddress",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name, z.address as name from Zoo z order by z.name, name",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name, z.address as name from Zoo z order by z.name, name",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					// using ASC
					checkTestOrderByResults(
							session.createQuery(
									"select z2.name as zname, z2.address as zooAddress from Zoo z2 where z2.name in ( select name from Zoo ) order by zname ASC, zooAddress ASC",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name as name, z.address as address from Zoo z order by name ASC, address ASC",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name as zooName, z.address as zooAddress from Zoo z order by zooName ASC, zooAddress ASC",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name, z.address as name from Zoo z order by z.name ASC, name ASC",
									Object[].class
							).list(),
							zoo2, zoo4, zoo3, zoo1, null
					);
					checkTestOrderByResults(
							session.createQuery(
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
							session.createQuery(
									"select z.name as address, z.address as name from Zoo z order by name, address",
									Object[].class
							).list(),
							zoo3, zoo2, zoo1, zoo4, null
					);
					checkTestOrderByResults(
							session.createQuery(
									"select z.name, z.address as name from Zoo z order by name, z.name",
									Object[].class
							).list(),
							zoo3, zoo2, zoo1, zoo4, null
					);
					// using ASC
					checkTestOrderByResults(
							session.createQuery(
									"select z.name as address, z.address as name from Zoo z order by name ASC, address ASC",
									Object[].class
							).list(),
							zoo3, zoo2, zoo1, zoo4, null
					);
					checkTestOrderByResults(
							session.createQuery(
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
							session.createQuery(
									"select z.name as zooName, z.address as zooAddress from Zoo z order by zooName",
									Object[].class
							).list(),
							zoo2, zoo4, null, null, zoosWithSameName
					);

					checkTestOrderByResults(
							session.createQuery(
									"select z.name as address, z.address as name from Zoo z order by address",
									Object[].class
							).list(),
							zoo2, zoo4, null, null, zoosWithSameName
					);
				}
		);
	}

	@Test
	public void testOrderByComponentDescSelectAliasRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered by address desc, name desc:
					//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
					//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
					// using DESC
					checkTestOrderByResults(
							session.createQuery(
									"select z.name as zooName, z.address as zooAddress from Zoo z order by zooAddress DESC, zooName DESC",
									Object[].class
							).list(),
							zoo4, zoo1, zoo2, zoo3, null
					);
				}
		);
	}

	@Test
	public void testOrderByEntityWithFetchJoinedCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered by address desc, name desc:
					//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
					//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
					//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
					// using DESC
					session.createQuery( "from Zoo z join fetch z.mammals", Zoo.class ).list();
				}
		);
	}

	@Test
	public void testOrderBySelectNewArgAliasRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered by name, address:
					//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
					//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
					//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
					List<Zoo> list =
							session.createQuery(
									"select new Zoo( z.name as zname, z.address as zaddress) from Zoo z order by zname, zaddress",
									Zoo.class
							).list();
					assertThat( list ).hasSize( 4 );
					assertThat( list.get( 0 ) ).isEqualTo( zoo2 );
					assertThat( list.get( 1 ) ).isEqualTo( zoo4 );
					assertThat( list.get( 2 ) ).isEqualTo( zoo3 );
					assertThat( list.get( 3 ) ).isEqualTo( zoo1 );

					// ordered by address, name:
					//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
					//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
					list =
							session.createQuery(
									"select new Zoo( z.name as zname, z.address as zaddress) from Zoo z order by zaddress, zname",
									Zoo.class
							).list();
					assertThat( list ).hasSize( 4 );
					assertThat( list.get( 0 ) ).isEqualTo( zoo3 );
					assertThat( list.get( 1 ) ).isEqualTo( zoo2 );
					assertThat( list.get( 2 ) ).isEqualTo( zoo1 );
					assertThat( list.get( 3 ) ).isEqualTo( zoo4 );
				}
		);
	}

	@Test
	@Timeout(value = 5, unit = TimeUnit.MINUTES)
	public void testOrderBySelectNewMapArgAliasRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered by name, address:
					//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
					//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
					//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
					List<Map> list =
							session.createQuery(
									"select new map( z.name as zname, z.address as zaddress ) from Zoo z left join z.mammals m order by zname, zaddress",
									Map.class
							).list();
					assertThat( list ).hasSize( 4 );
					assertThat( list.get( 0 ).get( "zname" ) ).isEqualTo( zoo2.getName() );
					assertThat( list.get( 0 ).get( "zaddress" ) ).isEqualTo( zoo2.getAddress() );
					assertThat( list.get( 1 ).get( "zname" ) ).isEqualTo( zoo4.getName() );
					assertThat( list.get( 1 ).get( "zaddress" ) ).isEqualTo( zoo4.getAddress() );
					assertThat( list.get( 2 ).get( "zname" ) ).isEqualTo( zoo3.getName() );
					assertThat( list.get( 2 ).get( "zaddress" ) ).isEqualTo( zoo3.getAddress() );
					assertThat( list.get( 3 ).get( "zname" ) ).isEqualTo( zoo1.getName() );
					assertThat( list.get( 3 ).get( "zaddress" ) ).isEqualTo( zoo1.getAddress() );

					// ordered by address, name:
					//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
					//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
					list =
							session.createQuery(
									"select new map( z.name as zname, z.address as zaddress ) from Zoo z left join z.mammals m order by zaddress, zname",
									Map.class
							).list();
					assertThat( list ).hasSize( 4 );

					assertThat( list.get( 0 ).get( "zname" ) ).isEqualTo( zoo3.getName() );
					assertThat( list.get( 0 ).get( "zaddress" ) ).isEqualTo( zoo3.getAddress() );

					assertThat( list.get( 1 ).get( "zname" ) ).isEqualTo( zoo2.getName() );
					assertThat( list.get( 1 ).get( "zaddress" ) ).isEqualTo( zoo2.getAddress() );

					assertThat( list.get( 2 ).get( "zname" ) ).isEqualTo( zoo1.getName() );
					assertThat( list.get( 2 ).get( "zaddress" ) ).isEqualTo( zoo1.getAddress() );

					assertThat( list.get( 3 ).get( "zname" ) ).isEqualTo( zoo4.getName() );
					assertThat( list.get( 3 ).get( "zaddress" ) ).isEqualTo( zoo4.getAddress() );
				}
		);
	}

	@Test
	public void testOrderByAggregatedArgAliasRef(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered by name, address:
					//   zoo2  A Zoo       1313 Mockingbird Lane, Anywhere, IL USA
					//   zoo4  Duh Zoo     1312 Mockingbird Lane, Nowhere, IL USA
					//   zoo3  Zoo         1312 Mockingbird Lane, Anywhere, IL USA
					//   zoo1  Zoo         1313 Mockingbird Lane, Anywhere, IL USA
					List<Object[]> list =
							session.createQuery(
									"select z.name as zname, count(*) as cnt from Zoo z group by z.name order by cnt desc, zname",
									Object[].class
							).list();
					assertThat( list ).hasSize( 3 );
					assertThat( list.get( 0 )[0] ).isEqualTo( zoo3.getName() );
					assertThat( list.get( 0 )[1] ).isEqualTo( 2L );
					assertThat( list.get( 1 )[0] ).isEqualTo( zoo2.getName() );
					assertThat( list.get( 1 )[1] ).isEqualTo( 1L );
					assertThat( list.get( 2 )[0] ).isEqualTo( zoo4.getName() );
					assertThat( list.get( 2 )[1] ).isEqualTo( 1L );
				}
		);
	}

	private void checkTestOrderByResults(
			List results,
			Zoo zoo1,
			Zoo zoo2,
			Zoo zoo3,
			Zoo zoo4,
			Set<Zoo> zoosUnordered) {
		assertThat( results ).hasSize( 4 );
		Set<Zoo> zoosUnorderedCopy = (zoosUnordered == null ? null : new HashSet<>( zoosUnordered ));
		checkTestOrderByResult( results.get( 0 ), zoo1, zoosUnorderedCopy );
		checkTestOrderByResult( results.get( 1 ), zoo2, zoosUnorderedCopy );
		checkTestOrderByResult( results.get( 2 ), zoo3, zoosUnorderedCopy );
		checkTestOrderByResult( results.get( 3 ), zoo4, zoosUnorderedCopy );
		if ( zoosUnorderedCopy != null ) {
			assertThat( zoosUnorderedCopy ).isEmpty();
		}
	}

	private void checkTestOrderByResult(
			Object result,
			Zoo zooExpected,
			Set<Zoo> zoosUnordered) {
		assertThat( result ).isInstanceOf( Object[].class );
		Object[] resultArray = (Object[]) result;
		assertThat( resultArray ).hasSize( 2 );
		Hibernate.initialize( ((Address) resultArray[1]).getStateProvince() );
		if ( zooExpected == null ) {
			Zoo zooResult = new Zoo();
			zooResult.setName( (String) resultArray[0] );
			zooResult.setAddress( (Address) resultArray[1] );
			assertThat( zoosUnordered.remove( zooResult ) ).isTrue();
		}
		else {
			assertThat( ((Object[]) result)[1] ).isEqualTo( zooExpected.getAddress() );
			assertThat( ((Object[]) result)[0] ).isEqualTo( zooExpected.getName() );
		}
	}
}
