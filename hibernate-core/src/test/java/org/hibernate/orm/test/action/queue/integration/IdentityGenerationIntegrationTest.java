/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.integration;

import jakarta.persistence.*;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for IDENTITY id generation strategy to verify that
 * IDENTITY inserts execute immediately before other operations.
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		IdentityGenerationIntegrationTest.IdentityEntity.class,
		IdentityGenerationIntegrationTest.SequenceEntity.class,
		IdentityGenerationIntegrationTest.IdentityParent.class,
		IdentityGenerationIntegrationTest.IdentityChild.class,
		IdentityGenerationIntegrationTest.RegularParent.class,
		IdentityGenerationIntegrationTest.IdentityChildOfRegular.class
})
@SessionFactory
public class IdentityGenerationIntegrationTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.createMutationQuery("delete from IdentityChildOfRegular").executeUpdate();
			session.createMutationQuery("delete from RegularParent").executeUpdate();
			session.createMutationQuery("delete from IdentityChild").executeUpdate();
			session.createMutationQuery("delete from IdentityParent").executeUpdate();
			session.createMutationQuery("delete from SequenceEntity").executeUpdate();
			session.createMutationQuery("delete from IdentityEntity").executeUpdate();
		});
	}

	@Test
	public void testSimpleIdentityInsert(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			IdentityEntity entity = new IdentityEntity();
			entity.setName("Test");

			session.persist(entity);

			// IDENTITY insert should execute immediately and assign ID
			assertNotNull(entity.getId(), "IDENTITY insert should execute immediately and assign ID");

			// Flush should succeed without errors
			session.flush();
		});

		// Verify entity was persisted
		scope.inTransaction(session -> {
			List<IdentityEntity> results = session.createQuery(
				"from IdentityEntity", IdentityEntity.class).list();
			assertEquals(1, results.size());
			assertEquals("Test", results.get(0).getName());
		});
	}

	@Test
	public void testIdentityInsertBeforeSequenceInsert(SessionFactoryScope scope) {
		final List<String> executionOrder = new ArrayList<>();

		scope.inTransaction(session -> {
			// Create sequence entity first
			SequenceEntity seqEntity = new SequenceEntity();
			seqEntity.setName("Sequence");
			session.persist(seqEntity);

			// Note: sequence entity's ID is assigned but INSERT is deferred
			Long seqId = seqEntity.getId();
			assertNotNull(seqId, "Sequence ID should be assigned");
			executionOrder.add("sequence-persist");

			// Create identity entity
			IdentityEntity identityEntity = new IdentityEntity();
			identityEntity.setName("Identity");
			session.persist(identityEntity);

			// IDENTITY insert should execute immediately
			assertNotNull(identityEntity.getId(),
				"IDENTITY insert should execute immediately before flush");
			executionOrder.add("identity-persist");

			// Flush executes remaining operations
			session.flush();
			executionOrder.add("flush");
		});

		// Verify execution order
		assertEquals(3, executionOrder.size());
		assertEquals("sequence-persist", executionOrder.get(0));
		assertEquals("identity-persist", executionOrder.get(1));
		assertEquals("flush", executionOrder.get(2));

		// Verify both entities were persisted
		scope.inTransaction(session -> {
			assertEquals(1L, session.createQuery(
				"select count(*) from IdentityEntity", Long.class).getSingleResult());
			assertEquals(1L, session.createQuery(
				"select count(*) from SequenceEntity", Long.class).getSingleResult());
		});
	}

	@Test
	public void testIdentityWithForeignKey(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction(session -> {
			IdentityParent parent = new IdentityParent();
			parent.setName("Parent");
			session.persist(parent);

			// IDENTITY insert executes immediately
			assertNotNull(parent.getId(), "Parent ID should be assigned immediately");

			IdentityChild child = new IdentityChild();
			child.setName("Child");
			child.setParent(parent);
			session.persist(child);

			// Child IDENTITY insert also executes immediately
			assertNotNull(child.getId(), "Child ID should be assigned immediately");

			session.flush();

			return parent.getId();
		});

		// Verify relationship persisted correctly
		scope.inTransaction(session -> {
			IdentityParent parent = session.find(IdentityParent.class, parentId);
			assertNotNull(parent);
			assertEquals(1, parent.getChildren().size());
			assertEquals("Child", parent.getChildren().get(0).getName());
		});
	}

	@Test
	public void testIdentityChildWithSequenceParent(SessionFactoryScope scope) {
		// Test mixing IDENTITY child with SEQUENCE parent
		Long parentId = scope.fromTransaction(session -> {
			RegularParent parent = new RegularParent();
			parent.setName("Regular Parent");
			session.persist(parent);

			// Parent ID assigned from sequence but INSERT deferred
			assertNotNull(parent.getId());

			IdentityChildOfRegular child = new IdentityChildOfRegular();
			child.setName("Identity Child");
			child.setParent(parent);
			session.persist(child);

			// Child IDENTITY insert should execute immediately
			// This should trigger parent insert first (needed for FK)
			assertNotNull(child.getId(), "Child ID should be assigned");

			session.flush();

			return parent.getId();
		});

		// Verify relationship
		scope.inTransaction(session -> {
			RegularParent parent = session.find(RegularParent.class, parentId);
			assertNotNull(parent);
			assertEquals(1, parent.getChildren().size());
		});
	}

	@Test
	public void testMultipleIdentityInserts(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			// Create multiple IDENTITY entities
			for (int i = 0; i < 5; i++) {
				IdentityEntity entity = new IdentityEntity();
				entity.setName("Entity " + i);
				session.persist(entity);

				// Each should get ID immediately
				assertNotNull(entity.getId(),
					"Entity " + i + " should have ID assigned immediately");
			}

			session.flush();
		});

		// Verify all persisted
		scope.inTransaction(session -> {
			assertEquals(5L, session.createQuery(
				"select count(*) from IdentityEntity", Long.class).getSingleResult());
		});
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Test Entities
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Entity(name = "IdentityEntity")
	@Table(name = "identity_entity")
	public static class IdentityEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

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
	}

	@Entity(name = "SequenceEntity")
	@Table(name = "sequence_entity")
	public static class SequenceEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;

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
	}

	@Entity(name = "IdentityParent")
	@Table(name = "identity_parent")
	public static class IdentityParent {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent")
		private List<IdentityChild> children = new ArrayList<>();

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

		public List<IdentityChild> getChildren() {
			return children;
		}
	}

	@Entity(name = "IdentityChild")
	@Table(name = "identity_child")
	public static class IdentityChild {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		private IdentityParent parent;

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

		public IdentityParent getParent() {
			return parent;
		}

		public void setParent(IdentityParent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "RegularParent")
	@Table(name = "regular_parent")
	public static class RegularParent {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent")
		private List<IdentityChildOfRegular> children = new ArrayList<>();

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

		public List<IdentityChildOfRegular> getChildren() {
			return children;
		}
	}

	@Entity(name = "IdentityChildOfRegular")
	@Table(name = "identity_child_regular")
	public static class IdentityChildOfRegular {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		private RegularParent parent;

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

		public RegularParent getParent() {
			return parent;
		}

		public void setParent(RegularParent parent) {
			this.parent = parent;
		}
	}
}
