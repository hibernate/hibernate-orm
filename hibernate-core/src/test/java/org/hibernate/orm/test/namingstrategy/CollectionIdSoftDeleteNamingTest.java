/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.CollectionIdColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.SoftDeleteColumnNamingInput;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.serial.MetadataSerialization;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.descriptor.java.LongJavaType;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Naming of collection row identifiers and soft-delete indicators.
///
/// @author Steve Ebersole
@BaseUnitTest
class CollectionIdSoftDeleteNamingTest {
	@Test
	void supportedMappingShapes() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Owner.class, Details.class ),
					new StandardImplicitNamingStrategy(), new ImplicitNamingStrategyMatrixTest.PhysicalStrategy( "p_" ) );
			for (String path : new String[] { "items", "details.items" }) {
				final var collection = (IdentifierCollection) metadata.getCollectionBinding( Owner.class.getName() + "." + path );
				assertThat( collection.getIdentifier().getColumns().get( 0 ).getName() ).isEqualTo( "p_id" );
				assertThat( collection.getSoftDeleteColumn().getName() ).isEqualTo( "p_deleted" );
			}
			assertThat( metadata.getEntityBinding( Owner.class.getName() ).getRootClass().getSoftDeleteColumn().getName() ).isEqualTo( "p_deleted" );
		}
	}

	@ParameterizedTest
	@MethodSource("org.hibernate.orm.test.namingstrategy.ImplicitNamingStrategyMatrixTest#cases")
	void defaultsAcrossStrategies(ImplicitNamingStrategyMatrixTest.Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Owner.class, Details.class, Active.class, Timestamp.class ),
					test.strategy().implementation, new ImplicitNamingStrategyMatrixTest.PhysicalStrategy( test.prefix() ) );
			for (String path : List.of( "items", "details.items" )) {
				final var bag = (IdentifierCollection) metadata.getCollectionBinding( Owner.class.getName() + "." + path );
				assertThat( bag.getIdentifier().getColumns().get( 0 ).getName() ).isEqualTo( test.physical( "id" ) );
				assertThat( bag.getSoftDeleteColumn().getName() ).isEqualTo( test.physical( "deleted" ) );
			}
			for (Class<?> type : List.of( Owner.class, Active.class, Timestamp.class )) {
				final var indicator = metadata.getEntityBinding( type.getName() ).getRootClass().getSoftDeleteColumn();
				assertThat( indicator.getName() ).isEqualTo( test.physical( type == Active.class ? "active" : "deleted" ) );
				assertThat( indicator.isNullable() ).isEqualTo( type == Timestamp.class );
				if ( type != Owner.class ) {
					final var collection = metadata.getCollectionBinding( type.getName() + ".items" );
					assertThat( collection.getSoftDeleteColumn().getName() ).isEqualTo( indicator.getName() );
					assertThat( collection.getSoftDeleteColumn().isNullable() ).isEqualTo( type == Timestamp.class );
				}
			}
		}
	}

	@Test
	void inputFactsExplicitBypassAndProvenance() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new Recording();
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Owner.class, Details.class, Explicit.class ), implicit, physical );
			assertThat( implicit.ids ).extracting( CollectionIdColumnNamingInput::attributePath )
					.containsExactlyInAnyOrder( "items", "details.items" );
			assertThat( implicit.soft ).hasSize( 3 );
			assertThat( implicit.soft ).filteredOn( i -> i.kind() == SoftDeleteColumnNamingInput.Kind.ENTITY )
					.singleElement().satisfies( i -> assertThat( i.attributePath() ).isEmpty() );
			assertThat( implicit.ids ).allSatisfy( i -> {
				assertThat( i.owner().getClassName() ).isEqualTo( Owner.class.getName() );
				final var table = ((NamedTableNamingInput) i.table()).names();
				assertThat( table.physicalName().getText() ).isEqualTo( "p_" + table.logicalName().getText() );
				assertThat( implicit.soft ).anySatisfy( soft -> {
					assertThat( soft.attributePath() ).contains( i.attributePath() );
					assertThat( soft.table() ).isEqualTo( i.table() );
				} );
			} );
			assertThat( physical.inputs ).filteredOn( n -> n.getText().startsWith( "custom_" ) ).hasSize( 5 )
					.allSatisfy( n -> { assertThat( n.isExplicit() ).isFalse(); assertThat( n.isQuoted() ).isTrue(); } );
			assertThat( physical.inputs ).filteredOn( n -> List.of( "row_key", "entity_flag", "collection_flag" ).contains( n.getText() ) )
					.hasSize( 3 ).allSatisfy( n -> assertThat( n.isExplicit() ).isTrue() );
			final var bag = (IdentifierCollection) metadata.getCollectionBinding( Explicit.class.getName() + ".items" );
			final var column = bag.getIdentifier().getColumns().get( 0 );
			assertThat( column.getName() ).isEqualTo( "p_row_key" );
			assertThat( column.isQuoted() ).isTrue();
			assertThat( column.getComment() ).isEqualTo( "row identity" );
			assertThat( bag.getCollectionTable().getColumn( column.getPhysicalName() ) ).isSameAs( column );
			assertThat( bag.getCollectionTable().getColumn( bag.getSoftDeleteColumn().getPhysicalName() ) ).isSameAs( bag.getSoftDeleteColumn() );
			assertThat( bag.getSoftDeleteColumn().getComment() ).isEqualTo( "collection indicator" );
			final var collectionTable = (org.hibernate.mapping.PhysicalTable) bag.getCollectionTable();
			assertThat( collectionTable.getIndex( "row_key_lookup" ).getSelectables() ).containsExactly( column );
			assertThat( collectionTable.getIndex( "collection_flag_lookup" ).getSelectables() ).containsExactly( bag.getSoftDeleteColumn() );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void rejectsInvalidImplicitResults(boolean returnNull) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			for (boolean collectionId : new boolean[] {true, false}) {
				final var strategy = new StandardImplicitNamingStrategy() {
					@Override
					@Nonnull
					@SuppressWarnings("DataFlowIssue") // Deliberately invalid strategy result.
					public LogicalName determineCollectionIdColumnName(@Nonnull CollectionIdColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
						return collectionId ? (returnNull ? null : new LogicalName( "invalid", false, true )) : super.determineCollectionIdColumnName( input, context );
					}
					@Override
					@Nonnull
					@SuppressWarnings("DataFlowIssue") // Deliberately invalid strategy result.
					public LogicalName determineSoftDeleteColumnName(@Nonnull SoftDeleteColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
						return collectionId ? super.determineSoftDeleteColumnName( input, context ) : (returnNull ? null : new LogicalName( "invalid", false, true ));
					}
				};
				assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
						new MappingSources().addManagedClasses( Owner.class, Details.class ), strategy, new Prefix() ) )
						.hasMessageContaining( "non-null implicit name for " + (collectionId ? "collection identifier" : "soft-delete") + " column" );
			}
		}
	}

	@Test
	void validatesInputShapes() {
		final var owner = new org.hibernate.boot.model.naming.spi.EntityNamingInput( "Owner", "Owner", "Owner" );
		final var table = new org.hibernate.boot.model.naming.spi.InlineViewNamingInput( new LogicalName( "view", false, false ) );
		assertThatThrownBy( () -> new CollectionIdColumnNamingInput( owner, "", table ) ).isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> new CollectionIdColumnNamingInput( owner, "items", null ) ).isInstanceOf( NullPointerException.class );
		assertThatThrownBy( () -> new SoftDeleteColumnNamingInput( owner, SoftDeleteColumnNamingInput.Kind.ENTITY, Optional.of( "items" ), table, SoftDeleteType.DELETED ) ).isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> new SoftDeleteColumnNamingInput( owner, SoftDeleteColumnNamingInput.Kind.COLLECTION, Optional.empty(), table, SoftDeleteType.DELETED ) ).isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> new SoftDeleteColumnNamingInput( owner, SoftDeleteColumnNamingInput.Kind.COLLECTION, Optional.of( "" ), table, SoftDeleteType.DELETED ) ).isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> new SoftDeleteColumnNamingInput( owner, SoftDeleteColumnNamingInput.Kind.ENTITY, null, table, SoftDeleteType.DELETED ) ).isInstanceOf( NullPointerException.class );
	}

	@Test
	void transformedQuotedNamesWorkAtRuntime() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "jakarta.persistence.schema-generation.database.action", "create-drop" ).build()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Owner.class, Details.class ), new Recording(), new Prefix() {
						@Override
						@Nonnull
						public PhysicalName toPhysicalTableName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
							return context.getPhysicalNameFactory().create( "p_" + name.getText(), true );
						}
					} );
			try (var factory = SessionFactoryPipeline.build( metadata, new SessionFactoryOptionsCollector() )) {
				try (var session = factory.openSession()) {
					final var tx = session.beginTransaction();
					final var owner = new Owner();
					owner.id = 1;
					owner.items = new ArrayList<>( List.of( "one", "two" ) );
					owner.details = new Details();
					owner.details.items = new ArrayList<>( List.of( "embedded" ) );
					session.persist( owner );
					tx.commit();
				}
				try (var session = factory.openSession()) {
					final var tx = session.beginTransaction();
					final var owner = session.find( Owner.class, 1L );
					assertThat( owner.items ).containsExactlyInAnyOrder( "one", "two" );
					assertThat( owner.details.items ).containsExactly( "embedded" );
					owner.items.remove( "one" );
					tx.commit();
				}
				try (var session = factory.openSession()) {
					final var tx = session.beginTransaction();
					final var owner = session.find( Owner.class, 1L );
					assertThat( owner.items ).containsExactly( "two" );
					session.remove( owner );
					tx.commit();
				}
				try (var session = factory.openSession()) {
					assertThat( session.find( Owner.class, 1L ) ).isNull();
				}
			}
		}
	}

	@Test
	void archivesDoNotReplayNaming() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().applySetting( MappingSettings.METADATA_SERIALIZATION_ENABLED, true ).build()) {
			final var implicit = new Recording();
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Owner.class, Details.class ), implicit, physical );
			final int physicalCount = physical.inputs.size();
			final var bytes = new ByteArrayOutputStream();
			MetadataSerialization.serialize( (MetadataImplementor) metadata ).writeTo( bytes );
			final var restored = MetadataSerialization.read( new ByteArrayInputStream( bytes.toByteArray() ) ).restore( registry ).getMetadata();
			final var bag = (IdentifierCollection) restored.getCollectionBinding( Owner.class.getName() + ".items" );
			assertThat( bag.getIdentifier().getColumns().get( 0 ).getName() ).isEqualTo( "p_custom_id" );
			assertThat( bag.getIdentifier().getColumns().get( 0 ).isQuoted() ).isTrue();
			assertThat( bag.getSoftDeleteColumn().getName() ).isEqualTo( "p_custom_deleted" );
			assertThat( bag.getSoftDeleteColumn().isQuoted() ).isTrue();
			final var entityIndicator = restored.getEntityBinding( Owner.class.getName() ).getRootClass().getSoftDeleteColumn();
			assertThat( entityIndicator.getName() ).isEqualTo( "p_custom_deleted" );
			assertThat( entityIndicator.isQuoted() ).isTrue();
			assertThat( implicit.ids ).hasSize( 2 );
			assertThat( implicit.soft ).hasSize( 3 );
			assertThat( physical.inputs ).hasSize( physicalCount );
		}
	}

	@Test
	void normalizedXmlCollectionIdentifierUsesPhysicalNaming() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new Recording();
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addMappingResource( "org/hibernate/orm/test/schemaupdate/idbag/Mappings.orm.xml" ),
					implicit, physical );
			final var bag = (IdentifierCollection) metadata.getCollectionBinding( "org.hibernate.orm.test.schemaupdate.idbag.IdBagOwner.children" );
			assertThat( bag.getIdentifier().getColumns().get( 0 ).getName() ).isEqualTo( "p_bag_id" );
			assertThat( implicit.ids ).isEmpty();
			assertThat( physical.inputs ).filteredOn( n -> n.getText().equals( "bag_id" ) ).singleElement()
					.satisfies( n -> assertThat( n.isExplicit() ).isTrue() );
		}
	}

	@Test
	void embeddableAndAssociationIdentifierBags() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new Recording();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( OtherShapes.class, Element.class, Target.class ), implicit, new Prefix() );
			assertThat( implicit.ids ).extracting( CollectionIdColumnNamingInput::attributePath )
					.containsExactlyInAnyOrder( "elements", "targets" );
			for (String path : List.of( "elements", "targets" )) {
				final var bag = (IdentifierCollection) metadata.getCollectionBinding( OtherShapes.class.getName() + "." + path );
				assertThat( bag.getIdentifier().getColumns().get( 0 ).getName() ).isEqualTo( "p_custom_id" );
			}
		}
	}

	@Entity(name = "NamingOtherShapes")
	static class OtherShapes {
		@Id long id;
		@ElementCollection
		@CollectionId(generator = "increment")
		@CollectionIdJavaType(LongJavaType.class)
		Collection<Element> elements;
		@jakarta.persistence.ManyToMany
		@CollectionId(generator = "increment")
		@CollectionIdJavaType(LongJavaType.class)
		Collection<Target> targets;
	}

	@Embeddable
	static class Element {
		String text;
	}

	@Entity(name = "NamingTarget")
	static class Target {
		@Id long id;
	}

	static class Recording extends StandardImplicitNamingStrategy {
		final List<CollectionIdColumnNamingInput> ids = new ArrayList<>();
		final List<SoftDeleteColumnNamingInput> soft = new ArrayList<>();
		@Override
		@Nonnull
		public LogicalName determineCollectionIdColumnName(@Nonnull CollectionIdColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
			ids.add( input );
			return context.implicitName( "custom_id", true );
		}
		@Override
		@Nonnull
		public LogicalName determineSoftDeleteColumnName(@Nonnull SoftDeleteColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
			soft.add( input );
			return context.implicitName( "custom_" + input.strategy().getDefaultColumnName(), true );
		}
	}

	static class Prefix extends PhysicalNamingStrategyStandardImpl {
		final List<LogicalName> inputs = new ArrayList<>();
		@Override
		@Nonnull
		public PhysicalName toPhysicalColumnName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			inputs.add( name );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), false );
		}
		@Override
		@Nonnull
		public PhysicalName toPhysicalTableName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), false );
		}
	}

	@Entity(name = "NamingActive")
	@SoftDelete(strategy = SoftDeleteType.ACTIVE)
	static class Active {
		@Id long id;
		@ElementCollection @SoftDelete(strategy = SoftDeleteType.ACTIVE) Collection<String> items;
	}

	@Entity(name = "NamingTimestamp")
	@SoftDelete(strategy = SoftDeleteType.TIMESTAMP)
	static class Timestamp {
		@Id long id;
		@ElementCollection @SoftDelete(strategy = SoftDeleteType.TIMESTAMP) Collection<String> items;
	}

	@Entity(name = "NamingExplicit")
	@SoftDelete(columnName = "entity_flag")
	static class Explicit {
		@Id long id;
		@ElementCollection
		@jakarta.persistence.CollectionTable(indexes = {
				@jakarta.persistence.Index(name = "row_key_lookup", columnList = "`row_key`"),
				@jakarta.persistence.Index(name = "collection_flag_lookup", columnList = "collection_flag")
		})
		@CollectionId(generator = "increment", column = @jakarta.persistence.Column(name = "`row_key`", comment = "row identity"))
		@CollectionIdJavaType(LongJavaType.class)
		@SoftDelete(columnName = "collection_flag", comment = "collection indicator")
		Collection<String> items;
	}

	@Entity(name = "NamingOwner")
	@SoftDelete
	static class Owner {
		@Id long id;
		@ElementCollection
		@CollectionId(generator = "increment")
		@CollectionIdJavaType(LongJavaType.class)
		@SoftDelete
		Collection<String> items;
		@Embedded Details details;
	}

	@Embeddable
	static class Details {
		@ElementCollection
		@CollectionId(generator = "increment")
		@CollectionIdJavaType(LongJavaType.class)
		@SoftDelete
		Collection<String> items;
	}
}
