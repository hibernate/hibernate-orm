/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.List;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.integration.query.entities.Address;
import org.hibernate.orm.test.envers.integration.query.entities.Car;
import org.hibernate.orm.test.envers.integration.query.entities.Person;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@SuppressWarnings("unchecked")
@Jpa(annotatedClasses = {
		Car.class,
		Person.class,
		Address.class
})
@EnversTest
public class AssociationToOneInnerJoinQueryTest {

	private Car vw;
	private Car ford;
	private Car toyota;
	private Address address1;
	private Address address2;
	private Person vwOwner;
	private Person fordOwner;
	private Person toyotaOwner;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			address1 = new Address( "Freiburgerstrasse", 5 );
			em.persist( address1 );
			address2 = new Address( "Hindenburgstrasse", 30 );
			em.persist( address2 );
			vwOwner = new Person( "VW owner", 20, address1 );
			em.persist( vwOwner );
			fordOwner = new Person( "Ford owner", 30, address1 );
			em.persist( fordOwner );
			toyotaOwner = new Person( "Toyota owner", 30, address2 );
			em.persist( toyotaOwner );
			final Person nonOwner = new Person( "NonOwner", 30, address1 );
			em.persist( nonOwner );
			vw = new Car( "VW" );
			vw.setOwner( vwOwner );
			em.persist( vw );
			ford = new Car( "Ford" );
			ford.setOwner( fordOwner );
			em.persist( ford );
			toyota = new Car( "Toyota" );
			toyota.setOwner( toyotaOwner );
			em.persist( toyota );
			em.getTransaction().commit();

			// revision 2
			em.getTransaction().begin();
			toyotaOwner.setAge( 40 );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testAssociationQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final Car result1 = (Car) auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 ).traverseRelation( "owner", JoinType.INNER )
					.add( AuditEntity.property( "name" ).like( "Ford%" ) ).getSingleResult();
			assertEquals( ford.getId(), result1.getId(), "Unexpected single car at revision 1" );

			Car result2 = (Car) auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 ).traverseRelation( "owner", JoinType.INNER ).traverseRelation( "address", JoinType.INNER )
					.add( AuditEntity.property( "number" ).eq( 30 ) ).getSingleResult();
			assertEquals( toyota.getId(), result2.getId(), "Unexpected single car at revision 1" );

			List<Car> resultList1 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 ).traverseRelation( "owner", JoinType.INNER )
					.add( AuditEntity.property( "age" ).ge( 30 ) ).add( AuditEntity.property( "age" ).lt( 40 ) ).up()
					.addOrder( AuditEntity.property( "make" ).asc() ).getResultList();
			assertEquals( 2, resultList1.size(), "Unexpected number of cars for query in revision 1" );
			assertEquals( ford.getId(), resultList1.get( 0 ).getId(), "Unexpected car at index 0 in revision 1" );
			assertEquals( toyota.getId(), resultList1.get( 1 ).getId(), "Unexpected car at index 1 in revision 2" );

			Car result3 = (Car) auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 ).traverseRelation( "owner", JoinType.INNER )
					.add( AuditEntity.property( "age" ).ge( 30 ) ).add( AuditEntity.property( "age" ).lt( 40 ) ).up()
					.addOrder( AuditEntity.property( "make" ).asc() ).getSingleResult();
			assertEquals( ford.getId(), result3.getId(), "Unexpected car at revision 2" );
		} );
	}

	@Test
	public void testAssociationQueryWithOrdering(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List<Car> cars1 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 ).traverseRelation( "owner", JoinType.INNER ).traverseRelation( "address", JoinType.INNER )
					.addOrder( AuditEntity.property( "number" ).asc() ).up().addOrder( AuditEntity.property( "age" ).desc() ).getResultList();
			assertEquals( 3, cars1.size(), "Unexpected number of results" );
			assertEquals( ford.getId(), cars1.get( 0 ).getId(), "Unexpected car at index 0" );
			assertEquals( vw.getId(), cars1.get( 1 ).getId(), "Unexpected car at index 1" );
			assertEquals( toyota.getId(), cars1.get( 2 ).getId(), "Unexpected car at index 2" );

			List<Car> cars2 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 1 ).traverseRelation( "owner", JoinType.INNER ).traverseRelation( "address", JoinType.INNER )
					.addOrder( AuditEntity.property( "number" ).asc() ).up().addOrder( AuditEntity.property( "age" ).asc() ).getResultList();
			assertEquals( 3, cars2.size(), "Unexpected number of results" );
			assertEquals( vw.getId(), cars2.get( 0 ).getId(), "Unexpected car at index 0" );
			assertEquals( ford.getId(), cars2.get( 1 ).getId(), "Unexpected car at index 1" );
			assertEquals( toyota.getId(), cars2.get( 2 ).getId(), "Unexpected car at index 2" );
		} );
	}

	@Test
	public void testAssociationQueryWithProjection(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List<Integer> list1 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 ).traverseRelation( "owner", JoinType.INNER )
					.addProjection( AuditEntity.property( "age" ) ).addOrder( AuditEntity.property( "age" ).asc() ).getResultList();
			assertEquals( 3, list1.size(), "Unexpected number of results" );
			assertEquals( Integer.valueOf( 20 ), list1.get( 0 ), "Unexpected age at index 0" );
			assertEquals( Integer.valueOf( 30 ), list1.get( 1 ), "Unexpected age at index 0" );
			assertEquals( Integer.valueOf( 40 ), list1.get( 2 ), "Unexpected age at index 0" );

			List<Address> list2 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 ).traverseRelation( "owner", JoinType.INNER )
					.addOrder( AuditEntity.property( "age" ).asc() ).traverseRelation( "address", JoinType.INNER ).addProjection( AuditEntity.selectEntity( false ) ).getResultList();
			assertEquals( 2, list2.size(), "Unexpected number of results" );
			assertEquals( address1.getId(), list2.get( 0 ).getId(), "Unexpected address at index 0" );
			assertEquals( address2.getId(), list2.get( 1 ).getId(), "Unexpected address at index 1" );

			List<Address> list3 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 ).traverseRelation( "owner", JoinType.INNER ).traverseRelation( "address", JoinType.INNER )
					.addProjection( AuditEntity.selectEntity( true ) ).addOrder( AuditEntity.property( "number" ).asc() ).getResultList();
			assertEquals( 2, list3.size(), "Unexpected number of results" );
			assertEquals( address1.getId(), list3.get( 0 ).getId(), "Unexpected address at index 0" );
			assertEquals( address2.getId(), list3.get( 1 ).getId(), "Unexpected address at index 1" );

			List<Object[]> list4 = auditReader.createQuery().forEntitiesAtRevision( Car.class, 2 ).traverseRelation( "owner", JoinType.INNER )
					.addOrder( AuditEntity.property( "age" ).asc() ).addProjection( AuditEntity.selectEntity( false ) ).traverseRelation( "address", JoinType.INNER )
					.addProjection( AuditEntity.property( "number" ) ).getResultList();
			assertEquals( 3, list4.size(), "Unexpected number of results" );
			final Object[] index0 = list4.get( 0 );
			assertEquals( vwOwner.getId(), ( (Person) index0[0] ).getId(), "Unexpected owner at index 0" );
			assertEquals( Integer.valueOf( 5 ), index0[1], "Unexpected number at index 0" );
			final Object[] index1 = list4.get( 1 );
			assertEquals( fordOwner.getId(), ( (Person) index1[0] ).getId(), "Unexpected owner at index 1" );
			assertEquals( Integer.valueOf( 5 ), index1[1], "Unexpected number at index 1" );
			final Object[] index2 = list4.get( 2 );
			assertEquals( toyotaOwner.getId(), ( (Person) index2[0] ).getId(), "Unexpected owner at index 2" );
			assertEquals( Integer.valueOf( 30 ), index2[1], "Unexpected number at index 2" );
		} );
	}

	@Test
	public void testDisjunctionOfPropertiesFromDifferentEntities(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// all cars where the owner has an age of 20 or lives in an address with number 30.
			List<Car> resultList = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Car.class, 1 )
					.traverseRelation( "owner", JoinType.INNER, "p" )
					.traverseRelation( "address", JoinType.INNER, "a" )
					.up().up().add( AuditEntity.disjunction().add(AuditEntity.property( "p", "age" )
							.eq( 20 ) ).add( AuditEntity.property( "a", "number" ).eq( 30 ) ) )
					.addOrder( AuditEntity.property( "make" ).asc() ).getResultList();
			assertEquals( 2, resultList.size(), "Expected two cars to be returned, Toyota and VW" );
			assertEquals( toyota.getId(), resultList.get(0).getId(), "Unexpected car at index 0" );
			assertEquals( vw.getId(), resultList.get(1).getId(), "Unexpected car at index 1" );
		} );
	}

	@Test
	public void testComparisonOfTwoPropertiesFromDifferentEntities(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// the car where the owner age is equal to the owner address number.
			Car result = (Car) AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Car.class, 1 )
					.traverseRelation( "owner", JoinType.INNER, "p" )
					.traverseRelation( "address", JoinType.INNER, "a" )
					.up().up().add(AuditEntity.property( "p", "age" )
							.eqProperty( "a", "number" ) ).getSingleResult();
			assertEquals( toyota.getId(), result.getId(), "Unexpected car returned" );
		} );
	}
}
