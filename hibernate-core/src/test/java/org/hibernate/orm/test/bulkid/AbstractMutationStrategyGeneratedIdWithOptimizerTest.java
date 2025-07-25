/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractMutationStrategyGeneratedIdWithOptimizerTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Doctor.class,
				Engineer.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		Class<? extends SqmMultiTableInsertStrategy> insertStrategyClass = getMultiTableInsertStrategyClass();
		if ( insertStrategyClass != null ) {
			configuration.setProperty( AvailableSettings.QUERY_MULTI_TABLE_INSERT_STRATEGY, insertStrategyClass );
		}
	}

	protected abstract Class<? extends SqmMultiTableInsertStrategy> getMultiTableInsertStrategyClass();

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected boolean isCleanupTestDataUsingBulkDelete() {
		return true;
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			Doctor doctor = new Doctor();
			doctor.setName( "Doctor John" );
			doctor.setEmployed( true );
			session.persist( doctor );
		});
	}

	@Test
	public void testInsertStatic() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "insert into Engineer(id, name, employed, fellow) values (0, :name, :employed, false)" )
					.setParameter( "name", "John Doe" )
					.setParameter( "employed", true )
					.executeUpdate();

			final Engineer engineer = session.find( Engineer.class, 0 );
			assertEquals( "John Doe", engineer.getName() );
			assertTrue( engineer.isEmployed() );
			assertFalse( engineer.isFellow() );
		});
	}

	@Test
	public void testInsertGenerated() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "insert into Engineer(name, employed, fellow) values (:name, :employed, false)" )
					.setParameter( "name", "John Doe" )
					.setParameter( "employed", true )
					.executeUpdate();
			final Engineer engineer = session.createQuery( "from Engineer e where e.name = 'John Doe'", Engineer.class )
					.getSingleResult();
			assertEquals( "John Doe", engineer.getName() );
			assertTrue( engineer.isEmployed() );
			assertFalse( engineer.isFellow() );
		});
	}

	@Test
	public void testInsertSelectStatic() {
		doInHibernate( this::sessionFactory, session -> {
			final int insertCount = session.createQuery( "insert into Engineer(id, name, employed, fellow) "
														+ "select d.id + 1, 'John Doe', true, false from Doctor d" )
					.executeUpdate();

			final Engineer engineer = session.createQuery( "from Engineer e where e.name = 'John Doe'", Engineer.class )
					.getSingleResult();
			assertEquals( 1, insertCount );
			assertEquals( "John Doe", engineer.getName() );
			assertTrue( engineer.isEmployed() );
			assertFalse( engineer.isFellow() );
		});
	}

	@Test
	public void testInsertSelectGenerated() {
		doInHibernate( this::sessionFactory, session -> {
			final int insertCount = session.createQuery( "insert into Engineer(name, employed, fellow) "
														+ "select 'John Doe', true, false from Doctor d" )
					.executeUpdate();
			final Engineer engineer = session.createQuery( "from Engineer e where e.name = 'John Doe'", Engineer.class )
					.getSingleResult();
			assertEquals( 1, insertCount );
			assertEquals( "John Doe", engineer.getName() );
			assertTrue( engineer.isEmployed() );
			assertFalse( engineer.isFellow() );
		});
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Integer id;

		private String name;

		private boolean employed;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isEmployed() {
			return employed;
		}

		public void setEmployed(boolean employed) {
			this.employed = employed;
		}
	}
	@Entity(name = "Doctor")
	public static class Doctor extends Person {
	}

	@Entity(name = "Engineer")
	public static class Engineer extends Person {

		private boolean fellow;

		public boolean isFellow() {
			return fellow;
		}

		public void setFellow(boolean fellow) {
			this.fellow = fellow;
		}
	}

}
