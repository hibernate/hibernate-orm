/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.jdbc.AbstractWork;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/join/Person.hbm.xml"
)
@SessionFactory
public class JoinTest {

	@Test
	@SkipForDialect( dialectClass = AltibaseDialect.class, reason = "In line view in left join is not possible in Altibase")
	public void testSequentialSelects(SessionFactoryScope scope) {
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

//					assertEquals( s.createQuery("from java.io.Serializable").list().size(), 0 );

					assertEquals( 3, s.createQuery( "from Person" ).list().size() );
					assertEquals( 1, s.createQuery( "from Person p where p.class is null" ).list().size() );
					assertEquals( 1, s.createQuery( "from Person p where p.class = Customer" ).list().size() );
					assertEquals( 1, s.createQuery( "from Customer c" ).list().size() );
					s.clear();

					List customers = s.createQuery( "from Customer c left join fetch c.salesperson" ).list();
					for ( Iterator iter = customers.iterator(); iter.hasNext(); ) {
						Customer c = (Customer) iter.next();
						assertTrue( Hibernate.isInitialized( c.getSalesperson() ) );
						assertEquals( "Mark", c.getSalesperson().getName() );
					}
					assertEquals( 1, customers.size() );
					s.clear();

					customers = s.createQuery( "from Customer" ).list();
					for ( Iterator iter = customers.iterator(); iter.hasNext(); ) {
						Customer c = (Customer) iter.next();
						assertFalse( Hibernate.isInitialized( c.getSalesperson() ) );
						assertEquals( "Mark", c.getSalesperson().getName() );
					}
					assertEquals( 1, customers.size() );
					s.clear();


					mark = s.get( Employee.class, new Long( mark.getId() ) );
					joe = s.get( Customer.class, new Long( joe.getId() ) );

					mark.setZip( "30306" );
					assertEquals( 1, s.createQuery( "from Person p where p.zip = '30306'" ).list().size() );
					s.remove( mark );
					s.remove( joe );
					s.remove( yomomma );
					assertTrue( s.createQuery( "from Person" ).list().isEmpty() );
				}
		);
	}

	@Test
	public void testSequentialSelectsOptionalData(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					User jesus = new User();
					jesus.setName( "Jesus Olvera y Martinez" );
					jesus.setSex( 'M' );

					s.persist( jesus );

//					assertEquals( 0, s.createQuery("from java.io.Serializable").list().size() );

					assertEquals( 1, s.createQuery( "from Person" ).list().size() );
					assertEquals( 0, s.createQuery( "from Person p where p.class is null" ).list().size() );
					assertEquals( 1, s.createQuery( "from Person p where p.class = User" ).list().size() );
					assertTrue( s.createQuery( "from User u" ).list().size() == 1 );
					s.clear();

					// Remove the optional row from the join table and requery the User obj
					doWork( s );
					s.clear();

					jesus = (User) s.get( Person.class, new Long( jesus.getId() ) );
					s.clear();

					// Cleanup the test data
					s.remove( jesus );

					assertTrue( s.createQuery( "from Person" ).list().isEmpty() );

				}
		);
	}

	private void doWork(final Session s) {
		s.doWork(
				new AbstractWork() {
					@Override
					public void execute(Connection connection) throws SQLException {
						try (PreparedStatement ps = connection.prepareStatement( "delete from t_user" )) {
							ps.execute();
						}
					}
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
					User u = new User();
					u.setName( "Steve" );
					u.setSex( 'M' );
					u.setPasswordExpiryDays( PASSWORD_EXPIRY_DAYS );
					s.persist( u );
					s.flush();

					// Test value conversion during insert
					// Oracle returns BigDecimaal while other dialects return Double;
					// casting to Number so it works on all dialects
					Number heightViaSql = (Number) s.createNativeQuery(
							"select height_centimeters from person where name='Emmanuel'" ).uniqueResult();
					assertEquals( HEIGHT_CENTIMETERS, heightViaSql.doubleValue(), 0.01d );
					Number expiryViaSql = (Number) s.createNativeQuery(
							"select pwd_expiry_weeks from t_user where person_id=?" )
							.setParameter( 1, u.getId() )
							.uniqueResult();
					assertEquals( PASSWORD_EXPIRY_WEEKS, expiryViaSql.doubleValue(), 0.01d );

					// Test projection
					Double heightViaHql = (Double) s.createQuery(
							"select p.heightInches from Person p where p.name = 'Emmanuel'" ).uniqueResult();
					assertEquals( HEIGHT_INCHES, heightViaHql, 0.01d );
					Double expiryViaHql = (Double) s.createQuery(
							"select u.passwordExpiryDays from User u where u.name = 'Steve'" ).uniqueResult();
					assertEquals( PASSWORD_EXPIRY_DAYS, expiryViaHql, 0.01d );

					// Test restriction and entity load via criteria
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Person> personCriteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> rootPerson = personCriteria.from( Person.class );
					personCriteria.where( criteriaBuilder.between(
							rootPerson.get( "heightInches" ),
							HEIGHT_INCHES - 0.01d,
							HEIGHT_INCHES + 0.01d
					) );
					p = s.createQuery( personCriteria ).uniqueResult();
//					p = (Person)s.createCriteria(Person.class)
//							.add(Restrictions.between("heightInches", HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d))
//							.uniqueResult();
					assertEquals( HEIGHT_INCHES, p.getHeightInches(), 0.01d );
					CriteriaQuery<User> userCriteria = criteriaBuilder.createQuery( User.class );
					Root<User> userRoot = userCriteria.from( User.class );
					userCriteria.where( criteriaBuilder.between(
							userRoot.get( "passwordExpiryDays" ),
							PASSWORD_EXPIRY_DAYS - 0.01d,
							PASSWORD_EXPIRY_DAYS + 0.01d
					) );
					u = s.createQuery( userCriteria ).uniqueResult();
//					u = (User)s.createCriteria(User.class)
//							.add(Restrictions.between("passwordExpiryDays", PASSWORD_EXPIRY_DAYS - 0.01d, PASSWORD_EXPIRY_DAYS + 0.01d))
//							.uniqueResult();
					assertEquals( PASSWORD_EXPIRY_DAYS, u.getPasswordExpiryDays(), 0.01d );

					// Test predicate and entity load via HQL
					p = (Person) s.createQuery( "from Person p where p.heightInches between ?1 and ?2" )
							.setParameter( 1, HEIGHT_INCHES - 0.01d )
							.setParameter( 2, HEIGHT_INCHES + 0.01d )
							.uniqueResult();
					assertNotNull( p );
					assertEquals( 0.01d, HEIGHT_INCHES, p.getHeightInches() );
					u = (User) s.createQuery( "from User u where u.passwordExpiryDays between ?1 and ?2" )
							.setParameter( 1, PASSWORD_EXPIRY_DAYS - 0.01d )
							.setParameter( 2, PASSWORD_EXPIRY_DAYS + 0.01d )
							.uniqueResult();
					assertEquals( PASSWORD_EXPIRY_DAYS, u.getPasswordExpiryDays(), 0.01d );

					// Test update
					p.setHeightInches( 1 );
					u.setPasswordExpiryDays( 7d );
					s.flush();
					heightViaSql = (Number) s.createNativeQuery(
							"select height_centimeters from person where name='Emmanuel'" ).uniqueResult();
					assertEquals( 2.54d, heightViaSql.doubleValue(), 0.01d );
					expiryViaSql = (Number) s.createNativeQuery( "select pwd_expiry_weeks from t_user where person_id=?" )
							.setParameter( 1, u.getId() )
							.uniqueResult();
					assertEquals( 1d, expiryViaSql.doubleValue(), 0.01d );

					s.remove( p );
					s.remove( u );
					assertTrue( s.createQuery( "from Person" ).list().isEmpty() );

				}
		);
	}
}
