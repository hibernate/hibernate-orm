/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.ForeignKeyNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
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

/// Verifies typed FK dependencies, explicit bypass, and paired composite references.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitForeignKeyNamingTest {
	@Test
	void exposesBothNamingStagesAndActualColumnPairs() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var inputs = new ArrayList<ForeignKeyNamingInput>();
			final var physicalNames = new ArrayList<LogicalName>();
			final var strategy = new StandardImplicitNamingStrategy() {
				@Override @Nonnull
				public LogicalName determineForeignKeyName(@Nonnull ForeignKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
					inputs.add( input );
					return context.implicitName( input.referencesPrimaryKey() ? "pk_reference" : "alternate_reference", true );
				}
			};
			final var physical = new Prefix() {
				@Override @Nonnull
				public PhysicalName toPhysicalForeignKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
					physicalNames.add( name );
					return context.getPhysicalNameFactory().create( "fk_" + name.getText(), false );
				}
			};
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Owner.class, Target.class ), strategy, physical );
			assertThat( inputs ).hasSize( 2 );
			for ( var input : inputs ) {
				assertThat( input.table().logicalName().getText() ).isEqualTo( "fk_owner" );
				assertThat( ((NamedTableNamingInput) input.table()).names().physicalName().getText() ).isEqualTo( "p_fk_owner" );
				assertThat( input.referencedTable().logicalName().getText() ).isEqualTo( "fk_target" );
				assertThat( ((NamedTableNamingInput) input.referencedTable()).names().physicalName().getText() ).isEqualTo( "p_fk_target" );
				input.columns().forEach( pair -> {
					assertThat( pair.localColumn().physicalName().getText() ).isEqualTo( "p_" + pair.localColumn().logicalName().getText() );
					assertThat( pair.referencedColumn().physicalName().getText() ).isEqualTo( "p_" + pair.referencedColumn().logicalName().getText() );
				} );
			}
			final var pk = inputs.stream().filter( ForeignKeyNamingInput::referencesPrimaryKey ).findFirst().orElseThrow();
			assertThat( pk.columns() ).extracting( pair -> pair.localColumn().logicalName().getText() + "->" + pair.referencedColumn().logicalName().getText() )
					.containsExactlyInAnyOrder( "a_fk->a", "b_fk->b" );
			final var alternate = inputs.stream().filter( input -> !input.referencesPrimaryKey() ).findFirst().orElseThrow();
			assertThat( alternate.columns() ).singleElement().satisfies( pair ->
					assertThat( pair.referencedColumn().logicalName().getText() ).isEqualTo( "business_code" ) );
			assertThat( physicalNames ).hasSize( 3 );
			assertThat( physicalNames.stream().filter( LogicalName::isExplicit ) ).singleElement()
					.satisfies( name -> assertThat( name.getText() ).isEqualTo( "declared" ) );
			assertThat( metadata.getEntityBinding( Owner.class.getName() ).getTable().getForeignKeyCollection() )
					.extracting( org.hibernate.mapping.ForeignKey::getName )
					.containsExactlyInAnyOrder( "\"fk_pk_reference\"", "\"fk_alternate_reference\"", "fk_declared" );
			assertThatThrownBy( () -> pk.columns().clear() ).isInstanceOf( UnsupportedOperationException.class );
		}
	}

	@Test
	void rejectsNullResult() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new StandardImplicitNamingStrategy() {
				@Override @Nonnull
				@SuppressWarnings("DataFlowIssue")
				public LogicalName determineForeignKeyName(@Nonnull ForeignKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
					return null;
				}
			};
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Owner.class, Target.class ), strategy, new Prefix() ) )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "null for FOREIGN_KEY" );
		}
	}

	@Test
	void metadataRestorationDoesNotReplayForeignKeyNaming() throws Exception {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( org.hibernate.cfg.MappingSettings.METADATA_SERIALIZATION_ENABLED, true ).build()) {
			final var inputs = new ArrayList<ForeignKeyNamingInput>();
			final var strategy = new StandardImplicitNamingStrategy() {
				@Override @Nonnull
				public LogicalName determineForeignKeyName(@Nonnull ForeignKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
					inputs.add( input );
					return super.determineForeignKeyName( input, context );
				}
			};
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Owner.class, Target.class ), strategy, new Prefix() );
			final var before = metadata.getEntityBinding( Owner.class.getName() ).getTable().getForeignKeyCollection()
					.stream().map( org.hibernate.mapping.ForeignKey::getName ).toList();
			assertThat( before ).containsExactlyInAnyOrder( "FKidyfddstv2sm4fnn2feexcbck", "FK8gt6upwt33van9w5kqah0vxmr", "declared" );
			final var bytes = new java.io.ByteArrayOutputStream();
			org.hibernate.boot.serial.MetadataSerialization.serialize( (org.hibernate.boot.spi.MetadataImplementor) metadata ).writeTo( bytes );
			final var restored = org.hibernate.boot.serial.MetadataSerialization.read(
					new java.io.ByteArrayInputStream( bytes.toByteArray() ) ).restore( registry ).getMetadata();
			assertThat( restored.getEntityBinding( Owner.class.getName() ).getTable().getForeignKeyCollection() )
					.extracting( org.hibernate.mapping.ForeignKey::getName ).containsExactlyInAnyOrderElementsOf( before );
			assertThat( inputs ).hasSize( 2 );
		}
	}

	static class Prefix extends PhysicalNamingStrategyStandardImpl {
		@Override @Nonnull
		public PhysicalName toPhysicalTableName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
		@Override @Nonnull
		public PhysicalName toPhysicalColumnName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
	}

	@Entity @Table(name = "fk_target")
	static class Target {
		@Id long a;
		@Id long b;
		@Column(name = "business_code", unique = true) String businessCode;
	}
	@Entity @Table(name = "fk_owner")
	static class Owner {
		@Id long id;
		@ManyToOne @JoinColumns({ @JoinColumn(name = "b_fk", referencedColumnName = "b"), @JoinColumn(name = "a_fk", referencedColumnName = "a") })
		Target target;
		@ManyToOne @JoinColumn(name = "code_fk", referencedColumnName = "business_code")
		Target byCode;
		@ManyToOne @JoinColumns(value = { @JoinColumn(name = "explicit_a", referencedColumnName = "a"), @JoinColumn(name = "explicit_b", referencedColumnName = "b") }, foreignKey = @ForeignKey(name = "declared"))
		Target explicit;
	}
}
