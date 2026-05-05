/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.constraint;

import jakarta.persistence.Column;
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
import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.constraint.ConstraintModelBuilder;
import org.hibernate.action.queue.constraint.ForeignKey;
import org.hibernate.action.queue.constraint.UniqueConstraint;
import org.hibernate.annotations.NaturalId;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ConstraintModelBuilder}
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
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
		ForeignKeyModelBuilderTest.CompositeKeyTarget.class,
		ForeignKeyModelBuilderTest.EntityWithUniqueColumn.class,
		ForeignKeyModelBuilderTest.UniqueTargetEntity.class,
		ForeignKeyModelBuilderTest.EntityWithUniqueTargetFK.class,
		ForeignKeyModelBuilderTest.EntityWithSecondaryNaturalId.class,
		ForeignKeyModelBuilderTest.EntityWithQuotedUniqueColumn.class,
		ForeignKeyModelBuilderTest.EntityWithSameColumnNameOnSecondary.class
})
@SessionFactory
public class ForeignKeyModelBuilderTest {
	private final PlanningOptions planningOptions = new PlanningOptions(
			true,
			false,
			true,
			true,
			PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
	);

	@Test
	public void testBasicForeignKeyModelBuilding(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		assertNotNull(constraintModel);
		assertNotNull(foreignKeys);
		assertFalse(foreignKeys.isEmpty(), "Foreign key model should contain foreign keys");
	}

	@Test
	public void testSecondaryTableForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		// EntityWithSecondaryTable should have FK from secondary table to primary table
		boolean foundSecondaryTableFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("entity_secondary") &&
						fk.targetTable().contains("entity_primary"));

		assertTrue(foundSecondaryTableFK, "Should find FK from secondary table to primary table");
	}

	@Test
	public void testJoinedInheritanceForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		// ChildEntity table should have FK to ParentEntity table in joined inheritance
		boolean foundInheritanceFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("ChildEntity") &&
						fk.targetTable().contains("ParentEntity"));

		assertTrue(foundInheritanceFK, "Should find FK from child table to parent table in joined inheritance");
	}

	@Test
	public void testToOneForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		// EntityWithToOne should have FK to TargetEntity
		boolean foundToOneFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("EntityWithToOne") &&
						fk.targetTable().contains("TargetEntity"));

		assertTrue(foundToOneFK, "Should find FK for @ManyToOne association");
	}

	@Test
	public void testCollectionTableForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		// Owner's @OneToMany should create FK in Item table
		boolean foundCollectionFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("Item") &&
						fk.targetTable().contains("Owner"));

		assertTrue(foundCollectionFK, "Should find FK from collection table to owner table");
	}

	@Test
	public void testManyToManyForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		// Many-to-many join table should have two FKs
		long m2mFKCount = foreignKeys.stream()
				.filter(fk -> fk.keyTable().contains("student_course"))
				.count();

		assertEquals(2, m2mFKCount, "Many-to-many join table should have 2 foreign keys");
	}

	@Test
	public void testEmbeddedToOneForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		// EntityWithEmbedded has embedded component with ToOne - should find FK
		boolean foundEmbeddedFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("EntityWithEmbedded") &&
						fk.targetTable().contains("TargetEntity"));

		assertTrue(foundEmbeddedFK, "Should find FK from embedded component's ToOne association");
	}

	@Test
	public void testNullableForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
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
	public void testForeignKeyColumns(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
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
	public void testNoDuplicateForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		Set<String> signatures = new HashSet<>();
		for ( ForeignKey foreignKey : foreignKeys ) {
			assertTrue(
					signatures.add( foreignKeySignature( foreignKey ) ),
					"Should not collect duplicate foreign keys"
			);
		}
	}

	@Test
	public void testElementCollectionForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		// Element collection table should have FK back to owner
		boolean foundElementCollectionFK = foreignKeys.stream()
				.anyMatch(fk -> fk.keyTable().contains("element_values") &&
						fk.targetTable().contains("EntityWithElementCollection"));

		assertTrue(foundElementCollectionFK, "Should find FK from element collection table to owner");
	}

	@Test
	public void testCompositeForeignKeys(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
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
	public void testDeferrableFlagIsAlwaysFalse(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		// Currently, all FKs are marked as non-deferrable
		boolean allNonDeferrable = foreignKeys.stream()
				.allMatch(fk -> !fk.isDeferrable());

		assertTrue(allNonDeferrable, "All FKs should be marked as non-deferrable (current implementation)");
	}

	@Test
	public void testForeignKeyTableNames(DomainModelScope bootModelScope, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = ConstraintModelBuilder.buildConstraintModel(
				sessionFactory.getMappingMetamodel(),
				planningOptions,
				bootModelScope.getDomainModel(),
				sessionFactory.getSqlStringGenerationContext()
		);
		var foreignKeys = constraintModel.foreignKeys();

		// Verify all FKs have non-null, non-empty table names
		boolean allHaveValidTables = foreignKeys.stream()
				.allMatch(fk -> fk.keyTable() != null && !fk.keyTable().isEmpty() &&
						fk.targetTable() != null && !fk.targetTable().isEmpty());

		assertTrue(allHaveValidTables, "All FKs should have valid key and target table names");
	}

	@Test
	public void testUniqueColumnConstraint(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = sessionFactory.getMappingMetamodel().getConstraintModel();

		UniqueConstraint uniqueConstraint = constraintModel.uniqueConstraints().stream()
				.filter( constraint -> constraint.tableName().contains( "entity_with_unique_column" ) )
				.filter( UniqueConstraint::isUniqueKey )
				.filter( constraint -> hasPropertyName( constraint, "code" ) )
				.findFirst()
				.orElse( null );

		assertNotNull( uniqueConstraint, "Should find unique constraint for @Column(unique = true)" );
		assertEquals( 1, uniqueConstraint.columns().getJdbcTypeCount() );
		assertEquals( "code", uniqueConstraint.columns().getSelectable( 0 ).getSelectableName() );
		assertFalse( uniqueConstraint.nullable(), "Unique code column should be marked non-nullable" );
	}

	private static boolean hasPropertyName(UniqueConstraint constraint, String propertyName) {
		if ( constraint.propertyNames() == null ) {
			return false;
		}
		for ( String candidate : constraint.propertyNames() ) {
			if ( candidate.equals( propertyName ) ) {
				return true;
			}
		}
		return false;
	}

	@Test
	public void testForeignKeyTargetingUniqueKey(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = sessionFactory.getMappingMetamodel().getConstraintModel();

		ForeignKey foreignKey = constraintModel.foreignKeys().stream()
				.filter( fk -> fk.keyTable().contains( "entity_with_unique_target_fk" ) )
				.filter( fk -> fk.targetTable().contains( "unique_target_entity" ) )
				.findFirst()
				.orElse( null );

		assertNotNull( foreignKey, "Should find FK targeting a unique non-PK column" );
		assertEquals( ForeignKey.TargetType.UNIQUE_KEY, foreignKey.targetType() );
	}

	@Test
	public void testSecondaryTableNaturalIdConstraint(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = sessionFactory.getMappingMetamodel().getConstraintModel();

		UniqueConstraint naturalIdConstraint = constraintModel.uniqueConstraints().stream()
				.filter( constraint -> constraint.tableName().contains( "secondary_natural_id" ) )
				.filter( UniqueConstraint::isUniqueKey )
				.filter( constraint -> hasPropertyName( constraint, "code" ) )
				.findFirst()
				.orElse( null );

		assertNotNull( naturalIdConstraint, "Should find natural-id constraint on secondary table" );
		assertEquals( "secondary_natural_id", naturalIdConstraint.columns().getSelectable( 0 ).getContainingTableExpression() );
	}

	@Test
	public void testQuotedTableUniqueConstraintLookupIsNormalized(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = sessionFactory.getMappingMetamodel().getConstraintModel();

		UniqueConstraint uniqueConstraint = constraintModel.getUniqueConstraintsForTable( "quoted_unique_table" )
				.stream()
				.filter( UniqueConstraint::isUniqueKey )
				.filter( constraint -> hasPropertyName( constraint, "quotedCode" ) )
				.findFirst()
				.orElse( null );

		assertNotNull( uniqueConstraint, "Should find quoted-table unique constraint via unquoted table lookup" );
		assertEquals( 1, uniqueConstraint.columns().getJdbcTypeCount() );
		assertEquals( "quotedCode", uniqueConstraint.propertyNames()[0] );
	}

	@Test
	public void testUniqueConstraintColumnMatchingUsesContainingTable(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = sessionFactory.getMappingMetamodel().getConstraintModel();

		UniqueConstraint uniqueConstraint = constraintModel.uniqueConstraints().stream()
				.filter( constraint -> constraint.tableName().contains( "same_column_secondary" ) )
				.filter( UniqueConstraint::isUniqueKey )
				.filter( constraint -> hasPropertyName( constraint, "secondaryCode" ) )
				.findFirst()
				.orElse( null );

		assertNotNull(
				uniqueConstraint,
				"Should match the secondary-table column when names overlap: "
						+ uniqueConstraintDescriptions( constraintModel.uniqueConstraints() )
		);
		assertEquals( 1, uniqueConstraint.columns().getJdbcTypeCount() );
		assertEquals( "same_column_secondary", uniqueConstraint.columns().getSelectable( 0 ).getContainingTableExpression() );
		assertEquals( "secondaryCode", uniqueConstraint.propertyNames()[0] );
	}

	@Test
	public void testCollectionTableUniqueConstraint(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = sessionFactory.getMappingMetamodel().getConstraintModel();

		UniqueConstraint uniqueConstraint = constraintModel.uniqueConstraints().stream()
				.filter( constraint -> constraint.tableName().contains( "element_values" ) )
				.filter( UniqueConstraint::isUniqueKey )
				.filter( constraint -> constraint.columns().getJdbcTypeCount() == 2 )
				.findFirst()
				.orElse( null );

		assertNotNull( uniqueConstraint, "Should find collection-table unique constraint" );
		assertEquals( "element_values", uniqueConstraint.columns().getSelectable( 0 ).getContainingTableExpression() );
		assertEquals( "element_values", uniqueConstraint.columns().getSelectable( 1 ).getContainingTableExpression() );
		assertNull( uniqueConstraint.propertyNames(), "Collection unique constraints are not entity property slots" );
	}

	@Test
	public void testPrimaryKeyUniqueConstraintsForAllEntityTables(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		var constraintModel = sessionFactory.getMappingMetamodel().getConstraintModel();

		assertPrimaryKeyUniqueConstraint( constraintModel.uniqueConstraints(), "entity_primary" );
		assertPrimaryKeyUniqueConstraint( constraintModel.uniqueConstraints(), "entity_secondary" );
		assertPrimaryKeyUniqueConstraint( constraintModel.uniqueConstraints(), "ParentEntity" );
		assertPrimaryKeyUniqueConstraint( constraintModel.uniqueConstraints(), "ChildEntity" );
	}

	private static void assertPrimaryKeyUniqueConstraint(
			List<UniqueConstraint> uniqueConstraints,
			String tableName) {
		UniqueConstraint primaryKey = uniqueConstraints.stream()
				.filter( constraint -> constraint.tableName().equals( tableName ) )
				.filter( UniqueConstraint::isPrimaryKey )
				.findFirst()
				.orElse( null );

		assertNotNull( primaryKey, "Should find primary-key unique constraint for " + tableName );
		assertEquals( tableName, primaryKey.columns().getSelectable( 0 ).getContainingTableExpression() );
		assertFalse( primaryKey.nullable(), "Primary-key unique constraint should be non-nullable" );
	}

	private static String foreignKeySignature(ForeignKey foreignKey) {
		return foreignKey.keyTable()
				+ "->"
				+ foreignKey.targetTable()
				+ ":"
				+ selectableNames( foreignKey.keyColumns() )
				+ "->"
				+ selectableNames( foreignKey.targetColumns() );
	}

	private static String selectableNames(SelectableMappings columns) {
		StringBuilder signature = new StringBuilder();
		for ( int i = 0; i < columns.getJdbcTypeCount(); i++ ) {
			if ( i > 0 ) {
				signature.append( ',' );
			}
			signature.append( columns.getSelectable( i ).getSelectableName() );
		}
		return signature.toString();
	}

	private static String uniqueConstraintDescriptions(List<UniqueConstraint> uniqueConstraints) {
		return uniqueConstraints.stream()
				.map( constraint -> constraint.tableName()
						+ ":"
						+ selectableNames( constraint.columns() )
						+ ":"
						+ ( constraint.propertyNames() == null ? "<none>" : String.join( ",", constraint.propertyNames() ) ) )
				.toList()
				.toString();
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
		@jakarta.persistence.CollectionTable(
				name = "element_values",
				joinColumns = @JoinColumn(name = "owner_id"),
				uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = { "owner_id", "value" })
		)
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

	@Entity(name = "EntityWithUniqueColumn")
	@Table(name = "entity_with_unique_column")
	public static class EntityWithUniqueColumn {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "code", unique = true, nullable = false)
		private String code;
	}

	@Entity(name = "UniqueTargetEntity")
	@Table(name = "unique_target_entity")
	public static class UniqueTargetEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "code", unique = true, nullable = false)
		private String code;
	}

	@Entity(name = "EntityWithUniqueTargetFK")
	@Table(name = "entity_with_unique_target_fk")
	public static class EntityWithUniqueTargetFK {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn(name = "target_code", referencedColumnName = "code")
		private UniqueTargetEntity target;
	}

	@Entity(name = "EntityWithSecondaryNaturalId")
	@Table(name = "primary_natural_id")
	@SecondaryTable(name = "secondary_natural_id")
	public static class EntityWithSecondaryNaturalId {
		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		@Column(table = "secondary_natural_id", name = "code", nullable = false)
		private String code;
	}

	@Entity(name = "EntityWithQuotedUniqueColumn")
	@Table(name = "`quoted_unique_table`")
	public static class EntityWithQuotedUniqueColumn {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`quoted_code`", unique = true, nullable = false)
		private String quotedCode;
	}

	@Entity(name = "EntityWithSameColumnNameOnSecondary")
	@Table(name = "same_column_primary")
	@SecondaryTable(
			name = "same_column_secondary",
			uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "code")
	)
	public static class EntityWithSameColumnNameOnSecondary {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "code")
		private String primaryCode;

		@Column(table = "same_column_secondary", name = "code")
		private String secondaryCode;
	}
}
