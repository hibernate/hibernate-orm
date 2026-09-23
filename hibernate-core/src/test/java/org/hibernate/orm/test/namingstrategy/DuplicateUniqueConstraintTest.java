/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.AnnotationException;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Duplicate explicit declarations must fail before deduplication or PK absorption.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey("HHH-20918")
class DuplicateUniqueConstraintTest {
	@Test
	void identicalColumnsAreStillDuplicateDeclarations() {
		assertDuplicate( IdenticalColumns.class, "identical_table", "@Table", "duplicate" );
	}

	@Test
	void primaryKeyAbsorptionDoesNotHideDuplicateDeclarations() {
		assertDuplicate( PrimaryKeyColumns.class, "pk_table", "@Table", "duplicate" );
	}

	@Test
	void secondaryTableReportsItsDeclaration() {
		assertDuplicate( SecondaryColumns.class, "secondary_details", "@SecondaryTable(name=\"secondary_details\")", "duplicate" );
	}

	@Test
	void collectionTableReportsTheMember() {
		assertDuplicate( CollectionColumns.class, "collection_values", ".values @CollectionTable", "duplicate" );
	}

	@Test
	void joinTableReportsTheMember() {
		assertDuplicate( JoinColumns.class, "joined_values", ".values @JoinTable", "duplicate", Target.class );
	}

	@Test
	void declarationsOnDifferentClassesIdentifyBothLocations() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( SharedTableFirst.class ).addManagedClass( SharedTableSecond.class ),
					new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() ) )
					.isInstanceOf( AnnotationException.class )
					.hasMessageContaining( "shared_table" )
					.hasMessageContaining( "duplicate" )
					.hasMessageContaining( SharedTableFirst.class.getName() + " @Table.uniqueConstraints[0]" )
					.hasMessageContaining( SharedTableSecond.class.getName() + " @Table.uniqueConstraints[0]" );
		}
	}

	@Test
	void unquotedNamesCompareWithoutCase() {
		assertDuplicate( CaseNames.class, "case_table", "@Table", "DUPLICATE" );
	}

	@Test
	void unnamedConstraintsRemainIndependent() {
		assertKeys( Unnamed.class, "unnamed_table", 2 );
	}

	@Test
	void distinctExplicitNamesRemainIndependent() {
		assertKeys( DistinctNames.class, "distinct_table", 2 );
	}

	@Test
	void quotedNamesRemainCaseSensitive() {
		assertKeys( QuotedNames.class, "quoted_table", 2 );
	}

	@Test
	void sameNameOnDifferentTablesIsAllowed() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( SeparateTables.class ),
					new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() );
			for ( var table : metadata.collectTableMappings() ) {
				assertThat( table.getUniqueKeys() ).hasSize( 1 );
			}
		}
	}

	private void assertDuplicate(Class<?> type, String table, String location, String name, Class<?>... additional) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var sources = new MappingSources().addManagedClass( type );
			for ( var other : additional ) {
				sources.addManagedClass( other );
			}
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming( registry, sources,
					new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() ) )
					.isInstanceOf( AnnotationException.class )
					.hasMessageContaining( "Duplicate explicit @UniqueConstraint name '" + name + "'" )
					.hasMessageContaining( "on table '" + table + "'" )
					.hasMessageContaining( type.getName() )
					.hasMessageContaining( location + ".uniqueConstraints[0]" )
					.hasMessageContaining( location + ".uniqueConstraints[1]" );
		}
	}

	private void assertKeys(Class<?> type, String tableName, int size) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( type ),
					new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() );
			final var table = metadata.getEntityBinding( type.getName() ).getTable();
			assertThat( table.getName() ).isEqualTo( tableName );
			assertThat( table.getUniqueKeys() ).hasSize( size );
		}
	}

	@Entity @Table(name = "identical_table", uniqueConstraints = {
			@UniqueConstraint(name = "duplicate", columnNames = "first"),
			@UniqueConstraint(name = "duplicate", columnNames = "first") })
	static class IdenticalColumns { @Id long id; String first; }

	@Entity @Table(name = "pk_table", uniqueConstraints = {
			@UniqueConstraint(name = "duplicate", columnNames = "id"),
			@UniqueConstraint(name = "duplicate", columnNames = "id") })
	static class PrimaryKeyColumns { @Id long id; }

	@Entity @SecondaryTable(name = "secondary_details", uniqueConstraints = {
			@UniqueConstraint(name = "duplicate", columnNames = "first"),
			@UniqueConstraint(name = "duplicate", columnNames = "second") })
	static class SecondaryColumns {
		@Id long id;
		@Column(table = "secondary_details") String first;
		@Column(table = "secondary_details") String second;
	}

	@Entity
	static class CollectionColumns {
		@Id long id;
		@ElementCollection
		@CollectionTable(name = "collection_values", joinColumns = @JoinColumn(name = "owner_id"), uniqueConstraints = {
				@UniqueConstraint(name = "duplicate", columnNames = "owner_id"),
				@UniqueConstraint(name = "duplicate", columnNames = "entry") })
		@Column(name = "entry") Set<String> values;
	}

	@Entity
	static class JoinColumns {
		@Id long id;
		@ManyToMany
		@JoinTable(name = "joined_values", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"), uniqueConstraints = {
				@UniqueConstraint(name = "duplicate", columnNames = "owner_id"),
				@UniqueConstraint(name = "duplicate", columnNames = "target_id") })
		Set<Target> values;
	}

	@Entity @Table(name = "shared_table", uniqueConstraints = @UniqueConstraint(name = "duplicate", columnNames = "first"))
	static class SharedTableFirst { @Id long id; String first; }

	@Entity @Table(name = "shared_table", uniqueConstraints = @UniqueConstraint(name = "duplicate", columnNames = "second"))
	static class SharedTableSecond { @Id long id; String second; }

	@Entity static class Target { @Id long id; }

	@Entity @Table(name = "case_table", uniqueConstraints = {
			@UniqueConstraint(name = "duplicate", columnNames = "first"),
			@UniqueConstraint(name = "DUPLICATE", columnNames = "second") })
	static class CaseNames { @Id long id; String first; String second; }

	@Entity @Table(name = "unnamed_table", uniqueConstraints = {
			@UniqueConstraint(columnNames = "first"), @UniqueConstraint(columnNames = "second") })
	static class Unnamed { @Id long id; String first; String second; }

	@Entity @Table(name = "distinct_table", uniqueConstraints = {
			@UniqueConstraint(name = "first_uk", columnNames = "first"),
			@UniqueConstraint(name = "second_uk", columnNames = "second") })
	static class DistinctNames { @Id long id; String first; String second; }

	@Entity @Table(name = "quoted_table", uniqueConstraints = {
			@UniqueConstraint(name = "`duplicate`", columnNames = "first"),
			@UniqueConstraint(name = "`DUPLICATE`", columnNames = "second") })
	static class QuotedNames { @Id long id; String first; String second; }

	@Entity @Table(name = "separate_primary", uniqueConstraints = @UniqueConstraint(name = "reused", columnNames = "first"))
	@SecondaryTable(name = "separate_secondary", uniqueConstraints = @UniqueConstraint(name = "reused", columnNames = "second"))
	static class SeparateTables {
		@Id long id;
		String first;
		@Column(table = "separate_secondary") String second;
	}
}
