/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.boot.model.naming.spi.ForeignKeyNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.IndexNamingInput;
import org.hibernate.boot.model.naming.spi.UniqueKeyNamingInput;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
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

/// Checks physical naming participation and explicit-name bypass in constraint binding.
///
/// @author Steve Ebersole
@BaseUnitTest
class PhysicalConstraintNamingTest {
	@Test
	void explicitAndImplicitNamesReachThePhysicalRole() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var physical = new ConstraintStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Owner.class ).addManagedClass( Target.class ),
					new BypassCheckingStrategy(), physical );
			final var table = (org.hibernate.mapping.PhysicalTable) metadata.getEntityBinding( Owner.class.getName() ).getTable();
			assertThat( table.getPrimaryKey().getName() ).startsWith( "pk_" );
			assertThat( table.getForeignKeyCollection() ).extracting( org.hibernate.mapping.ForeignKey::getName )
					.allMatch( name -> name.startsWith( "fk_" ) ).contains( "fk_owner_target" );
			assertThat( table.getIndexes().keySet() ).contains( "ix_owner_code" );
			assertThat( table.getIndexes().keySet() ).contains( "\"ix_QuotedIndex\"" );
			assertThat( table.getUniqueKeys().keySet() ).contains( "uk_owner_code" );
			assertThat( physical.foreignKeys ).anyMatch( LogicalName::isExplicit ).anyMatch( name -> !name.isExplicit() );
			assertThat( physical.foreignKeys ).hasSize( 2 );
			assertThat( physical.indexes ).anyMatch( LogicalName::isExplicit ).anyMatch( name -> !name.isExplicit() );
			assertThat( physical.uniqueKeys ).anyMatch( LogicalName::isExplicit ).anyMatch( name -> !name.isExplicit() );
		}
	}

	static class BypassCheckingStrategy extends StandardImplicitNamingStrategy {
		@Override
		@Nonnull
		public LogicalName determineForeignKeyName(@Nonnull ForeignKeyNamingInput input, @Nonnull ImplicitNamingContext context) {

			return super.determineForeignKeyName( input, context );
		}
		@Override
		@Nonnull
		public LogicalName determineIndexName(@Nonnull IndexNamingInput input, @Nonnull ImplicitNamingContext context) {
			return super.determineIndexName( input, context );
		}
		@Override
		@Nonnull
		public LogicalName determineUniqueKeyName(@Nonnull UniqueKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
			return super.determineUniqueKeyName( input, context );
		}
	}

	static class ConstraintStrategy extends PhysicalNamingStrategyStandardImpl {
		final List<LogicalName> foreignKeys = new ArrayList<>();
		final List<LogicalName> indexes = new ArrayList<>();
		final List<LogicalName> uniqueKeys = new ArrayList<>();
		@Override
		@Nonnull
		public PhysicalName toPhysicalPrimaryKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( "pk_" + name.getText(), name.isQuoted() );
		}
		@Override
		@Nonnull
		public PhysicalName toPhysicalForeignKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			foreignKeys.add( name );
			return context.getPhysicalNameFactory().create( "fk_" + name.getText(), name.isQuoted() );
		}
		@Override
		@Nonnull
		public PhysicalName toPhysicalIndexName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			indexes.add( name );
			return context.getPhysicalNameFactory().create( "ix_" + name.getText(), false );
		}
		@Override
		@Nonnull
		public PhysicalName toPhysicalUniqueKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			uniqueKeys.add( name );
			return context.getPhysicalNameFactory().create( "uk_" + name.getText(), name.isQuoted() );
		}
	}

	@Entity(name = "Owner")
	@Table(indexes = {@Index(name = "owner_code", columnList = "code"), @Index(columnList = "other"),
			@Index(name = "`QuotedIndex`", columnList = "code,other")},
			uniqueConstraints = {@UniqueConstraint(name = "owner_code", columnNames = "code"), @UniqueConstraint(columnNames = "other")})
	static class Owner {
		@Id long id;
		String code;
		String other;
		@ManyToOne @JoinColumn(foreignKey = @ForeignKey(name = "owner_target")) Target explicitTarget;
		@ManyToOne Target implicitTarget;
	}
	@Entity(name = "Target")
	static class Target { @Id long id; }
}
