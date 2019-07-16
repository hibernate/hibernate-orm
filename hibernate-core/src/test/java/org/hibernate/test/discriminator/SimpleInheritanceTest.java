/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.discriminator;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class SimpleInheritanceTest extends BaseCoreFunctionalTestCase {
	public void configure(Configuration cfg) {
		super.configure( cfg );
	}

	@Override
	public String[] getMappings() {
		return new String[] { "discriminator/SimpleInheritance.hbm.xml" };
	}

	@Test
	public void testDiscriminatorSubclass() {
		inTransaction(
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

					s.save( yomomma );
					s.save( mark );
					s.save( joe );

					assertEquals( s.createQuery( "from java.io.Serializable" ).list().size(), 0 );

					assertEquals( s.createQuery( "from org.hibernate.test.discriminator.Person" ).list().size(), 3 );
					assertEquals( s.createQuery(
							"from org.hibernate.test.discriminator.Person p where p.class = org.hibernate.test.discriminator.Person" )
										  .list()
										  .size(), 1 );
					assertEquals( s.createQuery(
							"from org.hibernate.test.discriminator.Person p where p.class = org.hibernate.test.discriminator.Customer" )
										  .list()
										  .size(), 1 );
					assertEquals( s.createQuery( "from org.hibernate.test.discriminator.Person p where type(p) = :who" )
										  .setParameter( "who", Person.class )
										  .list()
										  .size(), 1 );
					assertEquals( s.createQuery( "from org.hibernate.test.discriminator.Person p where type(p) in :who" )
										  .setParameterList( "who", new Class[] { Customer.class, Person.class } )
										  .list()
										  .size(), 2 );
					s.clear();

					List customers = s.createQuery( "from org.hibernate.test.discriminator.Customer" ).list();
					for ( Object customer : customers ) {
						Customer c = (Customer) customer;
						assertEquals( "Very demanding", c.getComments() );
					}
					assertEquals( customers.size(), 1 );
					s.clear();

					mark = s.get( Employee.class, mark.getId() );
					joe = s.get( Customer.class, joe.getId() );

					s.delete( mark );
					s.delete( joe );
					s.delete( yomomma );
					assertTrue( s.createQuery( "from org.hibernate.test.discriminator.Person" ).list().isEmpty() );

				}
		);
	}

	@Test
	public void testAccessAsIncorrectSubclass() {
		Employee employee = new Employee();
		inTransaction(
				s -> {
					employee.setId( 4 );
					employee.setName( "Steve" );
					employee.setSex( 'M' );
					employee.setTitle( "grand poobah" );
					s.save( employee );
				}
		);

		Customer c = null;
		Session s = openSession();
		try {
			s.beginTransaction();

			c = s.get( Customer.class, employee.getId() );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction().isActive() ) {
				s.getTransaction().rollback();
			}
		}
		finally {
			s.close();

		}
		assertNull( c );

		Employee e = null;
		s = openSession();
		try {
			s.beginTransaction();

			e = s.get( Employee.class, employee.getId() );
			c = s.get( Customer.class, e.getId() );
			s.getTransaction().commit();
		}
		catch (Exception ex) {
			if ( s.getTransaction().isActive() ) {
				s.getTransaction().rollback();
			}
		}
		finally {
			s.close();
		}
		assertNotNull( e );
		assertNull( c );

		inTransaction(
				session -> session.delete( employee )
		);
	}

	@Test
	public void testQuerySubclassAttribute() {
		inTransaction(
				s -> {
					Person p = new Person();
					p.setId( 5 );
					p.setName( "Emmanuel" );
					p.setSex( 'M' );
					s.save( p );
					Employee q = new Employee();
					q.setId( 6 );
					q.setName( "Steve" );
					q.setSex( 'M' );
					q.setTitle( "Mr" );
					q.setSalary( new BigDecimal( 1000 ) );
					s.save( q );

					List result = s.createQuery( "from org.hibernate.test.discriminator.Person where salary > 100" )
							.list();
					assertEquals( result.size(), 1 );
					assertSame( result.get( 0 ), q );

					result = s.createQuery(
							"from org.hibernate.test.discriminator.Person where salary > 100 or name like 'E%'" )
							.list();
					assertEquals( result.size(), 2 );

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = criteria.from( Person.class );
					criteria.where( criteriaBuilder.gt( root.get( "salary" ), new BigDecimal( 100 ) ) );

					result = s.createQuery( criteria ).list();
//					result = s.createCriteria( Person.class )
//							.add( Property.forName( "salary" ).gt( new BigDecimal( 100 ) ) )
//							.list();
					assertEquals( result.size(), 1 );
					assertSame( result.get( 0 ), q );

					//TODO: make this work:
		/*result = s.createQuery("select salary from Person where salary > 100").list();
		assertEquals( result.size(), 1 );
		assertEquals( result.get(0), new BigDecimal(1000) );*/

					s.delete( p );
					s.delete( q );
				}
		);
	}

	@Test
	public void testLoadSuperclassProxyPolymorphicAccess() {
		Employee employee = new Employee();
		inTransaction(
				s -> {
					employee.setId( 7 );
					employee.setName( "Steve" );
					employee.setSex( 'M' );
					employee.setTitle( "grand poobah" );
					s.save( employee );
				}
		);

		inTransaction(
				s -> {
					// load the superclass proxy.
					Person pLoad = s.load( Person.class, new Long( employee.getId() ) );
					assertTrue( pLoad instanceof HibernateProxy );
					Person pGet = s.get( Person.class, employee.getId() );
					Person pQuery = (Person) s.createQuery(
							"from org.hibernate.test.discriminator.Person where id = :id" )
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
					assertSame( pLoad, pQuery );
					assertSame( pLoad, pCriteria );

					// assert that the proxy is not an instance of Employee
					assertFalse( pLoad instanceof Employee );
				}
		);

		inTransaction(
				s -> s.delete( employee )
		);
	}

	@Test
	public void testLoadSuperclassProxyEvictPolymorphicAccess() {
		Employee employee = new Employee();
		inTransaction(
				s -> {
					employee.setId( 8 );
					employee.setName( "Steve" );
					employee.setSex( 'M' );
					employee.setTitle( "grand poobah" );
					s.save( employee );
				}

		);

		inTransaction(
				s -> {
					// load the superclass proxy.
					Person pLoad = s.load( Person.class, new Long( employee.getId() ) );
					assertTrue( pLoad instanceof HibernateProxy );
					// evict the proxy
					s.evict( pLoad );
					Employee pGet = (Employee) s.get( Person.class, employee.getId() );
					Employee pQuery = (Employee) s.createQuery(
							"from org.hibernate.test.discriminator.Person where id = :id" )
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

		inTransaction(
				s -> s.delete( employee )
		);
	}
}
