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
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.MappingException;
import org.hibernate.boot.serial.MetadataSerialization;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.PrimaryKeyNamingInput;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.descriptor.java.LongJavaType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies shared PK naming across mapped table kinds and unchanged schema emission.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitPrimaryKeyNamingTest {
	static MappingSources sources() {
		return new MappingSources().addManagedClasses( Owner.class, Target.class, Sub.class );
	}

	@Test
	void namesEachSurvivingKeyOnceWithBothTableNames() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new Recording();
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry, sources(), implicit, physical );
			assertThat( implicit.inputs ).extracting( input -> input.table().logicalName().getText() )
					.containsExactlyInAnyOrder( "owner", "details", "target", "sub", "tags", "items", "bag", "links", "ordered_links" );
			assertThat( physical.keys ).hasSize( implicit.inputs.size() );
			for ( var input : implicit.inputs ) {
				assertThat( input.table().names().physicalName().getText() )
						.isEqualTo( "p_" + input.table().logicalName().getText() );
				assertThat( input.table().logicalName().isExplicit() ).isTrue();
			}
			assertThat( physical.keys ).allMatch( name -> !name.isExplicit() );
			assertThat( metadata.getEntityBinding( Owner.class.getName() ).getTable().getPrimaryKey().getName() )
					.isEqualTo( "k_custom_owner" );
			final var database = metadata.getDatabase();
			final var sqlContext = SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
			final var ddl = database.getDialect().getTableExporter().getSqlCreateStrings(
					(org.hibernate.mapping.NamedTable) metadata.getEntityBinding( Owner.class.getName() ).getTable(), metadata, sqlContext );
			assertThat( String.join( "\n", ddl ) ).contains( "primary key" ).doesNotContain( "k_custom_owner" );
		}
	}

	@Test
	void preservesQuotingAndTruncatesOnlyDefaultGeneration() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Quoted.class ), new StandardImplicitNamingStrategy(), physical );
			assertThat( physical.keys ).singleElement().satisfies( name -> {
				assertThat( name.getText() ).isEqualTo( "p_abcdefghij_pk" );
				assertThat( name.isQuoted() ).isTrue();
			} );
			assertThat( metadata.getEntityBinding( Quoted.class.getName() ).getTable().getPrimaryKey().getName() )
					.isEqualTo( "\"k_p_abcdefghij_pk\"" );
		}
	}

	@Test
	void retainsExplicitUniqueKeyNameWhenAbsorbedIntoPrimaryKey() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Absorbed.class ), new StandardImplicitNamingStrategy(),
					new PhysicalNamingStrategyStandardImpl() );
			final var database = metadata.getDatabase();
			final var table = (org.hibernate.mapping.NamedTable) metadata.getEntityBinding( Absorbed.class.getName() ).getTable();
			assertThat( table.getUniqueKeys() ).doesNotContainKey( "chosen_pk" );
			assertThat( table.getPrimaryKey().getOrderingUniqueKey().getName() ).isEqualTo( "chosen_pk" );
			final var ddl = database.getDialect().getTableExporter().getSqlCreateStrings( table, metadata,
					SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() ) );
			assertThat( String.join( "\n", ddl ) ).contains( "constraint chosen_pk primary key" );
		}
	}

	@Test
	void rejectsNullAndExplicitImplicitResults() {
		for ( boolean explicit : new boolean[] { false, true } ) {
			try (var registry = ServiceRegistryUtil.serviceRegistry()) {
				final var strategy = new StandardImplicitNamingStrategy() {
					@Override
					@Nonnull
					@SuppressWarnings("DataFlowIssue") // Deliberately violates the SPI to verify result validation.
					public LogicalName determinePrimaryKeyName(@Nonnull PrimaryKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
						return explicit ? new LogicalName( "wrong", false, true ) : null;
					}
				};
				assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
						new MappingSources().addManagedClass( Quoted.class ), strategy, new PhysicalNamingStrategyStandardImpl() ) )
						.isInstanceOf( MappingException.class ).hasMessageContaining( "non-null implicit name for primary key" );
			}
		}
	}

	@Test
	void restorationDoesNotReplayNaming() throws Exception {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( MappingSettings.METADATA_SERIALIZATION_ENABLED, true ).build()) {
			final var implicit = new Recording();
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Quoted.class ), implicit, physical );
			final var bytes = new ByteArrayOutputStream();
			MetadataSerialization.serialize( (MetadataImplementor) metadata ).writeTo( bytes );
			final var restored = MetadataSerialization.read( new ByteArrayInputStream( bytes.toByteArray() ) )
					.restore( registry ).getMetadata();
			assertThat( restored.getEntityBinding( Quoted.class.getName() ).getTable().getPrimaryKey().getName() )
					.isEqualTo( "k_custom_abcdefghijklmnop" );
			assertThat( implicit.inputs ).hasSize( 1 );
			assertThat( physical.keys ).hasSize( 1 );
		}
	}

	@Test
	void inlineViewDoesNotInventAPhysicalTableDependency() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new Recording();
			final var physical = new Prefix();
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Inline.class ), implicit, physical );
			assertThat( implicit.inputs ).isEmpty();
			assertThat( physical.keys ).isEmpty();
		}
	}

	@Test
	void rejectsNullPhysicalResult() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var physical = new PhysicalNamingStrategyStandardImpl() {
				@Override
				@Nonnull
				@SuppressWarnings("DataFlowIssue") // Deliberately invalid strategy result.
				public PhysicalName toPhysicalPrimaryKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
					return null;
				}
			};
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Quoted.class ), new Recording(), physical ) )
					.isInstanceOf( MappingException.class )
					.hasMessageContaining( "Physical naming strategy returned null for primary key" );
		}
	}

	static class Recording extends StandardImplicitNamingStrategy {
		final List<PrimaryKeyNamingInput> inputs = new ArrayList<>();
		@Override
		@Nonnull
		public LogicalName determinePrimaryKeyName(@Nonnull PrimaryKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
			inputs.add( input );
			return context.implicitName( "custom_" + input.table().logicalName().getText() );
		}
	}

	static class Prefix extends PhysicalNamingStrategyStandardImpl {
		final List<LogicalName> keys = new ArrayList<>();
		@Override
		@Nonnull
		public PhysicalName toPhysicalTableName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
		@Override
		@Nonnull
		public PhysicalName toPhysicalPrimaryKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			keys.add( name );
			return context.getPhysicalNameFactory().create( "k_" + name.getText(), false );
		}
	}

	@Entity @org.hibernate.annotations.Subselect("select 1 as id")
	static class Inline { @Id long id; }
	@Entity @Table(name = "owner") @SecondaryTable(name = "details")
	static class Owner {
		@Id long id;
		@Column(table = "details") String description;
		@ElementCollection @CollectionTable(name = "tags") @Column(nullable = false) Set<String> tags;
		@ElementCollection @CollectionTable(name = "items") @OrderColumn List<String> items;
		@ElementCollection @CollectionTable(name = "bag")
		@CollectionId(generator = "increment") @CollectionIdJavaType(LongJavaType.class) Collection<String> bag;
		@OneToMany @JoinTable(name = "links") Set<Target> links;
		@OneToMany @JoinTable(name = "ordered_links") @OrderColumn List<Target> orderedLinks;
	}
	@Entity @Table(name = "target") @Inheritance(strategy = InheritanceType.JOINED)
	static class Target { @Id long id; }
	@Entity @Table(name = "sub")
	static class Sub extends Target { String extra; }
	@Entity @Table(name = "`abcdefghijklmnop`")
	static class Quoted { @Id long id; }
	@Entity @Table(name = "absorbed", uniqueConstraints = {
			@UniqueConstraint(name = "chosen_pk", columnNames = "id"),
			@UniqueConstraint(name = "chosen_code", columnNames = "code")})
	static class Absorbed { @Id long id; String code; }
}
