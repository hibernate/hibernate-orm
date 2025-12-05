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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class AssociationToOneLeftJoinQueryTest {

	private Car car1;
	private Car car2;
	private Car car3;
	private Person person1;
	private Person person2;
	private Address address1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			address1 = new Address( "address1", 1 );
			em.persist( address1 );
			Address address2 = new Address( "address2", 2 );
			em.persist( address2 );
			person1 = new Person( "person1", 30, address1 );
			em.persist( person1 );
			person2 = new Person( "person2", 20, null );
			em.persist( person2 );
			Person person3 = new Person( "person3", 10, address1 );
			em.persist( person3 );
			car1 = new Car( "car1" );
			car1.setOwner( person1 );
			em.persist( car1 );
			car2 = new Car( "car2" );
			car2.setOwner( person2 );
			em.persist( car2 );
			car3 = new Car( "car3" );
			em.persist( car3 );
			em.getTransaction().commit();

			// revision 2
			em.getTransaction().begin();
			person2.setAge( 21 );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testLeftJoinOnAuditedEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// all cars where the owner has an age of 20 or where there is no owner at all
			List<Car> resultList = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Car.class, 1 )
					.traverseRelation( "owner", JoinType.LEFT, "p" )
					.up().add( AuditEntity.or( AuditEntity.property( "p", "age" ).eq( 20 ),
							AuditEntity.relatedId( "owner" ).eq( null ) ) )
					.addOrder( AuditEntity.property( "make" ).asc() ).getResultList();
			assertEquals( 2, resultList.size(), "The result list should have 2 results, car1 because its owner has an age of 30 and car3 because it has no owner at all" );
			Car car0 = resultList.get( 0 );
			Car car1 = resultList.get( 1 );
			assertEquals( car2.getId(), car0.getId(), "Unexpected car at index 0" );
			assertEquals( car3.getId(), car1.getId(), "Unexpected car at index 0" );
		} );
	}

	/**
	 * In a first attempt to implement left joins in Envers, a full join
	 * has been performed and than the entities has been filtered in the
	 * where clause. However, this approach did only work for inner joins
	 * but not for left joins. One of the defects in this approach is,
	 * that audit entities, which have a null 'relatedId' are and do not
	 * match the query criterias, still joined to other entities which matched
	 * match the query criterias.
	 * This test ensures that this defect is no longer in the current implementation.
	 */
	@Test
	public void testEntitiesWithANullRelatedIdAreNotJoinedToOtherEntities(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Car> resultList = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Car.class, 1 )
					.traverseRelation( "owner", JoinType.LEFT, "p" )
					.up().add( AuditEntity.and( AuditEntity.property( "make" ).eq( "car3" ), AuditEntity.property( "p", "age" ).eq( 30 ) ) )
					.getResultList();
			assertTrue( resultList.isEmpty(), "Expected no cars to be returned, because car3 does not have an owner" );
		} );
	}

	/**
	 * In a first attempt to implement left joins in Envers, a full join
	 * has been performed and than the entities has been filtered in the
	 * where clause. However, this approach did only work for inner joins
	 * but not for left joins. One of the defects in this approach is,
	 * that audit entities, which have a null 'relatedId' and do match
	 * the query criterias, have been returned multiple times by a query.
	 * This test ensures that this defect is no longer in the current implementation.
	 */
	@Test
	public void testEntitiesWithANullRelatedIdAreNotReturnedMoreThanOnce(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Car> resultList = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Car.class, 1 )
					.traverseRelation( "owner", JoinType.LEFT, "p" )
					.up().add( AuditEntity.or( AuditEntity.property( "make" ).eq( "car3" ), AuditEntity.property( "p", "age" ).eq( 10 ) ) )
					.getResultList();
			assertEquals( 1, resultList.size(), "Expected car3 to be returned but only once" );
			assertEquals( car3.getId(), resultList.get( 0 ).getId(), "Unexpected car at index 0" );
		} );
	}

}
