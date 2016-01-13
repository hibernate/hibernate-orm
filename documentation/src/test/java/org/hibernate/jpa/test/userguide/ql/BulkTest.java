package org.hibernate.jpa.test.userguide.ql;

import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.jpa.test.util.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * <code>BulkTest</code> - Bulk JPQL Test
 *
 * @author Vlad Mihalcea
 */
public class BulkTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Customer.class
		};
	}

	@Test
	public void testUpdate() {
		final Calendar calendar = new GregorianCalendar();
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new Customer( "Vlad" ) );
			entityManager.persist( new Customer( "Mihalcea" ) );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			String oldName = "Vlad";
			String newName = "Mr. Vlad";

			String jpqlUpdate = "update Customer c set c.name = :newName where c.name = :oldName";
			int updatedEntities = entityManager.createQuery( jpqlUpdate )
				.setParameter( "oldName", oldName )
				.setParameter( "newName", newName )
				.executeUpdate();
			assertEquals(1, updatedEntities);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			String oldName = "Mr. Vlad";
			String newName = "Vlad";


			String jpqlUpdate = "update Customer set name = :newName where name = :oldName";
			int updatedEntities = entityManager.createQuery( jpqlUpdate )
					.setParameter( "oldName", oldName )
					.setParameter( "newName", newName )
					.executeUpdate();
			assertEquals(1, updatedEntities);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			String oldName = "Vlad";

			String jpqlDelete = "delete Customer c where c.name = :oldName";
			int updatedEntities = entityManager.createQuery( jpqlDelete )
					.setParameter( "oldName", oldName )
					.executeUpdate();
			assertEquals(1, updatedEntities);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			String oldName = "Mihalcea";

			String jpqlDelete = "delete Customer where name = :oldName";
			int updatedEntities = entityManager.createQuery( jpqlDelete )
					.setParameter( "oldName", oldName )
					.executeUpdate();
			assertEquals(1, updatedEntities);
		} );
	}

	@Entity(name = "Customer")
	public static class Customer {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Customer() {
		}

		public Customer(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
