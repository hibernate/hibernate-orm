package org.hibernate.test.bytecode.enhancement.cascade;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Bolek Ziobrowski
 * @author Gail Badner
 */
@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue(jiraKey = "HHH-13129")
public class CascadeOnUninitializedTest extends BaseNonConfigCoreFunctionalTestCase {
	private SQLStatementInterceptor sqlInterceptor;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Address.class };
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.FORMAT_SQL, "true" );
		sqlInterceptor = new SQLStatementInterceptor( settings );
	}

	@Test
	public void testMergeDetachedEnhancedEntityWithUninitializedManyToOne() {
		final Person person = persistPersonWithManyToOne();

		sqlInterceptor.clear();

		// get a detached Person
		final Person detachedPerson = fromTransaction(
				session -> session.get( Person.class, person.getId() )
		);

		// loading Person should lead to one SQL
		assertThat( sqlInterceptor.getQueryCount(), is( 1 ) );

		// primaryAddress should be "initialized" as an enhanced-proxy
		assertTrue( Hibernate.isPropertyInitialized( detachedPerson, "primaryAddress" ) );
		assertThat( detachedPerson.getPrimaryAddress(), not( instanceOf( HibernateProxy.class ) ) );
		assertFalse( Hibernate.isInitialized( detachedPerson.getPrimaryAddress() ) );

		// alter the detached reference
		detachedPerson.setName( "newName" );

		final Person mergedPerson = fromTransaction(
				session -> (Person) session.merge( detachedPerson )
		);

		// 1) select Person#addresses
		// 2) select Person#primaryAddress
		// 3) update Person

		assertThat( sqlInterceptor.getQueryCount(), is( 3 ) );

		// primaryAddress should not be initialized
		assertTrue( Hibernate.isPropertyInitialized( detachedPerson, "primaryAddress" ) );
		assertThat( detachedPerson.getPrimaryAddress(), not( instanceOf( HibernateProxy.class ) ) );
		assertFalse( Hibernate.isInitialized( detachedPerson.getPrimaryAddress() ) );

		assertEquals( "newName", mergedPerson.getName() );
	}

	@Test
	public void testDeleteEnhancedEntityWithUninitializedManyToOne() {
		Person person = persistPersonWithManyToOne();

		sqlInterceptor.clear();

		// get a detached Person
		Person detachedPerson = fromTransaction(
				session -> session.get( Person.class, person.getId() )
		);

		// loading Person should lead to one SQL
		assertThat( sqlInterceptor.getQueryCount(), is( 1 ) );

		// primaryAddress should be initialized as an enhance-proxy
		assertTrue( Hibernate.isPropertyInitialized( detachedPerson, "primaryAddress" ) );
		assertThat( detachedPerson, not( instanceOf( HibernateProxy.class ) ) );
		assertFalse( Hibernate.isInitialized( detachedPerson.getPrimaryAddress() ) );

		sqlInterceptor.clear();

		// deleting detachedPerson should result in detachedPerson.primaryAddress being initialized,
		// so that the DELETE operation can be cascaded to it.
		inTransaction(
				session -> session.delete( detachedPerson )
		);

		// 1) select Person#addresses
		// 2) select Person#primaryAddress
		// 3) delete Person
		// 4) select primary Address

		assertThat( sqlInterceptor.getQueryCount(), is( 4 ) );

		// both the Person and its Address should be deleted
		inTransaction(
				session -> {
					assertNull( session.get( Person.class, person.getId() ) );
					assertNull( session.get( Person.class, person.getPrimaryAddress().getId() ) );
				}
		);
	}

	@Test
	public void testMergeDetachedEnhancedEntityWithUninitializedOneToMany() {

		Person person = persistPersonWithOneToMany();

		// get a detached Person
		Person detachedPerson = TransactionUtil.doInHibernate(
				this::sessionFactory, session -> {
					return session.get( Person.class, person.getId() );
				}
		);

		// address should not be initialized
		assertFalse( Hibernate.isPropertyInitialized( detachedPerson, "addresses" ) );
		detachedPerson.setName( "newName" );

		Person mergedPerson = TransactionUtil.doInHibernate(
				this::sessionFactory, session -> {
					return (Person) session.merge( detachedPerson );
				}
		);

		// address should be initialized
		assertTrue( Hibernate.isPropertyInitialized( mergedPerson, "addresses" ) );
		assertEquals( "newName", mergedPerson.getName() );
	}

	@Test
	public void testDeleteEnhancedEntityWithUninitializedOneToMany() {
		Person person = persistPersonWithOneToMany();

		// get a detached Person
		Person detachedPerson = TransactionUtil.doInHibernate(
				this::sessionFactory, session -> {
					return session.get( Person.class, person.getId() );
				}
		);

		// address should not be initialized
		assertFalse( Hibernate.isPropertyInitialized( detachedPerson, "addresses" ) );

		// deleting detachedPerson should result in detachedPerson.address being initialized,
		// so that the DELETE operation can be cascaded to it.
		TransactionUtil.doInHibernate(
				this::sessionFactory, session -> {
					session.delete( detachedPerson );
				}
		);

		// both the Person and its Address should be deleted
		TransactionUtil.doInHibernate(
				this::sessionFactory, session -> {
					assertNull( session.get( Person.class, person.getId() ) );
					assertNull( session.get( Person.class, person.getAddresses().iterator().next().getId() ) );
				}
		);
	}

	public Person persistPersonWithManyToOne() {
		Address address = new Address();
		address.setDescription( "ABC" );

		final Person person = new Person();
		person.setName( "John Doe" );
		person.setPrimaryAddress( address );

		TransactionUtil.doInHibernate(
				this::sessionFactory, session -> {
					session.persist( person );
				}
		);

		return person;
	}

	public Person persistPersonWithOneToMany() {
		Address address = new Address();
		address.setDescription( "ABC" );

		final Person person = new Person();
		person.setName( "John Doe" );
		person.getAddresses().add( address );

		TransactionUtil.doInHibernate(
				this::sessionFactory, session -> {
					session.persist( person );
				}
		);

		return person;
	}

	@Entity
	@Table(name = "TEST_PERSON")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "NAME", length = 300, nullable = true)
		private String name;

		@ManyToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
		@JoinColumn(name = "primary_address_id")
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private Address primaryAddress;

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn( name = "person_id" )
		private Set<Address> addresses = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Address getPrimaryAddress() {
			return primaryAddress;
		}

		public void setPrimaryAddress(Address primaryAddress) {
			this.primaryAddress = primaryAddress;
		}

		public Set<Address> getAddresses() {
			return addresses;
		}

		public void setAddresses(Set<Address> addresses) {
			this.addresses = addresses;
		}
	}

	@Entity
	@Table(name = "TEST_ADDRESS")
	public static class Address {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "DESCRIPTION", length = 300, nullable = true)
		private String description;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}


