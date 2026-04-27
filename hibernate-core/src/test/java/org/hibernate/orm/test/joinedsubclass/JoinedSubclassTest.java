/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.joinedsubclass;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.LockMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/joinedsubclass/Person.hbm.xml"
)
@SessionFactory
public class JoinedSubclassTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testAccessAsIncorrectSubclass(SessionFactoryScope scope) {
		final Employee e = new Employee();
		scope.inTransaction(
				session -> {
					e.setName( "Steve" );
					e.setSex( 'M' );
					e.setTitle( "grand poobah" );
					session.persist( e );
				}
		);


		Customer c = scope.fromTransaction(
				session ->
						session.find( Customer.class, e.getId() )

		);
		assertNull( c );

		scope.inTransaction(
				session -> {
					Employee employee = session.find( Employee.class, e.getId() );
					Customer customer = session.find( Customer.class, e.getId() );
					assertNotNull( employee );
					assertNull( customer );
				}
		);

		scope.inTransaction(
				session -> {
					session.remove( e );
				}
		);
	}

	@Test
	public void testQuerySubclassAttribute(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Person p = new Person();
			p.setName( "Emmanuel" );
			p.setSex( 'M' );
			s.persist( p );
			Employee q = new Employee();
			q.setName( "Steve" );
			q.setSex( 'M' );
			q.setTitle( "Mr" );
			q.setSalary( new BigDecimal( 1000 ) );
			s.persist( q );

			var result = s.createQuery( Person.class,"from Person where salary > 100" ).list();
			assertEquals( 1, result.size() );
			assertSame( result.get( 0 ), q );

			result = s.createQuery( Person.class, "from Person where salary > 100 or name like 'E%'" ).list();
			assertEquals( 2, result.size() );

			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );

			Root<Person> root = criteria.from( Person.class );

			criteria.where( criteriaBuilder.gt( criteriaBuilder.treat( root, Employee.class ).get( "salary" ), new BigDecimal( 100 ) ) );

			result = s.createQuery( criteria ).list();
			assertEquals( 1, result.size() );
			assertSame( result.get( 0 ), q );

			s.remove( p );
			s.remove( q );
		} );
	}

	@Test
	public void testLockingJoinedSubclass(SessionFactoryScope scope) {
		Person p = new Person();
		Employee q = new Employee();
		scope.inTransaction(
				session -> {
					p.setName( "Emmanuel" );
					p.setSex( 'M' );
					session.persist( p );
					q.setName( "Steve" );
					q.setSex( 'M' );
					q.setTitle( "Mr" );
					q.setSalary( new BigDecimal( 1000 ) );
					session.persist( q );
				}
		);

		scope.inTransaction(
				session -> {
					session.lock( session.get(Person.class, p.getId()), LockMode.PESSIMISTIC_WRITE );
					session.lock( session.get( Employee.class, q.getId()), LockMode.PESSIMISTIC_WRITE );
					session.remove( p );
					session.remove( q );
				}
		);
	}

}
