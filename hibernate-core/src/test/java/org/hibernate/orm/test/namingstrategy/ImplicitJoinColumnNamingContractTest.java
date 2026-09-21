/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.AssociationKeyNamingInput;
import org.hibernate.boot.model.naming.spi.CollectionKeyNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.JoinColumnNamingInput;
import org.hibernate.boot.model.naming.spi.MapKeyJoinColumnNamingInput;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.PrimaryKeyJoinColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ReferencedColumnsNamingInput;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies join roles, dependency stages, explicit bypass, composite ordering, and additive quoting.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitJoinColumnNamingContractTest {
	@Test
	void rolesUseSettledNamesOnce() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new RecordingStrategy();
			final var physical = new PrefixStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Owner.class, Target.class ), strategy, physical );
			final var owner = metadata.getEntityBinding( Owner.class.getName() );
			assertThat( owner.getProperty( "target" ).getColumns().get( 0 ).getName() ).isEqualTo( "p_toOne_target_id" );
			assertThat( owner.getProperty( "target" ).getColumns().get( 0 ).isQuoted() ).isTrue();
			assertThat( owner.getProperty( "explicit" ).getColumns().get( 0 ).getName() ).isEqualTo( "p_chosen_fk" );
			final var targets = metadata.getCollectionBinding( Owner.class.getName() + ".targets" );
			assertThat( targets.getKey().getColumns().get( 0 ).getName() ).isEqualTo( "p_owner_targets_id" );
			assertThat( targets.getElement().getColumns().get( 0 ).getName() ).isEqualTo( "p_target_targets_id" );
			final var labels = (org.hibernate.mapping.Map) metadata.getCollectionBinding( Owner.class.getName() + ".labels" );
			assertThat( labels.getIndex().getColumns().get( 0 ).getName() ).isEqualTo( "p_map_labels_id" );
			assertThat( owner.getJoins().get( 0 ).getKey().getColumns().get( 0 ).getName() ).isEqualTo( "p_pk_id" );
			assertThat( strategy.calls ).containsExactlyInAnyOrder(
					"toOne_target_id", "owner_targets_id", "target_targets_id", "owner_labels_id", "map_labels_id", "pk_id", "owner_joinedTarget_id", "target_joinedTarget_id" );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "chosen_fk" ) )
					.singleElement().satisfies( name -> assertThat( name.isExplicit() ).isTrue() );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "toOne_target_id" ) )
					.singleElement().satisfies( name -> assertThat( name.isExplicit() ).isFalse() );
		}
	}

	@Test
	void compositeRequestsFollowResolvedTargetOrder() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new RecordingStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( CompositeOwner.class, CompositeTarget.class ), strategy, new PrefixStrategy() );
			assertThat( strategy.calls ).containsExactly( "toOne_target_first", "toOne_target_second" );
			assertThat( strategy.positions ).containsExactly( 0, 1 );
			assertThat( metadata.getEntityBinding( CompositeOwner.class.getName() ).getProperty( "target" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName )
					.containsExactly( "p_toOne_target_first", "p_toOne_target_second" );
		}
	}

	@Test
	void nonPrimaryKeyDependenciesWaitForTargetMembers() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new RecordingStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( BusinessOwner.class, BusinessTarget.class ), strategy, new PrefixStrategy() );
			assertThat( strategy.calls ).containsExactly( "toOne_target_business_code" );
			assertThat( metadata.getEntityBinding( BusinessOwner.class.getName() ).getProperty( "target" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_toOne_target_business_code" );
		}
	}

	@Test
	void inlineViewDependenciesHaveNoPhysicalTableName() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new RecordingStrategy() {
				@Override
				public LogicalName determineJoinColumnName(JoinColumnNamingInput input, ImplicitNamingContext context) {
					assertThat( input.reference().table() ).isInstanceOf( org.hibernate.boot.model.naming.spi.InlineViewNamingInput.class );
					return super.determineJoinColumnName( input, context );
				}
			};
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( ViewOwner.class, ViewTarget.class ), strategy, new PrefixStrategy() );
			assertThat( strategy.calls ).containsExactly( "toOne_target_id" );
		}
	}

	static class RecordingStrategy extends StandardImplicitNamingStrategy {
		final List<String> calls = new ArrayList<>();
		final List<Integer> positions = new ArrayList<>();

		LogicalName name(String role, ReferencedColumnsNamingInput reference, ImplicitNamingContext context) {
			final var column = reference.column();
			assertThat( column.physicalName().getText() ).isEqualTo( "p_" + column.logicalName().getText() );
			final String name = role + "_" + column.logicalName().getText();
			calls.add( name );
			return context.implicitName( name, true );
		}
		@Override
		public LogicalName determineJoinColumnName(JoinColumnNamingInput input, ImplicitNamingContext context) {
			positions.add( input.reference().columnPosition() );
			return name( "toOne_" + input.attributePath(), input.reference(), context );
		}
		@Override
		public LogicalName determineCollectionKeyColumnName(CollectionKeyNamingInput input, ImplicitNamingContext context) {
			return name( "owner_" + input.attributePath(), input.reference(), context );
		}
		@Override
		public LogicalName determineAssociationKeyColumnName(AssociationKeyNamingInput input, ImplicitNamingContext context) {
			return name( "target_" + input.attributePath(), input.reference(), context );
		}
		@Override
		public LogicalName determineMapKeyJoinColumnName(MapKeyJoinColumnNamingInput input, ImplicitNamingContext context) {
			return name( "map_" + input.attributePath(), input.reference(), context );
		}
		@Override
		public LogicalName determinePrimaryKeyJoinColumnName(PrimaryKeyJoinColumnNamingInput input, ImplicitNamingContext context) {
			return name( "pk", input.reference(), context );
		}
	}

	static class PrefixStrategy extends PhysicalNamingStrategyStandardImpl {
		final List<LogicalName> inputs = new ArrayList<>();
		@Override
		public PhysicalName toPhysicalColumnName(LogicalName name, PhysicalNamingContext context) {
			inputs.add( name );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), false );
		}
	}

	@Entity(name = "JoinOwner")
	@SecondaryTable(name = "owner_details")
	static class Owner {
		@Id long id;
		@ManyToOne Target target;
		@ManyToOne @jakarta.persistence.JoinTable(name = "owner_target") Target joinedTarget;
		@ManyToOne @JoinColumn(name = "chosen_fk") Target explicit;
		@ManyToMany Set<Target> targets;
		@ElementCollection Map<Target, String> labels;
		@Column(table = "owner_details") String details;
	}
	@Entity(name = "JoinTarget")
	static class Target { @Id long id; }

	@Embeddable
	static class CompositeId {
		String first;
		String second;
	}
	@Entity(name = "CompositeJoinTarget")
	static class CompositeTarget { @EmbeddedId CompositeId id; }
	@Entity(name = "CompositeJoinOwner")
	static class CompositeOwner {
		@Id long id;
		@ManyToOne @JoinColumns({ @JoinColumn(referencedColumnName = "second"), @JoinColumn(referencedColumnName = "first") })
		CompositeTarget target;
	}
	@Entity(name = "JoinBusinessTarget")
	static class BusinessTarget {
		@Id long id;
		@Column(name = "business_code", unique = true) String code;
	}
	@Entity(name = "JoinBusinessOwner")
	static class BusinessOwner {
		@Id long id;
		@ManyToOne @JoinColumn(referencedColumnName = "business_code") BusinessTarget target;
	}
	@Entity(name = "JoinViewTarget")
	@org.hibernate.annotations.Subselect("select 1 as p_id")
	static class ViewTarget { @Id long id; }
	@Entity(name = "JoinViewOwner")
	static class ViewOwner { @Id long id; @ManyToOne ViewTarget target; }

}
