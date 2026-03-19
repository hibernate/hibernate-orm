/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.bundling;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests collection operation bundling functionality.
 * When bundling is enabled, collection row operations should be grouped
 * into a single PlannedOperation with a bundled BindPlan.
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		CollectionBundlingTest.Person.class,
		CollectionBundlingTest.Company.class
})
public class CollectionBundlingTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.createMutationQuery("delete from Person").executeUpdate();
			session.createMutationQuery("delete from Company").executeUpdate();
		});
	}

	/**
	 * Test with bundling DISABLED (default behavior).
	 * Each collection entry should create a separate PlannedOperation.
	 */
	@Test
	@SessionFactory(
			useCollectingStatementInspector = true,
			generateStatistics = true
	)
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph")
			// bundling NOT enabled - default is false
	})
	public void testCollectionOperationsWithoutBundling(SessionFactoryScope scope) {
		// Insert person with collection
		scope.inTransaction(session -> {
			Person person = new Person();
			person.id = 1L;
			person.name = "John Doe";
			person.emails = new ArrayList<>();
			person.emails.add("john@example.com");
			person.emails.add("john.doe@example.com");
			person.emails.add("johndoe@work.com");
			session.persist(person);
		});

		// Verify data was inserted
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 1L);
			assertNotNull(person);
			assertEquals(3, person.emails.size());
			assertTrue(person.emails.contains("john@example.com"));
			assertTrue(person.emails.contains("john.doe@example.com"));
			assertTrue(person.emails.contains("johndoe@work.com"));
		});

		// Update collection - add and remove entries
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 1L);
			person.emails.remove("john.doe@example.com"); // delete
			person.emails.add("newemail@example.com");     // insert
			// john@example.com and johndoe@work.com remain
		});

		// Verify updates
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 1L);
			assertEquals(3, person.emails.size());
			assertTrue(person.emails.contains("john@example.com"));
			assertTrue(person.emails.contains("johndoe@work.com"));
			assertTrue(person.emails.contains("newemail@example.com"));
			assertFalse(person.emails.contains("john.doe@example.com"));
		});
	}

	/**
	 * Test with bundling ENABLED.
	 * Collection entries should be bundled into fewer PlannedOperations.
	 */
	@Test
	@SessionFactory(
			useCollectingStatementInspector = true,
			generateStatistics = true
	)
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void testCollectionOperationsWithBundling(SessionFactoryScope scope) {
		// Insert person with collection
		scope.inTransaction(session -> {
			Person person = new Person();
			person.id = 2L;
			person.name = "Jane Smith";
			person.emails = new ArrayList<>();
			person.emails.add("jane@example.com");
			person.emails.add("jane.smith@example.com");
			person.emails.add("jsmith@work.com");
			session.persist(person);
		});

		// Verify data was inserted
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 2L);
			assertNotNull(person);
			assertEquals(3, person.emails.size());
			assertTrue(person.emails.contains("jane@example.com"));
			assertTrue(person.emails.contains("jane.smith@example.com"));
			assertTrue(person.emails.contains("jsmith@work.com"));
		});

		// Update collection - add and remove entries
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 2L);
			person.emails.remove("jane.smith@example.com"); // delete
			person.emails.add("newemail@example.com");      // insert
			// jane@example.com and jsmith@work.com remain
		});

		// Verify updates
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 2L);
			assertEquals(3, person.emails.size());
			assertTrue(person.emails.contains("jane@example.com"));
			assertTrue(person.emails.contains("jsmith@work.com"));
			assertTrue(person.emails.contains("newemail@example.com"));
			assertFalse(person.emails.contains("jane.smith@example.com"));
		});
	}

	/**
	 * Test large collection to verify bundling works with many entries.
	 */
	@Test
	@SessionFactory(
			useCollectingStatementInspector = true,
			generateStatistics = true
	)
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true"),
			@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "20")
	})
	public void testLargeCollectionWithBundling(SessionFactoryScope scope) {
		final int collectionSize = 100;

		// Insert person with large collection
		scope.inTransaction(session -> {
			Person person = new Person();
			person.id = 3L;
			person.name = "Bob Large";
			person.emails = new ArrayList<>();
			for (int i = 0; i < collectionSize; i++) {
				person.emails.add("email" + i + "@example.com");
			}
			session.persist(person);
		});

		// Verify all entries were inserted
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 3L);
			assertNotNull(person);
			assertEquals(collectionSize, person.emails.size());
			assertTrue(person.emails.contains("email0@example.com"));
			assertTrue(person.emails.contains("email50@example.com"));
			assertTrue(person.emails.contains("email99@example.com"));
		});

		// Update collection - remove half, add new ones
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 3L);
			// Remove even numbered emails
			person.emails.removeIf(email -> {
				String numStr = email.substring("email".length(), email.indexOf("@"));
				int num = Integer.parseInt(numStr);
				return num % 2 == 0;
			});
			// Add new emails
			for (int i = 0; i < 25; i++) {
				person.emails.add("newemail" + i + "@example.com");
			}
		});

		// Verify updates
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 3L);
			assertEquals(75, person.emails.size()); // 50 odd + 25 new
			assertTrue(person.emails.contains("email1@example.com")); // odd, kept
			assertTrue(person.emails.contains("email51@example.com")); // odd, kept
			assertFalse(person.emails.contains("email0@example.com")); // even, removed
			assertFalse(person.emails.contains("email50@example.com")); // even, removed
			assertTrue(person.emails.contains("newemail0@example.com")); // new
			assertTrue(person.emails.contains("newemail24@example.com")); // new
		});
	}

	/**
	 * Test Set collection (instead of List).
	 */
	@Test
	@SessionFactory(
			useCollectingStatementInspector = true,
			generateStatistics = true
	)
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void testSetCollectionWithBundling(SessionFactoryScope scope) {
		// Insert company with departments (Set)
		scope.inTransaction(session -> {
			Company company = new Company();
			company.id = 1L;
			company.name = "Acme Corp";
			company.departments = new HashSet<>();
			company.departments.add("Engineering");
			company.departments.add("Sales");
			company.departments.add("Marketing");
			session.persist(company);
		});

		// Verify data
		scope.inTransaction(session -> {
			Company company = session.find(Company.class, 1L);
			assertNotNull(company);
			assertEquals(3, company.departments.size());
			assertTrue(company.departments.contains("Engineering"));
			assertTrue(company.departments.contains("Sales"));
			assertTrue(company.departments.contains("Marketing"));
		});

		// Update collection
		scope.inTransaction(session -> {
			Company company = session.find(Company.class, 1L);
			company.departments.remove("Sales");
			company.departments.add("HR");
			company.departments.add("Finance");
		});

		// Verify updates
		scope.inTransaction(session -> {
			Company company = session.find(Company.class, 1L);
			assertEquals(4, company.departments.size());
			assertTrue(company.departments.contains("Engineering"));
			assertTrue(company.departments.contains("Marketing"));
			assertTrue(company.departments.contains("HR"));
			assertTrue(company.departments.contains("Finance"));
			assertFalse(company.departments.contains("Sales"));
		});
	}

	/**
	 * Test empty collection operations.
	 */
	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void testEmptyCollectionWithBundling(SessionFactoryScope scope) {
		// Insert person with empty collection
		scope.inTransaction(session -> {
			Person person = new Person();
			person.id = 4L;
			person.name = "Empty Person";
			person.emails = new ArrayList<>();
			session.persist(person);
		});

		// Verify
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 4L);
			assertNotNull(person);
			assertNotNull(person.emails);
			assertTrue(person.emails.isEmpty());
		});

		// Add entries to previously empty collection
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 4L);
			person.emails.add("first@example.com");
			person.emails.add("second@example.com");
		});

		// Verify
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 4L);
			assertEquals(2, person.emails.size());
		});

		// Clear collection
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 4L);
			person.emails.clear();
		});

		// Verify
		scope.inTransaction(session -> {
			Person person = session.find(Person.class, 4L);
			assertTrue(person.emails.isEmpty());
		});
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		Long id;

		String name;

		@ElementCollection
		List<String> emails;
	}

	@Entity(name = "Company")
	public static class Company {
		@Id
		Long id;

		String name;

		@ElementCollection
		Set<String> departments;
	}
}
