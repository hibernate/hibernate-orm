/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.JoinType;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.query.Address;
import org.hibernate.envers.test.support.domains.query.Car;
import org.hibernate.envers.test.support.domains.query.Person;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@SuppressWarnings("unchecked")
public class AssociationToOneInnerJoinQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private Car vw;
	private Car ford;
	private Car toyota;
	private Address address1;
	private Address address2;
	private Person vwOwner;
	private Person fordOwner;
	private Person toyotaOwner;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Car.class, Person.class, Address.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					address1 = new Address( "Freiburgerstrasse", 5 );
					entityManager.persist( address1 );

					address2 = new Address( "Hindenburgstrasse", 30 );
					entityManager.persist( address2 );

					vwOwner = new Person( "VW owner", 20, address1 );
					entityManager.persist( vwOwner );

					fordOwner = new Person( "Ford owner", 30, address1 );
					entityManager.persist( fordOwner );

					toyotaOwner = new Person( "Toyota owner", 30, address2 );
					entityManager.persist( toyotaOwner );

					final Person nonOwner = new Person( "NonOwner", 30, address1 );
					entityManager.persist( nonOwner );

					vw = new Car( "VW" );
					vw.setOwner( vwOwner );
					entityManager.persist( vw );

					ford = new Car( "Ford" );
					ford.setOwner( fordOwner );
					entityManager.persist( ford );

					toyota = new Car( "Toyota" );
					toyota.setOwner( toyotaOwner );
					entityManager.persist( toyota );
				},

				// Revision 2
				entityManager -> {
					toyotaOwner.setAge( 40 );
				}
		);
	}

	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testAssociationQuery() {
		final AuditReader auditReader = getAuditReader();

		final Car result1 = (Car) auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.INNER )
				.add( AuditEntity.property( "name" ).like( "Ford%" ) )
				.getSingleResult();
		assertThat( "Unexpected single car at revision 1", result1.getId(), equalTo( ford.getId() ) );

		Car result2 = (Car) auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.INNER )
				.traverseRelation( "address", JoinType.INNER )
				.add( AuditEntity.property( "number" ).eq( 30 ) )
				.getSingleResult();
		assertThat( "Unexpected single car at revision 1", result2.getId(), equalTo( toyota.getId() ) );

		List<Car> resultList1 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.INNER )
				.add( AuditEntity.property( "age" ).ge( 30 ) )
				.add( AuditEntity.property( "age" ).lt( 40 ) )
				.up()
				.addOrder( AuditEntity.property( "make" ).asc() )
				.getResultList();

		List<Long> carIds = resultList1.stream().map( Car::getId ).collect( Collectors.toList() );
		assertThat( carIds, contains( ford.getId(), toyota.getId() ) );

		Car result3 = (Car) auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 )
				.traverseRelation( "owner", JoinType.INNER )
				.add( AuditEntity.property( "age" ).ge( 30 ) )
				.add( AuditEntity.property( "age" ).lt( 40 ) )
				.up()
				.addOrder( AuditEntity.property( "make" ).asc() )
				.getSingleResult();
		assertThat( "Unexpected car at revision 2", result3.getId(), equalTo( ford.getId() ) );
	}

	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testAssociationQueryWithOrdering() {
		final AuditReader auditReader = getAuditReader();

		List<Car> cars1 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.INNER )
				.traverseRelation( "address", JoinType.INNER )
				.addOrder( AuditEntity.property( "number" ).asc() )
				.up()
				.addOrder( AuditEntity.property( "age" ).desc() )
				.getResultList();

		List<Long> cars1Ids = cars1.stream().map( Car::getId ).collect( Collectors.toList() );
		assertThat( cars1Ids, contains( ford.getId(), vw.getId(), toyota.getId() ) );

		List<Car> cars2 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.INNER )
				.traverseRelation( "address", JoinType.INNER )
				.addOrder( AuditEntity.property( "number" ).asc() )
				.up()
				.addOrder( AuditEntity.property( "age" ).asc() )
				.getResultList();

		List<Long> cars2Ids = cars2.stream().map( Car::getId ).collect( Collectors.toList() );
		assertThat( cars2Ids, contains( vw.getId(), ford.getId(), toyota.getId() ) );
	}

	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testAssociationQueryWithProjection() {
		final AuditReader auditReader = getAuditReader();

		List<Integer> list1 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 )
				.traverseRelation( "owner", JoinType.INNER )
				.addProjection( AuditEntity.property( "age" ) )
				.addOrder( AuditEntity.property( "age" ).asc() )
				.getResultList();
		assertThat( list1, contains( 20, 30, 40 ) );

		List<Address> list2 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 )
				.traverseRelation( "owner", JoinType.INNER )
				.addOrder( AuditEntity.property( "age" ).asc() )
				.traverseRelation( "address", JoinType.INNER )
				.addProjection( AuditEntity.selectEntity( false ) )
				.getResultList();

		List<Long> list2AddressIds = list2.stream().map( Address::getId ).collect( Collectors.toList() );
		assertThat( list2AddressIds, contains( address1.getId(), address1.getId(), address2.getId() ) );

		List<Address> list3 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 )
				.traverseRelation( "owner", JoinType.INNER )
				.traverseRelation( "address", JoinType.INNER )
				.addProjection( AuditEntity.selectEntity( true ) )
				.addOrder( AuditEntity.property( "number" ).asc() )
				.getResultList();

		List<Long> list3AddressIds = list3.stream().map( Address::getId ).collect( Collectors.toList() );
		assertThat( list3AddressIds, contains( address1.getId(), address2.getId() ) );

		List<Object[]> list4 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 )
				.traverseRelation( "owner", JoinType.INNER )
				.addOrder( AuditEntity.property( "age" ).asc() )
				.addProjection( AuditEntity.selectEntity( false ) )
				.traverseRelation( "address", JoinType.INNER )
				.addProjection( AuditEntity.property( "number" ) )
				.getResultList();
		assertThat( "Unexpected number of results", list4, hasSize( 4 ) );
		final Object[] index0 = list4.get( 0 );
		assertThat( "Unexpected owner at index 0", ( (Person) index0[0] ).getId(), equalTo( vwOwner.getId() ) );
		assertThat( "Unexpected number at index 0", index0[1], equalTo( 5 ) );
		final Object[] index1 = list4.get( 1 );
		assertThat( "Unexpected owner at index 1", ( (Person) index1[0] ).getId(), equalTo( fordOwner.getId() ) );
		assertThat( "Unexpected number at index 1", index1[1], equalTo( 5 ) );
		final Object[] index2 = list4.get( 2 );
		assertThat( "Unexpected owner at index 2", ( (Person) index2[0] ).getId(), equalTo( toyotaOwner.getId() ) );
		assertThat( "Unexpected number at index 2", index2[1], equalTo( 30 ) );
	}

	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testDisjunctionOfPropertiesFromDifferentEntities() {
		final AuditReader auditReader = getAuditReader();
		// all cars where the owner has an age of 20 or lives in an address with number 30.
		List<Car> resultList = auditReader.createQuery()
				.forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.INNER, "p" )
				.traverseRelation( "address", JoinType.INNER, "a" )
				.up()
				.up()
				.add(
						AuditEntity.disjunction()
								.add( AuditEntity.property( "p", "age" ).eq( 20 ) )
								.add( AuditEntity.property( "a", "number" ).eq( 30 ) ) )
				.addOrder( AuditEntity.property( "make" ).asc() )
				.getResultList();

		List<Long> carIds = resultList.stream().map( Car::getId ).collect( Collectors.toList() );
		assertThat( carIds, contains( toyota.getId(), vw.getId() ) );
	}

	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testComparisonOfTwoPropertiesFromDifferentEntities() {
		AuditReader auditReader = getAuditReader();
		// the car where the owner age is equal to the owner address number.
		Car result = (Car) auditReader.createQuery()
				.forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.INNER, "p" )
				.traverseRelation( "address", JoinType.INNER, "a" )
				.up()
				.up()
				.add( AuditEntity.property( "p", "age" ).eqProperty( "a", "number" ) )
				.getSingleResult();
		assertThat( "Unexpected car returned", result.getId(), equalTo( toyota.getId() ) );
	}
}
