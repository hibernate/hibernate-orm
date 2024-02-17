package org.hibernate.orm.test.bulkid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractMutationStrategyIdTest extends BaseCoreFunctionalTestCase {

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
		final Class<? extends SqmMultiTableMutationStrategy> multiTableBulkIdStrategyClass = getMultiTableBulkIdStrategyClass();
		if ( multiTableBulkIdStrategyClass != null ) {
			configuration.setProperty(
					AvailableSettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY,
					multiTableBulkIdStrategyClass
			);
		}
	}


	protected abstract Class<? extends SqmMultiTableMutationStrategy> getMultiTableBulkIdStrategyClass();

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
			for ( int i = 0; i < entityCount(); i++ ) {
				Doctor doctor = new Doctor();
				doctor.setEmployed( ( i % 2 ) == 0 );
				session.persist( doctor );
			}

			for ( int i = 0; i < entityCount(); i++ ) {
				Engineer engineer = new Engineer();
				engineer.setEmployed( ( i % 2 ) == 0 );
				engineer.setFellow( ( i % 2 ) == 1 );
				session.persist( engineer );
			}
		});
	}

	protected int entityCount() {
		return 10;
	}

	@Test
	public void testUpdate() {
		doInHibernate( this::sessionFactory, session -> {
			int updateCount = session.createQuery( "update Person set name = :name where employed = :employed" )
					.setParameter( "name", "John Doe" )
					.setParameter( "employed", true )
					.executeUpdate();

			assertEquals(entityCount(), updateCount);
		});
	}

	@Test
	public void testDeleteFromPerson() {
		doInHibernate( this::sessionFactory, session -> {
			int updateCount = session.createQuery(
				"delete from Person where employed = :employed" )
			.setParameter( "employed", false )
			.executeUpdate();
			assertEquals( entityCount(), updateCount );
		});
	}

	@Test
	public void testDeleteFromEngineer() {
		doInHibernate( this::sessionFactory, session -> {
			int updateCount = session.createQuery( "delete from Engineer where fellow = :fellow" )
					.setParameter( "fellow", true )
					.executeUpdate();
			assertEquals( entityCount() / 2, updateCount );
		});
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private boolean employed;

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

		public boolean isEmployed() {
			return employed;
		}

		public void setEmployed(boolean employed) {
			this.employed = employed;
		}
	}
	//end::batch-bulk-hql-temp-table-base-class-example[]

	//tag::batch-bulk-hql-temp-table-sub-classes-example[]
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
	//end::batch-bulk-hql-temp-table-sub-classes-example[]

}
