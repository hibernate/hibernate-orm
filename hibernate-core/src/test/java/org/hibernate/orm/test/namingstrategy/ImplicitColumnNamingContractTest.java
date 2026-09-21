/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Formula;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.AnyColumnNamingInput;
import org.hibernate.boot.model.naming.spi.BasicColumnNamingInput;
import org.hibernate.boot.model.naming.spi.CollectionElementColumnNamingInput;
import org.hibernate.boot.model.naming.spi.IdentifierColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.ListIndexColumnNamingInput;
import org.hibernate.boot.model.naming.spi.MapKeyColumnNamingInput;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.mapping.Component;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Checks independent column roles, callback counts, provenance, quoting, and physical naming.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitColumnNamingContractTest {
	@Test
	void basicIdentifierAndCollectionRolesParticipateOnce() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new RoleStrategy();
			final var physical = new PrefixStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Owner.class ), implicit, physical );
			final var owner = metadata.getEntityBinding( Owner.class.getName() );
			assertColumn( owner.getIdentifier().getColumns().get( 0 ), "p_identifier_id", true );
			assertColumn( owner.getProperty( "title" ).getColumns().get( 0 ), "p_basic_title", true );
			assertColumn( owner.getProperty( "explicit" ).getColumns().get( 0 ), "p_explicit_column", false );
			final var component = (Component) owner.getProperty( "details" ).getValue();
			assertColumn( component.getProperty( "zip" ).getColumns().get( 0 ), "p_basic_details_zip", true );
			final var values = (org.hibernate.mapping.List) metadata.getCollectionBinding( Owner.class.getName() + ".values" );
			assertColumn( values.getElement().getColumns().get( 0 ), "p_element_values", true );
			assertColumn( values.getIndex().getColumns().get( 0 ), "p_index_values", true );
			final var labels = (org.hibernate.mapping.Map) metadata.getCollectionBinding( Owner.class.getName() + ".labels" );
			assertColumn( labels.getElement().getColumns().get( 0 ), "p_element_labels", true );
			assertColumn( labels.getIndex().getColumns().get( 0 ), "p_key_labels", true );
			final var explicitValues = (org.hibernate.mapping.List) metadata.getCollectionBinding( Owner.class.getName() + ".explicitValues" );
			assertColumn( explicitValues.getElement().getColumns().get( 0 ), "p_chosen_element", false );
			assertColumn( explicitValues.getIndex().getColumns().get( 0 ), "p_chosen_order", false );
			assertThat( implicit.calls ).containsExactlyInAnyOrder(
					"identifier:id", "basic:title", "basic:details.zip", "element:values", "index:values", "element:labels", "key:labels" );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "basic_title" ) )
					.singleElement().satisfies( name -> assertThat( name.isExplicit() ).isFalse() );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "explicit_column" ) )
					.singleElement().satisfies( name -> assertThat( name.isExplicit() ).isTrue() );
		}
	}

	@Test
	void temporalCompanionSuffixPreservesQuotedBaseName() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( TemporalOwner.class ), new RoleStrategy(), new PrefixStrategy() );
			final var columns = metadata.getEntityBinding( TemporalOwner.class.getName() )
					.getProperty( "eventTime" ).getColumns();
			assertThat( columns ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( "p_basic_eventTime", "p_basic_eventTime_tz" );
			assertThat( columns ).allMatch( org.hibernate.mapping.Column::isQuoted );
		}
	}

	@Test
	void automaticQuotingWaitsForPhysicalTransformation() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Owner.class ), new StandardImplicitNamingStrategy() {
						@Override
						public LogicalName determineCollectionElementColumnName(CollectionElementColumnNamingInput input, ImplicitNamingContext context) {
							return context.implicitName( "element." + input.attributePath() );
						}
					}, new PhysicalNamingStrategyStandardImpl() {
						@Override
						public PhysicalName toPhysicalColumnName(LogicalName name, PhysicalNamingContext context) {
							if ( name.getText().startsWith( "element." ) ) {
								assertThat( name.isQuoted() ).isFalse();
							}
							return context.getPhysicalNameFactory().create( name.getText().replace( '.', '_' ), name.isQuoted() );
						}
					} );
			final var values = metadata.getCollectionBinding( Owner.class.getName() + ".values" );
			assertColumn( values.getElement().getColumns().get( 0 ), "element_values", false );
		}
	}

	@Test
	void anyColumnsUseTheirRolesAndPhysicalPolicy() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new RoleStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( AnyOwner.class ).addManagedClass( Target.class ),
					implicit, new PrefixStrategy() );
			final var any = (org.hibernate.mapping.Any) metadata.getEntityBinding( AnyOwner.class.getName() ).getProperty( "target" ).getValue();
			assertColumn( any.getDiscriminatorDescriptor().getColumns().get( 0 ), "p_any_kind_target", true );
			assertColumn( any.getKeyDescriptor().getColumns().get( 0 ), "p_any_key_target_0", true );
			assertThat( implicit.calls ).containsOnlyOnce( "any_kind:target", "any_key:target_0" );
		}
	}

	@Test
	void explicitOrMissingImplicitResultIsRejected() {
		for ( boolean missing : List.of( false, true ) ) {
			try (var registry = ServiceRegistryUtil.serviceRegistry()) {
				assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithImplicitNaming( registry,
						new MappingSources().addManagedClass( InvalidOwner.class ), new StandardImplicitNamingStrategy() {
							@Override
							public LogicalName determineBasicColumnName(BasicColumnNamingInput input, ImplicitNamingContext context) {
								return missing ? null : new LogicalName( "invalid", false, true );
							}
						} ) ).hasMessageContaining( "non-null implicit name for basic column" );
			}
		}
	}

	private static void assertColumn(org.hibernate.mapping.Column column, String text, boolean quoted) {
		assertThat( column.getName() ).isEqualTo( text );
		assertThat( column.isQuoted() ).isEqualTo( quoted );
	}

	static class RoleStrategy extends StandardImplicitNamingStrategy {
		final List<String> calls = new ArrayList<>();
		private LogicalName name(String role, String path, ImplicitNamingContext context) {
			calls.add( role + ':' + path );
			return context.implicitName( role + '_' + path.replace( '.', '_' ), true );
		}
		@Override
		public LogicalName determineBasicColumnName(BasicColumnNamingInput input, ImplicitNamingContext context) {
			return name( "basic", input.attributePath(), context );
		}
		@Override
		public LogicalName determineIdentifierColumnName(IdentifierColumnNamingInput input, ImplicitNamingContext context) {
			return name( "identifier", input.attributePath(), context );
		}
		@Override
		public LogicalName determineCollectionElementColumnName(CollectionElementColumnNamingInput input, ImplicitNamingContext context) {
			return name( "element", input.attributePath(), context );
		}
		@Override
		public LogicalName determineListIndexColumnName(ListIndexColumnNamingInput input, ImplicitNamingContext context) {
			return name( "index", input.attributePath(), context );
		}
		@Override
		public LogicalName determineMapKeyColumnName(MapKeyColumnNamingInput input, ImplicitNamingContext context) {
			return name( "key", input.attributePath(), context );
		}
		@Override
		public LogicalName determineAnyDiscriminatorColumnName(AnyColumnNamingInput input, ImplicitNamingContext context) {
			return name( "any_kind", input.attributePath(), context );
		}
		@Override
		public LogicalName determineAnyKeyColumnName(AnyColumnNamingInput input, ImplicitNamingContext context) {
			return name( "any_key", input.attributePath() + '_' + input.columnPosition(), context );
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

	@Entity(name = "ColumnOwner")
	static class Owner {
		@Id long id;
		String title;
		@Column(name = "explicit_column") String explicit;
		@Formula("1") int calculated;
		@Embedded Details details;
		@ElementCollection @OrderColumn List<String> values;
		@ElementCollection Map<String, String> labels;
		@ElementCollection @Column(name = "chosen_element") @OrderColumn(name = "chosen_order") List<String> explicitValues;
	}
	@Embeddable
	static class Details { String zip; }

	@Entity(name = "AnyColumnOwner")
	static class AnyOwner {
		@Id long id;
		@Any @AnyKeyJavaClass(Integer.class) @AnyDiscriminatorValue(discriminator = "target", entity = Target.class)
		Object target;
	}
	@Entity(name = "AnyColumnTarget")
	static class Target { @Id Integer id; }
	@Entity(name = "InvalidColumnOwner")
	static class InvalidOwner { @Id long id; String title; }
	@Entity(name = "TemporalColumnOwner")
	static class TemporalOwner {
		@Id long id;
		@org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.COLUMN)
		java.time.OffsetDateTime eventTime;
	}
}
