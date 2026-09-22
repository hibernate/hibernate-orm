/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.relational.naming.spi.LogicalName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Subselect;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.spi.ForeignKeyNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Characterizes existing constraint lifecycle boundaries before FK/UK migration.
/// Includes HHH-20916 regressions for boot-time column uniqueness naming. Other
/// characterization assertions describe behavior awaiting the broader FK/UK migration.
///
/// @author Steve Ebersole
@BaseUnitTest
class ConstraintNamingCharacterizationTest {
	@Test
	void foreignKeyTargetingInlineViewIsNamedButNotExported() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new Recording();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Target.class, View.class, Holder.class ),
					strategy, new PhysicalNamingStrategyStandardImpl() );
			assertThat( strategy.foreignKeys ).containsExactly( "holder->View" );
			final var outgoing = metadata.getEntityBinding( View.class.getName() ).getTable().getForeignKeyCollection();
			assertThat( outgoing ).singleElement().satisfies( key -> assertThat( key.getName() ).isNull() );
			final var database = metadata.getDatabase();
			final var context = SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
			for ( var type : List.of( Holder.class ) ) {
				final var keys = metadata.getEntityBinding( type.getName() ).getTable().getForeignKeyCollection();
				assertThat( keys ).hasSize( 1 );
				for ( var key : keys ) {
					assertThat( key.getName() ).isNotBlank();
					assertThat( database.getDialect().getForeignKeyExporter().getSqlCreateStrings( key, metadata, context ) ).isEmpty();
				}
			}
		}
	}

	@Test
	void lastDeclaredExplicitUniqueKeySuppliesAbsorbedName() {
		for ( var type : List.of( FirstThenSecond.class, SecondThenFirst.class ) ) {
			try (var registry = ServiceRegistryUtil.serviceRegistry()) {
				final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, new MappingSources().addManagedClass( type ) );
				final var table = metadata.getEntityBinding( type.getName() ).getTable();
				assertThat( table.getUniqueKeys() ).isEmpty();
				assertThat( table.getPrimaryKey().getOrderingUniqueKey().getName() )
						.isEqualTo( type == FirstThenSecond.class ? "second" : "first" );
			}
		}
	}

	@Test
	void soleAbsorbedUniqueKeyCurrentlyLosesName() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, new MappingSources().addManagedClass( Sole.class ) );
			final var table = metadata.getEntityBinding( Sole.class.getName() ).getTable();
			assertThat( table.getUniqueKeys() ).isEmpty();
			assertThat( table.getPrimaryKey().getOrderingUniqueKey() ).isNull();
		}
	}

	@Test
	void oneToOneUniquenessIsNamedBeforeExport() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new Recording();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Target.class, Exclusive.class ), strategy,
					new PhysicalNamingStrategyStandardImpl() );
			final var table = (org.hibernate.mapping.NamedTable) metadata.getEntityBinding( Exclusive.class.getName() ).getTable();
			assertThat( strategy.uniqueKeys ).containsExactly( "exclusive_holder" );
			assertThat( table.getUniqueKeys().keySet() ).containsExactly( "UKbj44ryrb7uwf2fyiwgfxhjxrc" );
			final var database = metadata.getDatabase();
			database.getDialect().getTableExporter().getSqlCreateStrings( table, metadata,
					SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );
			assertThat( table.getUniqueKeys().keySet() ).containsExactly( "UKbj44ryrb7uwf2fyiwgfxhjxrc" );
			assertThat( strategy.uniqueKeys ).containsExactly( "exclusive_holder" );
		}
	}

	@Test
	void changelogUniquenessIsNamedBeforeExport() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new Recording();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Changes.class ), strategy,
					new PhysicalNamingStrategyStandardImpl() );
			final var table = (org.hibernate.mapping.NamedTable) metadata.getEntityBinding( Changes.class.getName() ).getTable();
			assertThat( strategy.uniqueKeys ).containsExactly( "changes" );
			assertThat( table.getUniqueKeys().keySet() ).containsExactly( "UKmslnnqfxiwdqtg6m9srmf2g19" );
			final var database = metadata.getDatabase();
			database.getDialect().getTableExporter().getSqlCreateStrings( table, metadata,
					SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );
			assertThat( table.getUniqueKeys().keySet() ).containsExactly( "UKmslnnqfxiwdqtg6m9srmf2g19" );
			assertThat( strategy.uniqueKeys ).containsExactly( "changes" );
		}
	}

	@Test
	void basicColumnUniquenessIsNamedBeforeExport() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new Recording();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( BasicUnique.class ), strategy,
					new PhysicalNamingStrategyStandardImpl() );
			final var table = (org.hibernate.mapping.NamedTable) metadata.getEntityBinding( BasicUnique.class.getName() ).getTable();
			assertThat( strategy.uniqueKeys ).containsExactly( "basic_unique" );
			assertThat( table.getUniqueKeys().keySet() ).containsExactly( "UK4b34e39s703sou7x1w6wf64gs" );
			final var database = metadata.getDatabase();
			database.getDialect().getTableExporter().getSqlCreateStrings( table, metadata,
					SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );
			assertThat( table.getUniqueKeys().keySet() ).containsExactly( "UK4b34e39s703sou7x1w6wf64gs" );
		}
	}

	@Test
	void elementColumnUniquenessAlreadyUsesStrategyRecipe() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new Recording();
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( ElementUnique.class ), strategy,
					new PhysicalNamingStrategyStandardImpl() );
			assertThat( strategy.uniqueKeys ).containsExactly( "unique_elements" );
		}
	}

	@Test
	void schemaDistinctTablesRetainTheirIndividualLogicalNamesForForeignKeys() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new Recording();
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Alpha.class, Beta.class, Target.class ), strategy,
					new PhysicalNamingStrategyStandardImpl() {
						@Override @Nonnull
						public org.hibernate.relational.naming.spi.PhysicalName toPhysicalTableName(
								@Nonnull org.hibernate.relational.naming.spi.LogicalName name,
								@Nonnull org.hibernate.boot.model.naming.spi.PhysicalNamingContext context) {
							return context.getPhysicalNameFactory().create( name.getText().equals( "target" ) ? "target" : "shared", false );
						}
					} );
			assertThat( strategy.uniqueKeys ).hasSize( 2 );
			assertThat( strategy.uniqueKeys ).containsExactlyInAnyOrder( "alpha", "beta" );
			assertThat( strategy.foreignKeys ).hasSize( 2 );
			assertThat( strategy.foreignKeys ).containsExactlyInAnyOrder( "alpha->target", "beta->target" );
		}
	}

	@Test
	void fallbackReplacementHonorsCustomStrategiesOnceAndPreservesQuoting() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicitCalls = new ArrayList<String>();
			final var physicalCalls = new ArrayList<String>();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( BasicUnique.class ),
					new StandardImplicitNamingStrategy() {
						@Override @Nonnull
						public Identifier determineUniqueKeyName(@Nonnull ImplicitUniqueKeyNameSource source) {
							implicitCalls.add( source.getTableName().getText() );
							return Identifier.toIdentifier( "chosen", true );
						}
					}, new PhysicalNamingStrategyStandardImpl() {
						@Override @Nonnull
						public org.hibernate.relational.naming.spi.PhysicalName toPhysicalUniqueKeyName(
								@Nonnull org.hibernate.relational.naming.spi.LogicalName name,
								@Nonnull org.hibernate.boot.model.naming.spi.PhysicalNamingContext context) {
							assertThat( name.isExplicit() ).isFalse();
							physicalCalls.add( name.getText() );
							return context.getPhysicalNameFactory().create( "p_" + name.getText(), false );
						}
					} );
			final var table = (org.hibernate.mapping.NamedTable) metadata.getEntityBinding( BasicUnique.class.getName() ).getTable();
			assertThat( table.getUniqueKeys().keySet() ).containsExactly( "\"p_chosen\"" );
			final var database = metadata.getDatabase();
			final var context = SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
			for ( int i = 0; i < 2; i++ ) {
				database.getDialect().getTableExporter().getSqlCreateStrings( table, metadata, context );
			}
			assertThat( implicitCalls ).containsExactly( "basic_unique" );
			assertThat( physicalCalls ).containsExactly( "chosen" );
			assertThat( table.getUniqueKeys().keySet() ).containsExactly( "\"p_chosen\"" );
		}
	}

	@Test
	void distinctUniqueColumnsDoNotMergeUnderACollidingFallbackName() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry,
					new MappingSources().addManagedClass( TwoUnique.class ) );
			final var keys = metadata.getEntityBinding( TwoUnique.class.getName() ).getTable().getUniqueKeys();
			assertThat( keys ).hasSize( 2 );
			assertThat( keys.values() ).allSatisfy( key -> assertThat( key.getColumns() ).hasSize( 1 ) );
		}
	}

	static class Recording extends StandardImplicitNamingStrategy {
		final List<String> foreignKeys = new ArrayList<>();
		final List<String> uniqueKeys = new ArrayList<>();
		@Override @Nonnull
		public LogicalName determineForeignKeyName(@Nonnull ForeignKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
			foreignKeys.add( input.table().logicalName().getText() + "->" + input.referencedTable().logicalName().getText() );
			return super.determineForeignKeyName( input, context );
		}
		@Override @Nonnull
		public Identifier determineUniqueKeyName(@Nonnull ImplicitUniqueKeyNameSource source) {
			uniqueKeys.add( source.getTableName().getText() );
			return super.determineUniqueKeyName( source );
		}
	}

	@Entity @org.hibernate.annotations.Changelog @Table(name = "changes")
	static class Changes {
		@Id long id;
		@org.hibernate.annotations.Changelog.ChangesetId long revision;
		@org.hibernate.annotations.Changelog.Timestamp java.time.Instant createdAt;
	}
	@Entity @Table(name = "alpha", schema = "a", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
	static class Alpha { @Id long id; String code; @ManyToOne Target target; }
	@Entity @Table(name = "beta", schema = "b", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
	static class Beta { @Id long id; String code; @ManyToOne Target target; }
	@Entity @Table(name = "two_unique")
	static class TwoUnique {
		@Id long id;
		@jakarta.persistence.Column(unique = true) String code;
		@jakarta.persistence.Column(unique = true) String alternateCode;
	}
	@Entity @Table(name = "basic_unique")
	static class BasicUnique { @Id long id; @jakarta.persistence.Column(unique = true) String code; }
	@Entity @Table(name = "element_owner")
	static class ElementUnique {
		@Id long id;
		@jakarta.persistence.ElementCollection
		@jakarta.persistence.CollectionTable(name = "unique_elements")
		@jakarta.persistence.Column(name = "code", unique = true)
		java.util.List<String> codes;
	}
	@Entity @Table(name = "target")
	static class Target { @Id long id; }
	@Entity(name = "View") @Subselect("select id, target_id from source_view")
	static class View { @Id long id; @ManyToOne @JoinColumn(name = "target_id") Target target; }
	@Entity @Table(name = "holder")
	static class Holder { @Id long id; @ManyToOne @JoinColumn(name = "view_id") View view; }
	@Entity @Table(name = "exclusive_holder")
	static class Exclusive { @Id long id; @OneToOne @JoinColumn(name = "target_id") Target target; }
	@Entity @Table(name = "first_second", uniqueConstraints = {
			@UniqueConstraint(name = "first", columnNames = "id"),
			@UniqueConstraint(name = "second", columnNames = "id") })
	static class FirstThenSecond { @Id long id; }
	@Entity @Table(name = "second_first", uniqueConstraints = {
			@UniqueConstraint(name = "second", columnNames = "id"),
			@UniqueConstraint(name = "first", columnNames = "id") })
	static class SecondThenFirst { @Id long id; }
	@Entity @Table(name = "sole", uniqueConstraints = @UniqueConstraint(name = "sole_pk", columnNames = "id"))
	static class Sole { @Id long id; }
}
