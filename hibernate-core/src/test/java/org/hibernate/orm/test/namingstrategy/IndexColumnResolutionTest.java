/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.Metadata;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies index resolution and approved corrections characterized before migration.
///
/// @author Steve Ebersole
@BaseUnitTest
class IndexColumnResolutionTest {
	@Test
	void entityReusesMappedColumnWithoutRepeatingPhysicalNaming() {
		final var strategy = new Prefix();
		inspect( Baseline.class, strategy, metadata -> {
			final var table = (org.hibernate.mapping.PhysicalTable) metadata.getEntityBinding( Baseline.class.getName() ).getTable();
			final var index = table.getIndexes().values().iterator().next();
			final var column = (org.hibernate.mapping.Column) index.getSelectables().get( 0 );
			assertThat( column.getName() ).isEqualTo( "p_code" );
			assertThat( column ).isSameAs( table.getColumn( column ) );
			assertThat( strategy.columns.stream().filter( "code"::equals ).count() ).isEqualTo( 1 );
			// Independent MD5/base-35 expectation: logical table + source column text, not p_code.
			assertThat( index.getName() ).isEqualTo( "IDX9bkm8xdpfbxpm6ywf7riilsgi" );
		} );
	}

	@Test
	void entityUnknownColumnFailsDuringBoot() {
		assertThatThrownBy( () -> inspect( Missing.class, new Prefix(), metadata -> {} ) )
				.isInstanceOf( org.hibernate.AnnotationException.class )
				.hasMessageContaining( "unknown column 'missing'" )
				.hasMessageContaining( "@Table.indexes[0]" );
	}

	@Test
	void collectionDirectionIsParsedSeparately() {
		inspect( OrderedCollection.class, new Prefix(), metadata -> {
			final var table = (org.hibernate.mapping.PhysicalTable) metadata.getCollectionBinding(
					OrderedCollection.class.getName() + ".values" ).getCollectionTable();
			final var index = table.getIndexes().get( "ordered_index" );
			assertThat( index.getSelectables() ).extracting( org.hibernate.mapping.Selectable::getText ).containsExactly( "p_entry" );
			assertThat( index.getSelectableOrderMap().values() ).containsExactly( "desc" );
		} );
	}

	@Test
	void oppositeTermOrdersReportNameCollision() {
		assertThatThrownBy( () -> inspect( OppositeOrders.class, new PhysicalNamingStrategyStandardImpl(), metadata -> {} ) )
				.isInstanceOf( org.hibernate.MappingException.class ).hasMessageContaining( "Index naming collision" );
	}

	@Test
	void repeatedExplicitNamesAreRejected() {
		assertThatThrownBy( () -> inspect( RepeatedName.class, new PhysicalNamingStrategyStandardImpl(), metadata -> {} ) )
				.isInstanceOf( org.hibernate.AnnotationException.class ).hasMessageContaining( "same_index" )
				.hasMessageContaining( RepeatedName.class.getName() )
				.hasMessageContaining( "@Table.indexes[0]" ).hasMessageContaining( "@Table.indexes[1]" );
	}

	@Test
	void entityAndCollectionUniqueIndexesUseTheSameRepresentation() {
		inspect( UniqueIndexes.class, new PhysicalNamingStrategyStandardImpl(), metadata -> {
			final var table = (org.hibernate.mapping.PhysicalTable) metadata.getEntityBinding( UniqueIndexes.class.getName() ).getTable();
			assertThat( table.getUniqueKeys() ).containsKey( "entity_unique" );
			assertThat( table.getIndexes() ).isEmpty();
			final var collection = (org.hibernate.mapping.PhysicalTable) metadata.getCollectionBinding( UniqueIndexes.class.getName() + ".values" ).getCollectionTable();
			assertThat( collection.getIndexes() ).isEmpty();
			assertThat( collection.getUniqueKeys() ).containsKey( "collection_unique" );
		} );
	}

	@Test
	void expressionCommaRemainsInsideOneTerm() {
		inspect( Expression.class, new PhysicalNamingStrategyStandardImpl(), metadata -> {
			final var table = (org.hibernate.mapping.PhysicalTable) metadata.getEntityBinding( Expression.class.getName() ).getTable();
			final var terms = table.getIndexes().get( "expression_index" ).getSelectables();
			assertThat( terms ).hasSize( 1 );
			assertThat( terms.get( 0 ).isFormula() ).isTrue();
			assertThat( terms.get( 0 ).getText() ).isEqualTo( "(coalesce(first, second))" );
		} );
	}

	@Test
	void collectionLogicalMatchWinsOverDifferentPhysicalMatch() {
		inspect( AmbiguousCollection.class, new Prefix(), metadata -> {
			final var table = (org.hibernate.mapping.PhysicalTable) metadata.getCollectionBinding( AmbiguousCollection.class.getName() + ".values" ).getCollectionTable();
			final var column = (org.hibernate.mapping.Column) table.getIndexes().get( "ambiguous_index" ).getSelectables().get( 0 );
			assertThat( column.getName() ).isEqualTo( "p_p_code" );
			assertThat( column ).isSameAs( table.getColumn( column ) );
			assertThat( table.getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.contains( "p_code", "p_p_code" );
		} );
	}

	private void inspect(Class<?> type, PhysicalNamingStrategyStandardImpl strategy, Consumer<Metadata> action) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			action.accept( MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( type ), new StandardImplicitNamingStrategy(), strategy ) );
		}
	}

	static class Prefix extends PhysicalNamingStrategyStandardImpl {
		final List<String> columns = new ArrayList<>();
		@Override @Nonnull
		public PhysicalName toPhysicalColumnName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			columns.add( name.getText() );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
	}

	@Entity @Table(name = "index_baseline", indexes = @Index(columnList = "code"))
	static class Baseline { @Id long id; String code; }

	@Entity @Table(indexes = @Index(name = "missing_idx", columnList = "missing"))
	static class Missing { @Id long id; String code; }

	@Entity
	static class OrderedCollection {
		@Id long id;
		@ElementCollection
		@CollectionTable(indexes = @Index(name = "ordered_index", columnList = "entry desc"))
		@Column(name = "entry") Set<String> values;
	}

	@Entity @Table(indexes = { @Index(columnList = "first,second"), @Index(columnList = "second,first") })
	static class OppositeOrders { @Id long id; String first; String second; }

	@Entity @Table(indexes = {
			@Index(name = "same_index", columnList = "first"), @Index(name = "same_index", columnList = "second") })
	static class RepeatedName { @Id long id; String first; String second; }

	@Entity @Table(indexes = @Index(name = "entity_unique", columnList = "code", unique = true))
	static class UniqueIndexes {
		@Id long id;
		String code;
		@ElementCollection
		@CollectionTable(indexes = @Index(name = "collection_unique", columnList = "entry", unique = true))
		@Column(name = "entry") Set<String> values;
	}

	@Entity @Table(indexes = @Index(name = "expression_index", columnList = "(coalesce(first, second))"))
	static class Expression { @Id long id; String first; String second; }

	@Entity
	static class AmbiguousCollection {
		@Id long id;
		@ElementCollection
		@CollectionTable(joinColumns = @JoinColumn(name = "code"),
				indexes = @Index(name = "ambiguous_index", columnList = "p_code"))
		@Column(name = "p_code") Set<String> values;
	}
}
