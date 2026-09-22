/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.AssociationTableNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.InlineViewNamingInput;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.PrimaryTableNamingInput;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Exercises immutable dependency inputs through real table binding and physical naming.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitTableNamingContractTest {
	@Test
	void logicalCompositionWithPhysicalDependenciesAndExplicitBypass() {
		for ( boolean reverse : List.of( false, true ) ) {
			try (var registry = ServiceRegistryUtil.serviceRegistry()) {
				final var strategy = new CapturingStrategy();
				final var sources = new MappingSources();
				sources.addManagedClass( reverse ? Passport.class : Person.class );
				sources.addManagedClass( reverse ? Person.class : Passport.class );
				final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming(
						registry, sources, strategy, new PrefixStrategy() );
				assertThat( metadata.getCollectionBinding( Person.class.getName() + ".passports" )
						.getCollectionTable().getName() ).isEqualTo( "p_Person_Passport" );
				assertThat( metadata.getCollectionBinding( Person.class.getName() + ".explicitPassports" )
						.getCollectionTable().getName() ).isEqualTo( "p_selected_links" );
				assertThat( strategy.inputs ).hasSize( 1 );
				final var input = strategy.inputs.get( 0 );
				final var owner = ((NamedTableNamingInput) input.owningTable()).names();
				final var target = ((NamedTableNamingInput) input.targetTable()).names();
				assertThat( owner.logicalName().getText() ).isEqualTo( "Person" );
				assertThat( owner.logicalName().isExplicit() ).isFalse();
				assertThat( owner.physicalName().getText() ).isEqualTo( "p_Person" );
				assertThat( target.logicalName().getText() ).isEqualTo( "Passport" );
				assertThat( target.logicalName().isExplicit() ).isTrue();
				assertThat( target.physicalName().getText() ).isEqualTo( "p_Passport" );
				assertThat( strategy.primaryCalls ).isEqualTo( 1 );
			}
		}
	}

	@Test
	void sharedPhysicalTableRetainsEachEntityLogicalAlias() {
		for ( boolean reverse : List.of( false, true ) ) {
			try (var registry = ServiceRegistryUtil.serviceRegistry()) {
				final var strategy = new CapturingStrategy();
				final var sources = new MappingSources()
						.addManagedClass( reverse ? Destination.class : Source.class )
						.addManagedClass( reverse ? Source.class : Destination.class );
				MetadataBuildingTestHelper.buildMetadataWithNaming( registry, sources, strategy, new PrefixStrategy() {
					@Override
					@Nonnull
					public PhysicalName toPhysicalTableName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
						return name.getText().equals( "Source" ) || name.getText().equals( "Destination" )
								? context.getPhysicalNameFactory().create( "shared", false )
								: super.toPhysicalTableName( name, context );
					}
				} );
				assertThat( strategy.inputs ).hasSize( 1 );
				assertThat( strategy.inputs.get( 0 ).owningTable().logicalName().getText() ).isEqualTo( "Source" );
				assertThat( strategy.inputs.get( 0 ).targetTable().logicalName().getText() ).isEqualTo( "Destination" );
			}
		}
	}

	@Test
	void quotingFromLogicalDependenciesSurvivesComposition() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( QuotedOwner.class ).addManagedClass( Passport.class ),
					new StandardImplicitNamingStrategy(), new PrefixStrategy() );
			final var table = metadata.getCollectionBinding( QuotedOwner.class.getName() + ".passports" ).getCollectionTable();
			assertThat( table.getName() ).isEqualTo( "p_QuotedOwner_Passport" );
			assertThat( table.isQuoted() ).isTrue();
		}
	}

	@Test
	void inlineViewHasNoPhysicalTableName() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new CapturingStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( ViewOwner.class ).addManagedClass( PassportView.class ),
					strategy, new PrefixStrategy() );
			assertThat( strategy.inputs ).hasSize( 1 );
			assertThat( strategy.inputs.get( 0 ).targetTable() ).isInstanceOf( InlineViewNamingInput.class );
			assertThat( metadata.getCollectionBinding( ViewOwner.class.getName() + ".passports" )
					.getCollectionTable().getName() ).isEqualTo( "p_ViewOwner_PassportView" );
		}
	}

	@Test
	void explicitStrategyResultIsRejected() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithImplicitNaming( registry,
					new MappingSources().addManagedClass( SimpleEntity.class ), new StandardImplicitNamingStrategy() {
						@Override
						@Nonnull
						public LogicalName determinePrimaryTableName(@Nonnull PrimaryTableNamingInput input, @Nonnull ImplicitNamingContext context) {
							return new LogicalName( "invalid", false, true );
						}
					} ) ).hasMessageContaining( "non-null implicit name for primary table" );
		}
	}

	static class CapturingStrategy extends StandardImplicitNamingStrategy {
		final List<AssociationTableNamingInput> inputs = new ArrayList<>();
		int primaryCalls;

		@Override
		@Nonnull
		public LogicalName determinePrimaryTableName(@Nonnull PrimaryTableNamingInput input, @Nonnull ImplicitNamingContext context) {
			primaryCalls++;
			return super.determinePrimaryTableName( input, context );
		}

		@Override
		@Nonnull
		public LogicalName determineAssociationTableName(@Nonnull AssociationTableNamingInput input, @Nonnull ImplicitNamingContext context) {
			inputs.add( input );
			return super.determineAssociationTableName( input, context );
		}
	}

	static class PrefixStrategy extends PhysicalNamingStrategyStandardImpl {
		@Override
		@Nonnull
		public PhysicalName toPhysicalTableName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
	}

	@Entity(name = "Person")
	static class Person {
		@Id long id;
		@ManyToMany Set<Passport> passports;
		@ManyToMany @JoinTable(name = "selected_links") Set<Passport> explicitPassports;
	}

	@Entity(name = "Passport") @Table(name = "Passport")
	static class Passport { @Id long id; }

	@Entity(name = "SimpleEntity")
	static class SimpleEntity { @Id long id; }

	@Entity(name = "Source") @Table(name = "Source")
	static class Source {
		@Id long id;
		@ManyToMany Set<Destination> destinations;
	}
	@Entity(name = "Destination") @Table(name = "Destination")
	static class Destination { @Id long id; }

	@Entity(name = "QuotedOwner") @Table(name = "`QuotedOwner`")
	static class QuotedOwner {
		@Id long id;
		@ManyToMany Set<Passport> passports;
	}

	@Entity(name = "ViewOwner")
	static class ViewOwner {
		@Id long id;
		@ManyToMany Set<PassportView> passports;
	}

	@Entity(name = "PassportView") @org.hibernate.annotations.Subselect("select id from passports")
	static class PassportView { @Id long id; }
}
