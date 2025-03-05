/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass2;

import java.math.BigDecimal;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 */
@DomainModel(xmlMappings = "org/hibernate/orm/test/unionsubclass2/Person.hbm.xml")
@SessionFactory
public class UnionSubclassTest {
	protected String[] getMappings() {
		return new String[] {};
	}

	@Test
	public void testUnionSubclass(SessionFactoryScope scope) {
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
						assertEquals( s.createQuery( "from java.io.Serializable" ).list().size(), 0 );
						fail( "IllegalArgumentException expected" );
					}
					catch (Exception e) {
						assertThat( e, instanceOf( IllegalArgumentException.class ) );
					}

					assertEquals( s.createQuery( "from Person" ).list().size(), 3 );
					assertEquals( s.createQuery( "from Person p where p.class = Customer" ).list().size(), 1 );
					assertEquals( s.createQuery( "from Person p where p.class = Person" ).list().size(), 1 );
					assertEquals( s.createQuery( "from Person p where type(p) in :who" ).setParameter(
							"who",
							Customer.class
					).list().size(), 1 );
					assertEquals( s.createQuery( "from Person p where type(p) in :who" ).setParameterList(
							"who",
							new Class[] {
									Customer.class,
									Person.class
							}
					).list().size(), 2 );
					s.clear();

					List customers = s.createQuery( "from Customer c left join fetch c.salesperson" ).list();
					for ( Object customer : customers ) {
						Customer c = (Customer) customer;
						assertTrue( Hibernate.isInitialized( c.getSalesperson() ) );
						assertEquals( c.getSalesperson().getName(), "Mark" );
					}
					assertEquals( customers.size(), 1 );
					s.clear();

					customers = s.createQuery( "from Customer" ).list();
					for ( Object customer : customers ) {
						Customer c = (Customer) customer;
						assertFalse( Hibernate.isInitialized( c.getSalesperson() ) );
						assertEquals( c.getSalesperson().getName(), "Mark" );
					}
					assertEquals( customers.size(), 1 );
					s.clear();


					mark = s.get( Employee.class, Long.valueOf( mark.getId() ) );
					joe = s.get( Customer.class, Long.valueOf( joe.getId() ) );

					mark.setZip( "30306" );
					assertEquals( s.createQuery( "from Person p where p.address.zip = '30306'" ).list().size(), 1 );

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = criteria.from( Person.class );

					CriteriaBuilder.In<Object> addresses = criteriaBuilder.in( root.get( "address" ) );
					addresses.value( mark.getAddress() );
					addresses.value( joe.getAddress() );
					criteria.where( addresses );

					s.createQuery( criteria ).list();
//					s.createCriteria( Person.class ).add(
//							Restrictions.in( "address", new Address[] {	mark.getAddress(),	joe.getAddress()} ) ).list();

					s.remove( mark );
					s.remove( joe );
					s.remove( yomomma );
					assertTrue( s.createQuery( "from Person" ).list().isEmpty() );

				}
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

					List result = s.createQuery( "from Person where salary > 100" ).list();
					assertEquals( result.size(), 1 );
					assertSame( result.get( 0 ), q );

					result = s.createQuery( "from Person where salary > 100 or name like 'E%'" ).list();
					assertEquals( result.size(), 2 );

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = criteria.from( Person.class );
					criteria.where( criteriaBuilder.gt( criteriaBuilder.treat( root, Employee.class ).get( "salary" ), new BigDecimal( 100 ) ) );

					result = s.createQuery( criteria ).list();

//					result = s.createCriteria( Person.class )
//							.add( Property.forName( "salary" ).gt( new BigDecimal( 100 ) ) )
//							.list();
					assertEquals( result.size(), 1 );
					assertSame( result.get( 0 ), q );

					result = s.createQuery( "select salary from Person where salary > 100" ).list();
					assertEquals( result.size(), 1 );
					assertEquals( ( (BigDecimal) result.get( 0 ) ).intValue(), 1000 );

					s.remove( p );
					s.remove( q );
				}
		);
	}

	@Test
	public void testCustomColumnReadAndWrite(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					final double HEIGHT_INCHES = 73;
					final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;
					Person p = new Person();
					p.setName( "Emmanuel" );
					p.setSex( 'M' );
					p.setHeightInches( HEIGHT_INCHES );
					s.persist( p );
					final double PASSWORD_EXPIRY_WEEKS = 4;
					final double PASSWORD_EXPIRY_DAYS = PASSWORD_EXPIRY_WEEKS * 7d;
					Employee e = new Employee();
					e.setName( "Steve" );
					e.setSex( 'M' );
					e.setTitle( "Mr" );
					e.setPasswordExpiryDays( PASSWORD_EXPIRY_DAYS );
					s.persist( e );
					s.flush();

					// Test value conversion during insert
					// Value returned by Oracle native query is a Types.NUMERIC, which is mapped to a BigDecimalType;
					// Cast returned value to Number then call Number.doubleValue() so it works on all dialects.
					Double heightViaSql =
							( (Number) s.createNativeQuery(
									"select height_centimeters from UPerson where name='Emmanuel'" )
									.uniqueResult() ).doubleValue();
					assertEquals( HEIGHT_CENTIMETERS, heightViaSql, 0.01d );
					Double expiryViaSql =
							( (Number) s.createNativeQuery( "select pwd_expiry_weeks from UEmployee where person_id=?" )
									.setParameter( 1, e.getId() )
									.uniqueResult()
							).doubleValue();
					assertEquals( PASSWORD_EXPIRY_WEEKS, expiryViaSql, 0.01d );

					// Test projection
					Double heightViaHql = (Double) s.createQuery(
							"select p.heightInches from Person p where p.name = 'Emmanuel'" )
							.uniqueResult();
					assertEquals( HEIGHT_INCHES, heightViaHql, 0.01d );
					Double expiryViaHql = (Double) s.createQuery(
							"select e.passwordExpiryDays from Employee e where e.name = 'Steve'" ).uniqueResult();
					assertEquals( PASSWORD_EXPIRY_DAYS, expiryViaHql, 0.01d );

					// Test restriction and entity load via criteria
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Person> personCriteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> personRoot = personCriteria.from( Person.class );
					personCriteria.where( criteriaBuilder.between(
							personRoot.get( "heightInches" ),
							HEIGHT_INCHES - 0.01d,
							HEIGHT_INCHES + 0.01d
					) );

					p = s.createQuery( personCriteria ).uniqueResult();
//					p = (Person) s.createCriteria( Person.class )
//							.add( Restrictions.between( "heightInches", HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d ) )
//							.uniqueResult();
					assertEquals( HEIGHT_INCHES, p.getHeightInches(), 0.01d );

					CriteriaQuery<Employee> employeeCriteria = criteriaBuilder.createQuery( Employee.class );
					Root<Employee> employeeRoot = employeeCriteria.from( Employee.class );
					employeeCriteria.where( criteriaBuilder.between(
							employeeRoot.get( "passwordExpiryDays" ),
							PASSWORD_EXPIRY_DAYS - 0.01d,
							PASSWORD_EXPIRY_DAYS + 0.01d
					) );

					e = s.createQuery( employeeCriteria ).uniqueResult();

//					e = (Employee) s.createCriteria( Employee.class )
//							.add( Restrictions.between(
//									"passwordExpiryDays",
//									PASSWORD_EXPIRY_DAYS - 0.01d,
//									PASSWORD_EXPIRY_DAYS - 0.01d,
//							) )
//							.uniqueResult();
					assertEquals( PASSWORD_EXPIRY_DAYS, e.getPasswordExpiryDays(), 0.01d );

					// Test predicate and entity load via HQL
					p = (Person) s.createQuery( "from Person p where p.heightInches between ?1 and ?2" )
							.setParameter( 1, HEIGHT_INCHES - 0.01d )
							.setParameter( 2, HEIGHT_INCHES + 0.01d )
							.uniqueResult();
					assertEquals( HEIGHT_INCHES, p.getHeightInches(), 0.01d );
					e = (Employee) s.createQuery( "from Employee e where e.passwordExpiryDays between ?1 and ?2" )
							.setParameter( 1, PASSWORD_EXPIRY_DAYS - 0.01d )
							.setParameter( 2, PASSWORD_EXPIRY_DAYS + 0.01d )
							.uniqueResult();
					assertEquals( PASSWORD_EXPIRY_DAYS, e.getPasswordExpiryDays(), 0.01d );

					// Test update
					p.setHeightInches( 1 );
					e.setPasswordExpiryDays( 7 );
					s.flush();
					heightViaSql =
							( (Number) s.createNativeQuery(
									"select height_centimeters from UPerson where name='Emmanuel'" )
									.uniqueResult() )
									.doubleValue();
					assertEquals( 2.54d, heightViaSql, 0.01d );
					expiryViaSql =
							( (Number) s.createNativeQuery( "select pwd_expiry_weeks from UEmployee where person_id=?" )
									.setParameter( 1, e.getId() )
									.uniqueResult()
							).doubleValue();
					assertEquals( 1d, expiryViaSql, 0.01d );
					s.remove( p );
					s.remove( e );
				}
		);
	}
}
