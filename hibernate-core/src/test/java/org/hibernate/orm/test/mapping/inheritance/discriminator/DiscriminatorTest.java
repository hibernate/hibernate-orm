/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import java.math.BigDecimal;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/mapping/inheritance/discriminator/Person.hbm.xml"
)
@SessionFactory
@SuppressWarnings("deprecation")
public class DiscriminatorTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDiscriminatorSubclass(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Employee mark = new Employee();
					mark.setName( "Mark" );
					mark.setTitle( "internal sales" );
					mark.setSex( 'M' );
					mark.setAddress( "buckhead" );
					mark.setZip( "30305" );
					mark.setCountry( "USA" );

					Customer joe = new Customer();
					joe.setName( "Joe" );
					joe.setAddress( "San Francisco" );
					joe.setZip( "XXXXX" );
					joe.setCountry( "USA" );
					joe.setComments( "Very demanding" );
					joe.setSex( 'M' );
					joe.setSalesperson( mark );

					Person yomomma = new Person();
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

					assertThat( s.createQuery( "from Person" ).list().size(), is( 3 ) );
					assertThat( s.createQuery( "from Person p where p.class = Person" ).list().size(), is( 1 ) );
					assertThat( s.createQuery( "from Person p where p.class = Customer" ).list().size(), is( 1 ) );
					s.clear();

					var customers = s.createQuery( "from Customer c left join fetch c.salesperson" ).list();
					for ( var customer : customers ) {
						var c = (Customer) customer;
						assertTrue( Hibernate.isInitialized( c.getSalesperson() ) );
						assertThat( c.getSalesperson().getName(), is( "Mark" ) );
					}
					assertThat( customers.size(), is( 1 ) );
					s.clear();

					customers = s.createQuery( "from Customer" ).list();
					for ( var customer : customers ) {
						var c = (Customer) customer;
						assertFalse( Hibernate.isInitialized( c.getSalesperson() ) );
						assertThat( c.getSalesperson().getName(), is( "Mark" ) );
					}
					assertThat( customers.size(), is( 1 ) );
					s.clear();


					mark = s.get( Employee.class, mark.getId() );
					joe = s.get( Customer.class, joe.getId() );

					mark.setZip( "30306" );
					assertThat( s.createQuery( "from Person p where p.address.zip = '30306'" ).list().size(), is( 1 ) );
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
					employee.setName( "Steve" );
					employee.setSex( 'M' );
					employee.setTitle( "grand poobah" );
					s.persist( employee );
				}
		);

		Customer c = null;
		scope.fromTransaction(
				s -> s.get( Customer.class, employee.getId() )
		);
		assertNull( c );

		scope.inTransaction(
				s -> {
					Employee e = s.get( Employee.class, employee.getId() );
					Customer customer = s.get( Customer.class, employee.getId() );
					assertNotNull( e );
					assertNull( customer );
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
					p.setName( "Emmanuel" );
					p.setSex( 'M' );
					s.persist( p );
					Employee q = new Employee();
					q.setName( "Steve" );
					q.setSex( 'M' );
					q.setTitle( "Mr" );
					q.setSalary( new BigDecimal( 1000 ) );
					s.persist( q );

					var result = s.createQuery( "from Person where salary > 100" ).list();
					assertEquals( 1, result.size() );
					assertSame( result.get( 0 ), q );

					result = s.createQuery( "from Person where salary > 100 or name like 'E%'" ).list();
					assertEquals( 2, result.size() );

					var criteriaBuilder = s.getCriteriaBuilder();
					var criteria = criteriaBuilder.createQuery( Person.class );
					var root = criteria.from( Person.class );
					criteria.where( criteriaBuilder.gt( criteriaBuilder.treat( root, Employee.class ).get( "salary" ), new BigDecimal( 100 ) ) );
					result = s.createQuery( criteria ).list();
//					result = s.createCriteria(Person.class)
//							.add( Property.forName( "salary").gt( new BigDecimal( 100) ) )
//							.list();
					assertEquals( 1, result.size() );
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
		Employee e = new Employee();
		scope.inTransaction(
				s -> {
					e.setName( "Steve" );
					e.setSex( 'M' );
					e.setTitle( "grand poobah" );
					s.persist( e );
				}
		);

		scope.inTransaction(
				s -> {
					// load the superclass proxy.
					Person pLoad = s.getReference( Person.class, e.getId() );
					assertInstanceOf( HibernateProxy.class, pLoad );
					Person pGet = s.get( Person.class, e.getId() );
					Person pQuery = (Person) s.createQuery( "from Person where id = :id" )
							.setParameter( "id", e.getId() )
							.uniqueResult();
					var criteriaBuilder = s.getCriteriaBuilder();
					var criteria = criteriaBuilder.createQuery( Person.class );
					var root = criteria.from( Person.class );
					criteria.where( criteriaBuilder.equal( root.get( "id" ), e.getId() ) );
					Person pCriteria = s.createQuery( criteria ).uniqueResult();
//		Person pCriteria = ( Person ) s.createCriteria( Person.class )
//				.add( Restrictions.idEq( new Long( e.getId() ) ) )
//				.uniqueResult();
					// assert that executing the queries polymorphically returns the same proxy
					assertSame( pLoad, pGet );
					assertSame( pLoad, pQuery );
					assertSame( pLoad, pCriteria );

					// assert that the proxy is not an instance of Employee
					assertFalse( pLoad instanceof Employee );
				}
		);

		scope.inTransaction(
				s -> {
					// load the superclass proxy.
					Person pLoad = s.getReference( Person.class, e.getId() );
					assertInstanceOf( HibernateProxy.class, pLoad );
					Person pGet = s.get( Person.class, e.getId() );
					Person pQuery = (Person) s.createQuery( "from Person where id = :id" )
							.setParameter( "id", e.getId() )
							.uniqueResult();
					var criteriaBuilder = s.getCriteriaBuilder();
					var criteria = criteriaBuilder.createQuery( Employee.class );
					var root = criteria.from( Employee.class );
					criteria.where( criteriaBuilder.equal( root.get( "id" ), e.getId() ) );
					Employee pCriteria = s.createQuery( criteria ).uniqueResult();
//		Person pCriteria = ( Person ) s.createCriteria( Person.class )
//				.add( Restrictions.idEq( new Long( e.getId() ) ) )
//				.uniqueResult();
					// assert that executing the queries polymorphically returns the same proxy
					assertSame( pLoad, pGet );
					assertSame( pLoad, pQuery );
					assertNotSame( pLoad, pCriteria );

					// assert that the proxy is not an instance of Employee
					assertFalse( pLoad instanceof Employee );
				}
		);

		scope.inTransaction(
				s -> s.remove( e )
		);
	}

	@Test
	public void testLoadSuperclassProxyEvictPolymorphicAccess(SessionFactoryScope scope) {
		Employee e = new Employee();
		scope.inTransaction(
				s -> {
					e.setName( "Steve" );
					e.setSex( 'M' );
					e.setTitle( "grand poobah" );
					s.persist( e );
				}
		);

		scope.inTransaction(
				s -> {
					// load the superclass proxy.
					Person pLoad = s.getReference( Person.class, e.getId() );
					assertInstanceOf( HibernateProxy.class, pLoad );
					// evict the proxy
					s.evict( pLoad );
					Employee pGet = (Employee) s.get( Person.class, e.getId() );
					Employee pQuery = (Employee) s.createQuery( "from Person where id = :id" )
							.setParameter( "id", e.getId() )
							.uniqueResult();
					var criteriaBuilder = s.getCriteriaBuilder();
					var criteria = criteriaBuilder.createQuery( Person.class );
					var root = criteria.from( Person.class );
					criteria.where( criteriaBuilder.equal( root.get( "id" ), e.getId() ) );

					Employee pCriteria = (Employee) s.createQuery( criteria ).uniqueResult();
//					Employee pCriteria = ( Employee ) s.createCriteria( Person.class )
//							.add( Restrictions.idEq( new Long( e.getId() ) ) )
//							.uniqueResult();
					// assert that executing the queries polymorphically returns the same Employee instance
					assertSame( pGet, pQuery );
					assertSame( pGet, pCriteria );
				}
		);

		scope.inTransaction(
				s -> s.remove( e )
		);
	}
}
