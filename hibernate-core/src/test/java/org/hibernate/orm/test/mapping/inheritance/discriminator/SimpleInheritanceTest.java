/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import java.math.BigDecimal;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
@DomainModel( xmlMappings = "org/hibernate/orm/test/mapping/inheritance/discriminator/SimpleInheritance.hbm.xml")
@SessionFactory
public class SimpleInheritanceTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDiscriminatorSubclass(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Employee mark = new Employee();
					mark.setId( 1 );
					mark.setName( "Mark" );
					mark.setTitle( "internal sales" );
					mark.setSex( 'M' );

					Customer joe = new Customer();
					joe.setId( 2 );
					joe.setName( "Joe" );
					joe.setComments( "Very demanding" );
					joe.setSex( 'M' );

					Person yomomma = new Person();
					yomomma.setId( 3 );
					yomomma.setName( "mum" );
					yomomma.setSex( 'F' );

					s.persist( yomomma );
					s.persist( mark );
					s.persist( joe );

					try {
						s.createQuery( "from java.io.Serializable" ).list();
						fail( "Expected IllegalAccessException" );
					}
					catch (Exception e) {
						assertThat( e, instanceOf( IllegalArgumentException.class ) );
					}

					assertThat(
							s.createQuery( "from Person" ).list().size(),
							is( 3 )
					);
					assertThat( s.createQuery(
							"from Person p where p.class = Person" )
										.list()
										.size(), is( 1 ) );
					assertThat( s.createQuery(
							"from Person p where p.class = Customer" )
										.list()
										.size(), is( 1 ) );
					assertThat( s.createQuery( "from Person p where type(p) = :who" )
										.setParameter( "who", Person.class )
										.list()
										.size(), is( 1 ) );
					assertThat( s.createQuery( "from Person p where type(p) in :who" )
										.setParameterList( "who", new Class[] { Customer.class, Person.class } )
										.list()
										.size(), is( 2 ) );
					s.clear();

					List<Customer> customers = s.createQuery( "from Customer" ).list();
					for ( Customer c : customers ) {
						assertThat( c.getComments(), is( "Very demanding" ) );
					}
					assertThat( customers.size(), is( 1 ) );
					s.clear();

					mark = s.get( Employee.class, mark.getId() );
					joe = s.get( Customer.class, joe.getId() );

					s.remove( mark );
					s.remove( joe );
					s.remove( yomomma );
					assertTrue( s.createQuery( "from Person" ).list().isEmpty() );

				}
		);
	}

	@Test
	public void testAccessAsIncorrectSubclass(SessionFactoryScope scope) {
		Employee employee = new Employee();
		scope.inTransaction(
				s -> {
					employee.setId( 4 );
					employee.setName( "Steve" );
					employee.setSex( 'M' );
					employee.setTitle( "grand poobah" );
					s.persist( employee );
				}
		);

		Customer c = scope.fromTransaction(
				s ->
						s.get( Customer.class, employee.getId() )

		);

		assertNull( c );

		scope.inTransaction(
				s -> {
					Employee e = s.get( Employee.class, employee.getId() );
					Customer c1 = s.get( Customer.class, e.getId() );
					assertNotNull( e );
					assertNull( c1 );
				}
		);

		scope.inTransaction(
				session -> session.remove( employee )
		);
	}

	@Test
	public void testQuerySubclassAttribute(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Person p = new Person();
					p.setId( 5 );
					p.setName( "Emmanuel" );
					p.setSex( 'M' );
					s.persist( p );
					Employee q = new Employee();
					q.setId( 6 );
					q.setName( "Steve" );
					q.setSex( 'M' );
					q.setTitle( "Mr" );
					q.setSalary( new BigDecimal( 1000 ) );
					s.persist( q );

					List result = s.createQuery( "from Person where salary > 100" )
							.list();
					assertThat( result.size(), is( 1 ) );
					assertSame( result.get( 0 ), q );

					result = s.createQuery(
							"from Person where salary > 100 or name like 'E%'" )
							.list();
					assertThat( result.size(), is( 2 ) );

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = criteria.from( Person.class );
					criteria.where( criteriaBuilder.gt( criteriaBuilder.treat( root, Employee.class ).get( "salary" ), new BigDecimal( 100 ) ) );

					result = s.createQuery( criteria ).list();
//					result = s.createCriteria( Person.class )
//							.add( Property.forName( "salary" ).gt( new BigDecimal( 100 ) ) )
//							.list();
					assertThat( result.size(), is( 1 ) );
					assertSame( result.get( 0 ), q );

					//TODO: make this work:
		/*result = s.createQuery("select salary from Person where salary > 100").list();
		assertEquals( result.size(), 1 );
		assertEquals( result.get(0), new BigDecimal(1000) );*/

					s.remove( p );
					s.remove( q );
				}
		);
	}

	@Test
	public void testLoadSuperclassProxyPolymorphicAccess(SessionFactoryScope scope) {
		Employee employee = new Employee();
		scope.inTransaction(
				s -> {
					employee.setId( 7 );
					employee.setName( "Steve" );
					employee.setSex( 'M' );
					employee.setTitle( "grand poobah" );
					s.persist( employee );
				}
		);

		scope.inTransaction(
				s -> {
					// load the superclass proxy.
					Person pLoad = s.getReference( Person.class, employee.getId() );
					assertTrue( pLoad instanceof HibernateProxy );
					Person pGet = s.get( Person.class, employee.getId() );
					Person pQuery = (Person) s.createQuery(
							"from Person where id = :id" )
							.setParameter( "id", employee.getId() )
							.uniqueResult();

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = criteria.from( Person.class );
					criteria.where( criteriaBuilder.equal( root.get( "id" ), employee.getId() ) );
					Person pCriteria = s.createQuery( criteria ).uniqueResult();

//					Person pCriteria = (Person) s.createCriteria( Person.class )
//							.add( Restrictions.idEq( e.getId() ) )
//							.uniqueResult();
					// assert that executing the queries polymorphically returns the same proxy
					assertSame( pLoad, pGet );
					assertSame( pLoad, pCriteria );
					assertSame( pLoad, pQuery );

					// assert that the proxy is not an instance of Employee
					assertFalse( pLoad instanceof Employee );
				}
		);

		scope.inTransaction(
				s -> s.remove( employee )
		);
	}

	@Test
	public void testLoadSuperclassProxyEvictPolymorphicAccess(SessionFactoryScope scope) {
		Employee employee = new Employee();
		scope.inTransaction(
				s -> {
					employee.setId( 8 );
					employee.setName( "Steve" );
					employee.setSex( 'M' );
					employee.setTitle( "grand poobah" );
					s.persist( employee );
				}

		);

		scope.inTransaction(
				s -> {
					// load the superclass proxy.
					Person pLoad = s.getReference( Person.class, employee.getId() );
					assertTrue( pLoad instanceof HibernateProxy );
					// evict the proxy
					s.evict( pLoad );
					Employee pGet = (Employee) s.get( Person.class, employee.getId() );
					Employee pQuery = (Employee) s.createQuery(
							"from Person where id = :id" )
							.setParameter( "id", employee.getId() )
							.uniqueResult();

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = criteria.from( Person.class );
					criteria.where( criteriaBuilder.equal( root.get( "id" ), employee.getId() ) );

					Employee pCriteria = (Employee) s.createQuery( criteria ).uniqueResult();
//					Employee pCriteria = (Employee) s.createCriteria( Person.class )
//							.add( Restrictions.idEq( employee.getId() ) )
//							.uniqueResult();
					// assert that executing the queries polymorphically returns the same Employee instance
					assertSame( pGet, pQuery );
					assertSame( pGet, pCriteria );
				}
		);

		scope.inTransaction(
				s -> s.remove( employee )
		);
	}
}
