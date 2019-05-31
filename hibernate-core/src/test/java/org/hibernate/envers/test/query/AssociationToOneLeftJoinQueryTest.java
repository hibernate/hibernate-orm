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
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@SuppressWarnings("unchecked")
public class AssociationToOneLeftJoinQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private Car car1;
	private Car car2;
	private Car car3;
	private Person person1;
	private Person person2;
	private Address address1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Car.class, Person.class, Address.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					address1 = new Address( "address1", 1 );
					entityManager.persist( address1 );

					Address address2 = new Address( "address2", 2 );
					entityManager.persist( address2 );

					person1 = new Person( "person1", 30, address1 );
					entityManager.persist( person1 );

					person2 = new Person( "person2", 20, null );
					entityManager.persist( person2 );

					Person person3 = new Person( "person3", 10, address1 );
					entityManager.persist( person3 );

					car1 = new Car( "car1" );
					car1.setOwner( person1 );
					entityManager.persist( car1 );

					car2 = new Car( "car2" );
					car2.setOwner( person2 );
					entityManager.persist( car2 );

					car3 = new Car( "car3" );
					entityManager.persist( car3 );
				},

				// Revision 2
				entityManager -> {
					person2.setAge( 21 );
				}
		);
	}

	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testLeftJoinOnAuditedEntity() {
		final AuditReader auditReader = getAuditReader();
		// all cars where the owner has an age of 20 or where there is no owner at all
		List<Car> resultList = auditReader.createQuery()
				.forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.LEFT, "p" )
				.up()
				.add(
						AuditEntity.or(
								AuditEntity.property( "p", "age").eq( 20 ),
								AuditEntity.relatedId( "owner" ).eq( null ) ) )
				.addOrder( AuditEntity.property( "make" ).asc() )
				.getResultList();
		// The result list should have 2 results
		// car1 because its owner has an age of 30, car3 because it has no owner
		assertThat( resultList, hasSize( 2 ) );

		List<Long> carIds = resultList.stream().map( Car::getId ).collect( Collectors.toList() );
		assertThat( carIds, contains( car2.getId(), car3.getId() ) );
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
	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testEntitiesWithANullRelatedIdAreNotJoinedToOtherEntities() {
		final AuditReader auditReader = getAuditReader();
		List<Car> resultList = auditReader.createQuery()
				.forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.LEFT, "p" )
				.up()
				.add(
						AuditEntity.and(
								AuditEntity.property( "make" ).eq( "car3" ),
								AuditEntity.property( "p", "age" ).eq( 30 ) ) )
				.getResultList();
		assertThat( "Expected no cars to be returned, because car3 does not have an owner", resultList, isEmpty() );
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
	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testEntitiesWithANullRelatedIdAreNotReturnedMoreThanOnce() {
		final AuditReader auditReader = getAuditReader();
		List<Car> resultList = auditReader.createQuery()
				.forEntitiesAtRevision( Car.class, 1 )
				.traverseRelation( "owner", JoinType.LEFT, "p" )
				.up()
				.add(
						AuditEntity.or(
								AuditEntity.property( "make" ).eq( "car3" ),
								AuditEntity.property( "p", "age" ).eq( 10 ) ) )
				.getResultList();
		assertThat( "Expected car3 to be returned but only once", resultList, hasSize( 1 ) );
		assertThat( "Unexpected car at index 0", resultList.get( 0 ).getId(), equalTo( car3.getId() ) );
	}
	
}
