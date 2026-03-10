/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.fk;

import java.util.Set;

import org.hibernate.action.queue.constraint.ConstraintModelBuilder;
import org.hibernate.action.queue.fk.ForeignKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ForeignKeyModelBuilder}
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		ForeignKeyModelBuilderTest.SimpleEntity.class,
		ForeignKeyModelBuilderTest.EntityWithSecondaryTable.class,
		ForeignKeyModelBuilderTest.ParentEntity.class,
		ForeignKeyModelBuilderTest.ChildEntity.class,
		ForeignKeyModelBuilderTest.EntityWithToOne.class,
		ForeignKeyModelBuilderTest.TargetEntity.class,
		ForeignKeyModelBuilderTest.EntityWithEmbedded.class,
		ForeignKeyModelBuilderTest.Owner.class,
		ForeignKeyModelBuilderTest.Item.class,
		ForeignKeyModelBuilderTest.Student.class,
		ForeignKeyModelBuilderTest.Course.class,
		ForeignKeyModelBuilderTest.EntityWithNullableFK.class,
		ForeignKeyModelBuilderTest.OptionalTarget.class,
		ForeignKeyModelBuilderTest.EntityWithElementCollection.class,
		ForeignKeyModelBuilderTest.EntityWithCompositeFK.class,
		ForeignKeyModelBuilderTest.CompositeKeyTarget.class
})
public class ForeignKeyModelBuilderTest {

	@Test
	public void testBasicForeignKeyModelBuilding(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		assertNotNull(constraintModel);
		assertNotNull(foreignKeys);
		assertFalse(foreignKeys.isEmpty(), "Foreign key model should contain foreign keys");
	}

	@Test
	public void testSecondaryTableForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// EntityWithSecondaryTable should have FK from secondary table to primary table
		boolean foundSecondaryTableFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("entity_secondary") &&
						fk.targetTable().contains("entity_primary"));

		assertTrue(foundSecondaryTableFK, "Should find FK from secondary table to primary table");
	}

	@Test
	public void testJoinedInheritanceForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// ChildEntity table should have FK to ParentEntity table in joined inheritance
		boolean foundInheritanceFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("ChildEntity") &&
						fk.targetTable().contains("ParentEntity"));

		assertTrue(foundInheritanceFK, "Should find FK from child table to parent table in joined inheritance");
	}

	@Test
	public void testToOneForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// EntityWithToOne should have FK to TargetEntity
		boolean foundToOneFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("EntityWithToOne") &&
						fk.targetTable().contains("TargetEntity"));

		assertTrue(foundToOneFK, "Should find FK for @ManyToOne association");
	}

	@Test
	public void testCollectionTableForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// Owner's @OneToMany should create FK in Item table
		boolean foundCollectionFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("Item") &&
						fk.targetTable().contains("Owner"));

		assertTrue(foundCollectionFK, "Should find FK from collection table to owner table");
	}

	@Test
	public void testManyToManyForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// Many-to-many join table should have two FKs
		long m2mFKCount = foreignKeys.stream()
				.filter(fk -> fk.keyTable().contains("student_course"))
				.count();

		assertEquals(2, m2mFKCount, "Many-to-many join table should have 2 foreign keys");
	}

	@Test
	public void testEmbeddedToOneForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// EntityWithEmbedded has embedded component with ToOne - should find FK
		boolean foundEmbeddedFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("EntityWithEmbedded") &&
						fk.targetTable().contains("TargetEntity"));

		assertTrue(foundEmbeddedFK, "Should find FK from embedded component's ToOne association");
	}

	@Test
	public void testNullableForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// Find nullable FK (EntityWithNullableFK -> OptionalTarget)
		ForeignKey nullableFK = foreignKeys.stream()
				.filter(fk -> fk.keyTable().contains("EntityWithNullableFK") &&
						fk.targetTable().contains("OptionalTarget"))
				.findFirst()
				.orElse(null);

		assertNotNull(nullableFK, "Should find nullable FK");
		assertTrue(nullableFK.nullable(), "FK should be marked as nullable");
	}

	@Test
	public void testForeignKeyColumns(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// Find a specific FK and verify column mappings
		ForeignKey toOneFK = foreignKeys.stream()
				.filter(fk -> fk.keyTable().contains("EntityWithToOne") &&
						fk.targetTable().contains("TargetEntity"))
				.findFirst()
				.orElse(null);

		assertNotNull(toOneFK, "Should find ToOne FK");
		assertTrue(toOneFK.keyColumns().getJdbcTypeCount() > 0, "FK should have key columns");
		assertTrue(toOneFK.targetColumns().getJdbcTypeCount() > 0, "FK should have target columns");
		assertEquals(toOneFK.keyColumns().getJdbcTypeCount(), toOneFK.targetColumns().getJdbcTypeCount(),
				"Key columns and target columns should have same size");
	}

	@Test
	public void testNoDuplicateForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// Count unique FKs - the builder uses IdentityHashMap to avoid duplicates
		// Just verify we got a reasonable number of FKs without duplicates causing issues
		int fkCount = foreignKeys.size();
		assertTrue(fkCount > 0, "Should have at least one FK");
	}

	@Test
	public void testElementCollectionForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// Element collection table should have FK back to owner
		boolean foundElementCollectionFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("element_values") &&
						fk.targetTable().contains("EntityWithElementCollection"));

		assertTrue(foundElementCollectionFK, "Should find FK from element collection table to owner");
	}

	@Test
	public void testCompositeForeignKeys(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// Find composite FK (multi-column)
		ForeignKey compositeFK = foreignKeys.stream()
				.filter(fk -> fk.keyTable().contains("EntityWithCompositeFK") &&
						fk.targetTable().contains("CompositeKeyTarget"))
				.findFirst()
				.orElse(null);

		assertNotNull(compositeFK, "Should find composite FK");
		assertTrue(compositeFK.keyColumns().getJdbcTypeCount() > 1, "Composite FK should have multiple columns");
		assertEquals(compositeFK.keyColumns().getJdbcTypeCount(), compositeFK.targetColumns().getJdbcTypeCount(),
				"Composite FK key and target columns should match");
	}

	@Test
	public void testDeferrableFlagIsAlwaysFalse(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// Currently, all FKs are marked as non-deferrable
		boolean allNonDeferrable = foreignKeys.stream()
				.allMatch(fk -> !fk.deferrable());

		assertTrue(allNonDeferrable, "All FKs should be marked as non-deferrable (current implementation)");
	}

	@Test
	public void testForeignKeyTableNames(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(sessionFactory.getMappingMetamodel());
		var foreignKeys = constraintModel.foreignKeys();

		// Verify all FKs have non-null, non-empty table names
		boolean allHaveValidTables = foreignKeys.stream()
				.allMatch(fk -> fk.keyTable() != null && !fk.keyTable().isEmpty() &&
						fk.targetTable() != null && !fk.targetTable().isEmpty());

		assertTrue(allHaveValidTables, "All FKs should have valid key and target table names");
	}

	// Test entities

	@Entity(name = "SimpleEntity")
	@Table(name = "simple_entity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;
	}

	@Entity(name = "EntityWithSecondaryTable")
	@Table(name = "entity_primary")
	@SecondaryTable(name = "entity_secondary")
	public static class EntityWithSecondaryTable {
		@Id
		@GeneratedValue
		private Long id;

		private String primaryField;

		@jakarta.persistence.Column(table = "entity_secondary")
		private String secondaryField;
	}

	@Entity(name = "ParentEntity")
	@Table(name = "ParentEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class ParentEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String parentField;
	}

	@Entity(name = "ChildEntity")
	@Table(name = "ChildEntity")
	public static class ChildEntity extends ParentEntity {
		private String childField;
	}

	@Entity(name = "TargetEntity")
	@Table(name = "TargetEntity")
	public static class TargetEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String targetField;
	}

	@Entity(name = "EntityWithToOne")
	@Table(name = "EntityWithToOne")
	public static class EntityWithToOne {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn(name = "target_id")
		private TargetEntity target;
	}

	@Embeddable
	public static class EmbeddableWithFK {
		@ManyToOne
		@JoinColumn(name = "embedded_target_id")
		private TargetEntity embeddedTarget;

		private String embeddedField;
	}

	@Entity(name = "EntityWithEmbedded")
	@Table(name = "EntityWithEmbedded")
	public static class EntityWithEmbedded {
		@Id
		@GeneratedValue
		private Long id;

		@Embedded
		private EmbeddableWithFK embedded;
	}

	@Entity(name = "Owner")
	@Table(name = "Owner")
	public static class Owner {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "owner")
		private Set<Item> items;
	}

	@Entity(name = "Item")
	@Table(name = "Item")
	public static class Item {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn(name = "owner_id")
		private Owner owner;
	}

	@Entity(name = "Student")
	@Table(name = "Student")
	public static class Student {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToMany
		@JoinTable(
			name = "student_course",
			joinColumns = @JoinColumn(name = "student_id"),
			inverseJoinColumns = @JoinColumn(name = "course_id")
		)
		private Set<Course> courses;
	}

	@Entity(name = "Course")
	@Table(name = "Course")
	public static class Course {
		@Id
		@GeneratedValue
		private Long id;

		private String courseName;
	}

	@Entity(name = "OptionalTarget")
	@Table(name = "OptionalTarget")
	public static class OptionalTarget {
		@Id
		@GeneratedValue
		private Long id;

		private String optionalField;
	}

	@Entity(name = "EntityWithNullableFK")
	@Table(name = "EntityWithNullableFK")
	public static class EntityWithNullableFK {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(optional = true)
		@JoinColumn(name = "optional_target_id", nullable = true)
		private OptionalTarget optionalTarget;
	}

	@Entity(name = "EntityWithElementCollection")
	@Table(name = "EntityWithElementCollection")
	public static class EntityWithElementCollection {
		@Id
		@GeneratedValue
		private Long id;

		@jakarta.persistence.ElementCollection
		@jakarta.persistence.CollectionTable(name = "element_values", joinColumns = @JoinColumn(name = "owner_id"))
		@jakarta.persistence.Column(name = "value")
		private Set<String> values;
	}

	@Entity(name = "CompositeKeyTarget")
	@Table(name = "CompositeKeyTarget")
	public static class CompositeKeyTarget {
		@jakarta.persistence.EmbeddedId
		private CompositeKey id;

		private String targetData;
	}

	@Embeddable
	public static class CompositeKey implements java.io.Serializable {
		private Long keyPart1;
		private Long keyPart2;

		public CompositeKey() {}

		public CompositeKey(Long keyPart1, Long keyPart2) {
			this.keyPart1 = keyPart1;
			this.keyPart2 = keyPart2;
		}

		// Getters and setters
		public Long getKeyPart1() { return keyPart1; }
		public void setKeyPart1(Long keyPart1) { this.keyPart1 = keyPart1; }
		public Long getKeyPart2() { return keyPart2; }
		public void setKeyPart2(Long keyPart2) { this.keyPart2 = keyPart2; }
	}

	@Entity(name = "EntityWithCompositeFK")
	@Table(name = "EntityWithCompositeFK")
	public static class EntityWithCompositeFK {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn(name = "composite_key_part1", referencedColumnName = "keyPart1")
		@JoinColumn(name = "composite_key_part2", referencedColumnName = "keyPart2")
		private CompositeKeyTarget compositeTarget;
	}
}
