/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies resolved UK columns and explicit versus strategy-generated name collisions.
///
/// @author Steve Ebersole
@BaseUnitTest
class UniqueKeyColumnResolutionTest {
	@Test
	@JiraKey("HHH-20917")
	void entityLogicalReferenceReusesMappedColumn() {
		inspectEntity( LogicalReference.class, "p_code", "code", 1, true );
	}

	@Test
	@JiraKey("HHH-20917")
	void entityPhysicalReferenceResolvesWithoutTransformation() {
		inspectEntity( PhysicalReference.class, "p_code", "p_code", 0, true );
	}

	@Test
	@JiraKey("HHH-20917")
	void entityMissingReferenceFailsDuringBoot() {
		assertThatThrownBy( () -> inspectEntity( MissingReference.class, "p_missing", "missing", 1, false ) )
				.isInstanceOf( org.hibernate.AnnotationException.class ).hasMessageContaining( "missing" );
	}

	private void inspectEntity(Class<?> type, String expectedName, String callbackName, int calls, boolean exists) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( type ), new StandardImplicitNamingStrategy(), strategy );
			final var table = metadata.getEntityBinding( type.getName() ).getTable();
			final var key = table.getUniqueKeys().get( "chosen" );
			final var column = key.getColumn( 0 );
			assertThat( column.getName() ).isEqualTo( expectedName );
			assertThat( strategy.columns.stream().filter( callbackName::equals ).count() ).isEqualTo( calls );
			final var actual = table.getColumn( column );
			if ( exists ) {
				assertThat( actual ).isNotNull().isSameAs( column );
			}
			else {
				assertThat( actual ).isNull();
			}
		}
	}

	@Test
	void collectionReferencePrefersLogicalMatchOverDifferentPhysicalMatch() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( CollectionOwner.class ), new StandardImplicitNamingStrategy(), strategy );
			final var table = metadata.getCollectionBinding( CollectionOwner.class.getName() + ".values" ).getCollectionTable();
			final var keyColumn = table.getUniqueKeys().get( "chosen" ).getColumn( 0 );
			assertThat( keyColumn.getName() ).isEqualTo( "p_p_code" );
			assertThat( keyColumn ).isSameAs( table.getColumn( keyColumn ) );
			assertThat( table.getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.contains( "p_code", "p_p_code" );
			assertThat( strategy.columns.stream().filter( "p_code"::equals ).count() ).isEqualTo( 1 );
		}
	}

	@Test
	void repeatedExplicitNameMergesDifferentColumnSets() {
		inspectNameCollision( RepeatedName.class, new StandardImplicitNamingStrategy() );
	}

	@Test
	void customImplicitNameCollisionRejectsDifferentColumnSets() {
		assertThatThrownBy( () -> inspectNameCollision( ImplicitCollision.class, new StandardImplicitNamingStrategy() {
			@Override @Nonnull
			public LogicalName determineUniqueKeyName(
					@Nonnull org.hibernate.boot.model.naming.spi.UniqueKeyNamingInput input,
					@Nonnull org.hibernate.boot.model.naming.spi.ImplicitNamingContext context) {
				return context.implicitName( "same_name" );
			}
		} ) ).isInstanceOf( org.hibernate.MappingException.class ).hasMessageContaining( "collision" );
	}

	private void inspectNameCollision(Class<?> type, StandardImplicitNamingStrategy strategy) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( type ), strategy, new Prefix() );
			final var table = metadata.getEntityBinding( type.getName() ).getTable();
			assertThat( table.getUniqueKeys() ).hasSize( 1 );
			assertThat( table.getUniqueKeys().get( "same_name" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName )
					.containsExactly( "p_first", "p_second" );
		}
	}

	@Entity @Table(uniqueConstraints = {
			@UniqueConstraint(name = "same_name", columnNames = "first"),
			@UniqueConstraint(name = "same_name", columnNames = "second") })
	static class RepeatedName { @Id long id; String first; String second; }

	@Entity @Table(uniqueConstraints = {
			@UniqueConstraint(columnNames = "first"),
			@UniqueConstraint(columnNames = "second") })
	static class ImplicitCollision { @Id long id; String first; String second; }

	static class Prefix extends PhysicalNamingStrategyStandardImpl {
		final List<String> columns = new ArrayList<>();
		@Override @Nonnull
		public PhysicalName toPhysicalColumnName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			columns.add( name.getText() );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
	}

	@Entity @Table(uniqueConstraints = @UniqueConstraint(name = "chosen", columnNames = "code"))
	static class LogicalReference { @Id long id; @Column(name = "code") String value; }

	@Entity @Table(uniqueConstraints = @UniqueConstraint(name = "chosen", columnNames = "p_code"))
	static class PhysicalReference { @Id long id; @Column(name = "code") String value; }

	@Entity @Table(uniqueConstraints = @UniqueConstraint(name = "chosen", columnNames = "missing"))
	static class MissingReference { @Id long id; @Column(name = "code") String value; }

	@Entity
	static class CollectionOwner {
		@Id long id;
		@ElementCollection
		@CollectionTable(uniqueConstraints = @UniqueConstraint(name = "chosen", columnNames = "p_code"),
				joinColumns = @jakarta.persistence.JoinColumn(name = "code"))
		@Column(name = "p_code") Set<String> values;
	}
}
