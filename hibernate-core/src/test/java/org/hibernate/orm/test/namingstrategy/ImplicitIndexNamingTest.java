/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.materialize.IndexColumnList;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.IndexNamingInput;
import org.hibernate.boot.model.naming.spi.IndexTermNamingInput;
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

/// Index strategy inputs, validation, quoting, export, and restoration.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitIndexNamingTest {
	@Test
	void typedInputsAndFinalNamesSurviveExportAndRestoration() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( org.hibernate.cfg.MappingSettings.METADATA_SERIALIZATION_ENABLED, true ).build()) {
			final var inputs = new ArrayList<IndexNamingInput>();
			final var physicalCalls = new ArrayList<LogicalName>();
			final var implicit = capturing( inputs );
			final var physical = new IndexColumnResolutionTest.Prefix() {
				@Override @Nonnull
				public PhysicalName toPhysicalIndexName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
					physicalCalls.add( name );
					return context.getPhysicalNameFactory().create( "ix_" + name.getText(), false );
				}
			};
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Mixed.class ), implicit, physical );
			assertThat( inputs ).hasSize( 1 );
			final var input = inputs.get( 0 );
			assertThat( input.table().logicalName().getText() ).isEqualTo( "index_pairs" );
			assertThat( input.type() ).isEqualTo( "hash" );
			assertThat( input.using() ).isEqualTo( "btree" );
			assertThat( input.unique() ).isFalse();
			assertThat( input.terms() ).extracting( IndexTermNamingInput::sourceText )
					.containsExactly( "second", "`First`", "(lower(p_third))" );
			assertThat( input.terms() ).extracting( IndexTermNamingInput::order ).containsExactly(
					IndexTermNamingInput.Order.DESC, IndexTermNamingInput.Order.ASC, IndexTermNamingInput.Order.UNSPECIFIED );
			final var column = (IndexTermNamingInput.ColumnTerm) input.terms().get( 1 );
			assertThat( column.names().logicalName().isQuoted() ).isTrue();
			assertThat( column.names().physicalName().getText() ).isEqualTo( "p_First" );
			assertThat( input.terms().get( 2 ) ).isInstanceOf( IndexTermNamingInput.ExpressionTerm.class );
			assertThatThrownBy( () -> input.terms().clear() ).isInstanceOf( UnsupportedOperationException.class );
			assertThat( physical.columns.stream().filter( "second"::equals ).count() ).isEqualTo( 1 );
			assertThat( physicalCalls ).hasSize( 2 );
			assertThat( physicalCalls ).anyMatch( LogicalName::isExplicit );
			final var table = (org.hibernate.mapping.PhysicalTable) metadata.getEntityBinding( Mixed.class.getName() ).getTable();
			final var index = table.getIndexes().get( "\"ix_generated\"" );
			assertThat( index ).isNotNull();
			final var context = org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl.forTests(
					metadata.getDatabase().getJdbcEnvironment() );
			final var exporter = metadata.getDatabase().getDialect().getIndexExporter();
			final var ddl = exporter.getSqlCreateStrings( index, metadata, context );
			assertThat( String.join( " ", ddl ) ).contains( "\"ix_generated\"", "p_second desc", "\"p_First\" asc",
					"(lower(p_third))", "using btree", "include (p_id)" );
			assertThat( exporter.getSqlCreateStrings( index, metadata, context ) ).containsExactly( ddl );
			final var bytes = new java.io.ByteArrayOutputStream();
			org.hibernate.boot.serial.MetadataSerialization.serialize( (org.hibernate.boot.spi.MetadataImplementor) metadata ).writeTo( bytes );
			final var restored = org.hibernate.boot.serial.MetadataSerialization.read(
					new java.io.ByteArrayInputStream( bytes.toByteArray() ) ).restore( registry ).getMetadata();
			final var restoredTable = (org.hibernate.mapping.PhysicalTable) restored.getEntityBinding( Mixed.class.getName() ).getTable();
			assertThat( restoredTable.getIndexes().keySet() ).containsExactlyInAnyOrderElementsOf( table.getIndexes().keySet() );
			assertThat( exporter.getSqlCreateStrings( restoredTable.getIndexes().get( "\"ix_generated\"" ), restored, context ) )
					.containsExactly( ddl );
			assertThat( inputs ).hasSize( 1 );
			assertThat( physicalCalls ).hasSize( 2 );
		}
	}

	@Test
	void physicalFallbackRetainsSourceSpellingAndBothResolvedNames() {
		final var inputs = new ArrayList<IndexNamingInput>();
		build( PhysicalReference.class, capturing( inputs ), new IndexColumnResolutionTest.Prefix() );
		final var column = (IndexTermNamingInput.ColumnTerm) inputs.get( 0 ).terms().get( 0 );
		assertThat( column.sourceText() ).isEqualTo( "p_code" );
		assertThat( column.names().logicalName().getText() ).isEqualTo( "code" );
		assertThat( column.names().physicalName().getText() ).isEqualTo( "p_code" );
	}

	@Test
	void secondaryTableUsesItsActualColumn() {
		final var inputs = new ArrayList<IndexNamingInput>();
		build( Secondary.class, capturing( inputs ), new IndexColumnResolutionTest.Prefix() );
		assertThat( inputs.get( 0 ).table().logicalName().getText() ).isEqualTo( "index_details" );
		assertThat( ((IndexTermNamingInput.ColumnTerm) inputs.get( 0 ).terms().get( 0 )).names().physicalName().getText() )
				.isEqualTo( "p_code" );
	}

	@Test
	void nullImplicitResultIsRejected() {
		assertThatThrownBy( () -> build( PhysicalReference.class, new StandardImplicitNamingStrategy() {
			@Override @Nonnull
			public LogicalName determineIndexName(@Nonnull IndexNamingInput input, @Nonnull ImplicitNamingContext context) { return null; }
		}, new IndexColumnResolutionTest.Prefix() ) ).isInstanceOf( MappingException.class ).hasMessageContaining( "null" );
	}

	@Test
	void nullPhysicalResultIsRejected() {
		assertThatThrownBy( () -> build( DifferentNames.class, new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() {
			@Override @Nonnull
			public PhysicalName toPhysicalIndexName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) { return null; }
		} ) ).isInstanceOf( MappingException.class ).hasMessageContaining( "null" );
	}

	@Test
	void physicalCollisionsDoNotMergeDifferentDefinitions() {
		assertThatThrownBy( () -> build( DifferentNames.class, new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() {
			@Override @Nonnull
			public PhysicalName toPhysicalIndexName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
				return context.getPhysicalNameFactory().create( "collision", false );
			}
		} ) ).isInstanceOf( MappingException.class ).hasMessageContaining( "Index naming collision" );
	}

	@Test
	void duplicateExplicitUniqueIndexesFailBeforeRepresentationSelection() {
		assertThatThrownBy( () -> build( DuplicateUnique.class, new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() ) )
				.isInstanceOf( org.hibernate.AnnotationException.class ).hasMessageContaining( "Duplicate explicit @Index name 'duplicate'" )
				.hasMessageContaining( "indexes[0]" ).hasMessageContaining( "indexes[1]" );
	}

	@Test
	void oppositeUniqueIndexOrdersDoNotSilentlyMergeAsUniqueKeys() {
		assertThatThrownBy( () -> build( OppositeUnique.class, new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "collision" );
	}

	@Test
	void typeDistinguishesOtherwiseIdenticalDefinitions() {
		assertThatThrownBy( () -> build( DifferentTypes.class, new StandardImplicitNamingStrategy(), new PhysicalNamingStrategyStandardImpl() ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "Index naming collision" );
	}

	@Test
	void parserPreservesQuotedCommasAndNestedExpressions() {
		final var terms = IndexColumnList.parse( "`a,b` desc, (coalesce(lower(code), 'x,''y')) ASC", "Entity @Table.indexes[0]" );
		assertThat( terms ).extracting( IndexColumnList.Term::text )
				.containsExactly( "`a,b`", "(coalesce(lower(code), 'x,''y'))" );
		assertThat( terms ).extracting( IndexColumnList.Term::order )
				.containsExactly( IndexTermNamingInput.Order.DESC, IndexTermNamingInput.Order.ASC );
		for ( var malformed : List.of( "first,,second", "(lower(code)", "`unclosed", "code,", ")code(" ) ) {
			assertThatThrownBy( () -> IndexColumnList.parse( malformed, "Entity @Table.indexes[0]" ) )
					.isInstanceOf( org.hibernate.AnnotationException.class ).hasMessageContaining( "Entity @Table.indexes[0]" );
		}
	}

	private StandardImplicitNamingStrategy capturing(List<IndexNamingInput> inputs) {
		return new StandardImplicitNamingStrategy() {
			@Override @Nonnull
			public LogicalName determineIndexName(@Nonnull IndexNamingInput input, @Nonnull ImplicitNamingContext context) {
				inputs.add( input );
				return context.implicitName( "generated", true );
			}
		};
	}

	private void build(Class<?> type, StandardImplicitNamingStrategy implicit, PhysicalNamingStrategyStandardImpl physical) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry, new MappingSources().addManagedClass( type ), implicit, physical );
		}
	}

	@Entity @Table(name = "index_pairs", indexes = {
			@Index(columnList = "second desc, `First` asc, (lower(p_third))", type = "hash", using = "btree", options = "include (p_id)"),
			@Index(name = "declared", columnList = "third") })
	static class Mixed { @Id long id; @Column(name = "`First`") String first; String second; String third; }

	@Entity @Table(indexes = @Index(columnList = "p_code"))
	static class PhysicalReference { @Id long id; String code; }

	@Entity @SecondaryTable(name = "index_details", indexes = @Index(columnList = "code"))
	static class Secondary { @Id long id; @Column(table = "index_details") String code; }

	@Entity @Table(indexes = {@Index(name = "one", columnList = "first"), @Index(name = "two", columnList = "second")})
	static class DifferentNames { @Id long id; String first; String second; }

	@Entity @Table(indexes = {@Index(name = "duplicate", columnList = "code", unique = true),
			@Index(name = "duplicate", columnList = "code", unique = true)})
	static class DuplicateUnique { @Id long id; String code; }

	@Entity @Table(indexes = {@Index(columnList = "first,second", unique = true), @Index(columnList = "second,first", unique = true)})
	static class OppositeUnique { @Id long id; String first; String second; }

	@Entity @Table(indexes = {@Index(columnList = "code", type = "hash"), @Index(columnList = "code", type = "btree")})
	static class DifferentTypes { @Id long id; String code; }
}
