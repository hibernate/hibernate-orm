/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.model.naming.spi.UniqueKeyNamingInput;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies settled UK dependencies, strategy boundaries, and stable finalized names.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitUniqueKeyNamingTest {
	@Test
	void pairedDependenciesAndExplicitBypassSurviveRestoration() throws Exception {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( org.hibernate.cfg.MappingSettings.METADATA_SERIALIZATION_ENABLED, true ).build()) {
			final var inputs = new ArrayList<UniqueKeyNamingInput>();
			final var physicalCalls = new ArrayList<LogicalName>();
			final var strategy = new StandardImplicitNamingStrategy() {
				@Override @Nonnull
				public LogicalName determineUniqueKeyName(@Nonnull UniqueKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
					inputs.add( input );
					return context.implicitName( "generated", true );
				}
			};
			final var physical = new UniqueKeyColumnResolutionTest.Prefix() {
				@Override @Nonnull
				public PhysicalName toPhysicalUniqueKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
					physicalCalls.add( name );
					return context.getPhysicalNameFactory().create( "uk_" + name.getText(), false );
				}
			};
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Pair.class ), strategy, physical );
			assertThat( inputs ).hasSize( 1 );
			final var input = inputs.get( 0 );
			assertThat( input.table().logicalName().getText() ).isEqualTo( "paired" );
			assertThat( input.columns() ).extracting( pair -> pair.logicalName().getText() + "->" + pair.physicalName().getText() )
					.containsExactly( "second->p_second", "First->p_First" );
			assertThat( input.columns().get( 1 ).logicalName().isQuoted() ).isTrue();
			assertThatThrownBy( () -> input.columns().clear() ).isInstanceOf( UnsupportedOperationException.class );
			assertThat( physicalCalls ).hasSize( 2 );
			final var table = metadata.getEntityBinding( Pair.class.getName() ).getTable();
			assertThat( table.getUniqueKeys().keySet() ).containsExactlyInAnyOrder( "\"uk_generated\"", "uk_declared" );
			final var database = metadata.getDatabase();
			final var sqlContext = org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
			final var exporter = database.getDialect().getTableExporter();
			final var firstDdl = exporter.getSqlCreateStrings( (org.hibernate.mapping.NamedTable) table, metadata, sqlContext );
			assertThat( String.join( " ", firstDdl ) ).contains( "constraint \"uk_generated\"", "constraint uk_declared" );
			assertThat( exporter.getSqlCreateStrings( (org.hibernate.mapping.NamedTable) table, metadata, sqlContext ) ).containsExactly( firstDdl );
			final var bytes = new java.io.ByteArrayOutputStream();
			org.hibernate.boot.serial.MetadataSerialization.serialize( (org.hibernate.boot.spi.MetadataImplementor) metadata ).writeTo( bytes );
			final var restored = org.hibernate.boot.serial.MetadataSerialization.read(
					new java.io.ByteArrayInputStream( bytes.toByteArray() ) ).restore( registry ).getMetadata();
			assertThat( restored.getEntityBinding( Pair.class.getName() ).getTable().getUniqueKeys().keySet() )
					.containsExactlyInAnyOrderElementsOf( table.getUniqueKeys().keySet() );
			assertThat( inputs ).hasSize( 1 );
			assertThat( physicalCalls ).hasSize( 2 );
		}
	}

	@Test
	void rejectsNullImplicitResult() {
		assertThatThrownBy( () -> build( new StandardImplicitNamingStrategy() {
			@Override @Nonnull
			public LogicalName determineUniqueKeyName(@Nonnull UniqueKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
				return null;
			}
		}, new PhysicalNamingStrategyStandardImpl(), Pair.class ) ).isInstanceOf( MappingException.class );
	}

	@Test
	void rejectsNullPhysicalResult() {
		assertThatThrownBy( () -> build( new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() {
			@Override @Nonnull
			public PhysicalName toPhysicalUniqueKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
				return null;
			}
		}, Pair.class ) ).isInstanceOf( MappingException.class );
	}

	@Test
	void differentExplicitNamesCollidingPhysicallyDoNotMerge() {
		assertThatThrownBy( () -> build( new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() {
			@Override @Nonnull
			public PhysicalName toPhysicalUniqueKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
				return context.getPhysicalNameFactory().create( "collision", false );
			}
		}, DifferentNames.class ) ).isInstanceOf( MappingException.class ).hasMessageContaining( "collision" );
	}

	@Test
	void absorbedUnnamedKeyDoesNotInvokeUniqueNaming() {
		build( new StandardImplicitNamingStrategy() {
			@Override @Nonnull
			public LogicalName determineUniqueKeyName(@Nonnull UniqueKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
				throw new AssertionError( "Absorbed UK must not invoke naming" );
			}
		}, new PhysicalNamingStrategyStandardImpl(), Absorbed.class );
	}

	private void build(StandardImplicitNamingStrategy implicit, PhysicalNamingStrategyStandardImpl physical, Class<?> type) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( type ), implicit, physical );
		}
	}

	@Entity @Table(name = "paired", uniqueConstraints = {
			@UniqueConstraint(columnNames = {"second", "`First`"}),
			@UniqueConstraint(name = "declared", columnNames = "third") })
	static class Pair {
		@Id long id;
		@Column(name = "`First`") String first;
		String second;
		String third;
	}

    @Entity @Table(uniqueConstraints = {
            @UniqueConstraint(name = "first_key", columnNames = "first"),
            @UniqueConstraint(name = "second_key", columnNames = "second") })
    static class DifferentNames { @Id long id; String first; String second; }

	@Entity @Table(uniqueConstraints = @UniqueConstraint(columnNames = "id"))
	static class Absorbed { @Id long id; }
}
