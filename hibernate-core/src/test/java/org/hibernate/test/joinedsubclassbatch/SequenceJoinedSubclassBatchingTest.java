/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinedsubclassbatch;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * Test batching of insert,update,delete on joined subclasses using SEQUENCE
 *
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12968\n")
@RequiresDialectFeature(DialectChecks.SupportsSequences.class)
public class SequenceJoinedSubclassBatchingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Employee.class,
				Customer.class
		};
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "20" );
	}

	@Test
	public void doBatchInsertUpdateJoinedSubclassNrEqualWithBatch() {
		doBatchInsertUpdateJoined( 20, 20 );
	}

	@Test
	public void doBatchInsertUpdateJoinedSubclassNrLessThenBatch() {
		doBatchInsertUpdateJoined( 19, 20 );
	}

	@Test
	public void doBatchInsertUpdateJoinedSubclassNrBiggerThenBatch() {
		doBatchInsertUpdateJoined( 21, 20 );
	}

	@Test
	public void testBatchInsertUpdateSizeEqJdbcBatchSize() {
		int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
		doBatchInsertUpdateJoined( 50, batchSize );
	}

	@Test
	public void testBatchInsertUpdateSizeLtJdbcBatchSize() {
		int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
		doBatchInsertUpdateJoined( 50, batchSize - 1 );
	}

	@Test
	public void testBatchInsertUpdateSizeGtJdbcBatchSize() {
		int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
		doBatchInsertUpdateJoined( 50, batchSize + 1 );
	}

	public void doBatchInsertUpdateJoined(int nEntities, int nBeforeFlush) {

		doInHibernate( this::sessionFactory, s -> {
			for ( int i = 0; i < nEntities; i++ ) {
				Employee e = new Employee();
				e.getId();
				e.setName( "Mark" );
				e.setTitle( "internal sales" );
				e.setSex( 'M' );
				e.setAddress( "buckhead" );
				e.setZip( "30305" );
				e.setCountry( "USA" );
				s.save( e );
				if ( i % nBeforeFlush == 0 && i > 0 ) {
					s.flush();
					s.clear();
				}
			}
		} );

		doInHibernate( this::sessionFactory, s -> {
			int i = 0;
			ScrollableResults sr = s.createQuery(
				"select e from Employee e" )
			.scroll( ScrollMode.FORWARD_ONLY );

			while ( sr.next() ) {
				Employee e = (Employee) sr.get( 0 );
				e.setTitle( "Unknown" );
			}
		} );

		doInHibernate( this::sessionFactory, s -> {
			int i = 0;
			ScrollableResults sr = s.createQuery(
				"select e from Employee e" )
			.scroll( ScrollMode.FORWARD_ONLY );

			while ( sr.next() ) {
				Employee e = (Employee) sr.get( 0 );
				s.delete( e );
			}
		} );
	}

	@Test
	public void testAssertSubclassInsertedSuccessfullyAfterFlush() {

		doInHibernate( this::sessionFactory, s -> {
			Employee e = new Employee();
			e.setName( "Mark" );
			e.setTitle( "internal sales" );
			e.setSex( 'M' );
			e.setAddress( "buckhead" );
			e.setZip( "30305" );
			e.setCountry( "USA" );
			s.save( e );
			s.flush();

			long numberOfInsertedEmployee = (long) s.createQuery( "select count(e) from Employee e" ).uniqueResult();
			Assert.assertEquals( 1L, numberOfInsertedEmployee );
		} );


		doInHibernate( this::sessionFactory, s -> {
			int i = 0;
			ScrollableResults sr = s.createQuery(
					"select e from Employee e" )
					.scroll( ScrollMode.FORWARD_ONLY );

			while ( sr.next() ) {
				Employee e = (Employee) sr.get( 0 );
				s.delete( e );
			}
		} );

	}

	@Embeddable
	public static class Address implements Serializable {

		public String address;

		public String zip;

		public String country;

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getZip() {
			return zip;
		}

		public void setZip(String zip) {
			this.zip = zip;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}
	}

	@Entity(name = "Customer")
	public static class Customer extends Person {

		@ManyToOne(fetch = FetchType.LAZY)
		private Employee salesperson;

		private String comments;

		public Employee getSalesperson() {
			return salesperson;
		}

		public void setSalesperson(Employee salesperson) {
			this.salesperson = salesperson;
		}

		public String getComments() {
			return comments;
		}

		public void setComments(String comments) {
			this.comments = comments;
		}
	}

	@Entity(name = "Employee")
	public static class Employee extends Person {

		@Column(nullable = false, length = 20)
		private String title;

		private BigDecimal salary;

		private double passwordExpiryDays;

		@ManyToOne(fetch = FetchType.LAZY)
		private Employee manager;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Employee getManager() {
			return manager;
		}

		public void setManager(Employee manager) {
			this.manager = manager;
		}

		public BigDecimal getSalary() {
			return salary;
		}

		public void setSalary(BigDecimal salary) {
			this.salary = salary;
		}

		public double getPasswordExpiryDays() {
			return passwordExpiryDays;
		}

		public void setPasswordExpiryDays(double passwordExpiryDays) {
			this.passwordExpiryDays = passwordExpiryDays;
		}
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		@Column(nullable = false, length = 80)
		private String name;

		@Column(nullable = false, updatable = false)
		private char sex;

		@javax.persistence.Version
		private int version;

		private double heightInches;

		@Embedded
		private Address address = new Address();

		public Address getAddress() {
			return address;
		}

		public void setAddress(String string) {
			this.address.address = string;
		}

		public void setZip(String string) {
			this.address.zip = string;
		}

		public void setCountry(String string) {
			this.address.country = string;
		}

		public char getSex() {
			return sex;
		}

		public void setSex(char sex) {
			this.sex = sex;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String identity) {
			this.name = identity;
		}

		public double getHeightInches() {
			return heightInches;
		}

		public void setHeightInches(double heightInches) {
			this.heightInches = heightInches;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}
	}
}

