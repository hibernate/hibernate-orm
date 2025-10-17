/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.joined;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/mapping/inheritance/joined/Person.hbm.xml"
)
@SessionFactory
public class JoinedSubclassTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testJoinedSubclass(SessionFactoryScope scope) {
		scope.inTransaction( s -> {

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

			assertEquals( 3, s.createQuery( "from Person" ).list().size() );
			assertEquals( 1, s.createQuery( "from Person p where p.class = Customer" ).list().size() );
			assertEquals( 1, s.createQuery( "from Person p where p.class = Person" ).list().size() );
			assertEquals( 1, s.createQuery( "from Person p where type(p) in :who" )
								.setParameter( "who", Customer.class )
								.list()
								.size() );
			assertEquals( 2, s.createQuery( "from Person p where type(p) in :who" ).setParameterList(
					"who",
					new Class[] {
							Customer.class,
							Person.class
					}
			).list().size() );
			s.clear();

			List customers = s.createQuery( "from Customer c left join fetch c.salesperson" ).list();
			for ( Object o : customers ) {
				Customer c = (Customer) o;
				assertTrue( Hibernate.isInitialized( c.getSalesperson() ) );
				assertEquals( "Mark", c.getSalesperson().getName() );
			}
			assertEquals( 1, customers.size() );
			s.clear();

			customers = s.createQuery( "from Customer" ).list();
			for ( Object customer : customers ) {
				Customer c = (Customer) customer;
				assertFalse( Hibernate.isInitialized( c.getSalesperson() ) );
				assertEquals( "Mark", c.getSalesperson().getName() );
			}
			assertEquals( 1, customers.size() );
			s.clear();


			mark = s.find( Employee.class, mark.getId() );
			joe = s.find( Customer.class, joe.getId() );

			mark.setZip( "30306" );
			assertEquals( 1, s.createQuery( "from Person p where p.address.zip = '30306'" ).list().size() );

			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			CriteriaBuilder.In<Address> in = criteriaBuilder.in( root.get( "address" ) );
			in.value( mark.getAddress() );
			in.value( joe.getAddress() );

			criteria.where( in );
			Query<Person> query = s.createQuery( criteria );
			query.list();
//		s.createCriteria( Person.class ).add(
//                Restrictions.in( "address", mark.getAddress(), joe.getAddress() ) ).list();

			s.remove( mark );
			s.remove( joe );
			s.remove( yomomma );
			assertTrue( s.createQuery( "from Person" ).list().isEmpty() );
		} );
	}

	@Test
	public void testCustomColumnReadAndWrite(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final double HEIGHT_INCHES = 73;
					final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;
					Person p = new Person();
					p.setName( "Emmanuel" );
					p.setSex( 'M' );
					p.setHeightInches( HEIGHT_INCHES );
					session.persist( p );
					final double PASSWORD_EXPIRY_WEEKS = 4;
					final double PASSWORD_EXPIRY_DAYS = PASSWORD_EXPIRY_WEEKS * 7d;
					Employee e = new Employee();
					e.setName( "Steve" );
					e.setSex( 'M' );
					e.setTitle( "Mr" );
					e.setPasswordExpiryDays( PASSWORD_EXPIRY_DAYS );
					session.persist( e );
					session.flush();

					// Test value conversion during insert
					// Value returned by Oracle native query is a Types.NUMERIC, which is mapped to a BigDecimalType;
					// Cast returned value to Number then call Number.doubleValue() so it works on all dialects.
					Double heightViaSql =
							( (Number) session.createNativeQuery(
									"select height_centimeters from JPerson where name='Emmanuel'" )
									.uniqueResult() )
									.doubleValue();
					assertEquals( HEIGHT_CENTIMETERS, heightViaSql, 0.01d );
					Double expiryViaSql =
							( (Number) session.createNativeQuery(
									"select pwd_expiry_weeks from JEmployee where person_id=?" )
									.setParameter( 1, e.getId() )
									.uniqueResult()
							).doubleValue();
					assertEquals( PASSWORD_EXPIRY_WEEKS, expiryViaSql, 0.01d );

					// Test projection
					Double heightViaHql = (Double) session.createQuery(
							"select p.heightInches from Person p where p.name = 'Emmanuel'" )
							.uniqueResult();
					assertEquals( HEIGHT_INCHES, heightViaHql, 0.01d );
					Double expiryViaHql = (Double) session.createQuery(
							"select e.passwordExpiryDays from Employee e where e.name = 'Steve'" ).uniqueResult();
					assertEquals( PASSWORD_EXPIRY_DAYS, expiryViaHql, 0.01d );

					// Test restriction and entity load via criteria
//		p = (Person)s.createCriteria(Person.class)
//				.add(Restrictions.between("heightInches", HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d))
//				.uniqueResult();
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = criteria.from( Person.class );
					criteria.where( criteriaBuilder.between(
							root.get( "heightInches" ),
							HEIGHT_INCHES - 0.01d,
							HEIGHT_INCHES + 0.01d
					) );
					p = session.createQuery( criteria ).uniqueResult();

					assertEquals( HEIGHT_INCHES, p.getHeightInches(), 0.01d );
					CriteriaQuery<Employee> employeeCriteriaQuery = criteriaBuilder.createQuery( Employee.class );
					Root<Employee> employeeRoot = employeeCriteriaQuery.from( Employee.class );
					employeeCriteriaQuery.where( criteriaBuilder.between(
							employeeRoot.get( "passwordExpiryDays" ),
							PASSWORD_EXPIRY_DAYS - 0.01d,
							PASSWORD_EXPIRY_DAYS + 0.01d
					) );
					e = session.createQuery( employeeCriteriaQuery ).uniqueResult();
//		e = (Employee) s.createCriteria( Employee.class )
//				.add( Restrictions.between(
//						"passwordExpiryDays",
//						PASSWORD_EXPIRY_DAYS - 0.01d,
//						PASSWORD_EXPIRY_DAYS + 0.01d
//				) )
//				.uniqueResult();
					assertEquals( PASSWORD_EXPIRY_DAYS, e.getPasswordExpiryDays(), 0.01d );

					// Test predicate and entity load via HQL
					p = (Person) session.createQuery( "from Person p where p.heightInches between ?1 and ?2" )
							.setParameter( 1, HEIGHT_INCHES - 0.01d )
							.setParameter( 2, HEIGHT_INCHES + 0.01d )
							.uniqueResult();
					assertEquals( HEIGHT_INCHES, p.getHeightInches(), 0.01d );
					e = (Employee) session.createQuery( "from Employee e where e.passwordExpiryDays between ?1 and ?2" )
							.setParameter( 1, PASSWORD_EXPIRY_DAYS - 0.01d )
							.setParameter( 2, PASSWORD_EXPIRY_DAYS + 0.01d )
							.uniqueResult();
					assertEquals( PASSWORD_EXPIRY_DAYS, e.getPasswordExpiryDays(), 0.01d );

					// Test update
					p.setHeightInches( 1 );
					e.setPasswordExpiryDays( 7 );
					session.flush();
					heightViaSql =
							( (Number) session.createNativeQuery(
									"select height_centimeters from JPerson where name='Emmanuel'" )
									.uniqueResult() )
									.doubleValue();
					assertEquals( 2.54d, heightViaSql, 0.01d );
					expiryViaSql =
							( (Number) session.createNativeQuery(
									"select pwd_expiry_weeks from JEmployee where person_id=?" )
									.setParameter( 1, e.getId() )
									.uniqueResult()
							).doubleValue();
					assertEquals( 1d, expiryViaSql, 0.01d );
					session.remove( p );
					session.remove( e );
				}
		);
	}

}
