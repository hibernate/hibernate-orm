/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.internal.ColumnNameHelper;
import org.hibernate.boot.model.naming.internal.ConstraintNamingHelper;
import org.hibernate.boot.model.naming.internal.ImplicitNamingContextImpl;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.NamingNamePair;
import org.hibernate.boot.model.naming.spi.UniqueKeyNamingInput;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.NamedTable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.relational.naming.spi.LogicalName;

/// Collects unique-key candidates and finalizes resolved, surviving constraints before export.
///
/// @author Steve Ebersole
public final class UniqueKeyMappingMaterializer {
	private UniqueKeyMappingMaterializer() {}

	public static void materializeUniqueKey(ResolvedUniqueKey key) {
		key.metadataBuildingContext().getMetadataCollector().getRelationalModelCorrespondences()
				.uniqueKeyCandidates().add( key );
	}

	/// Complete membership, absorption and naming after all mapping contributions.
	public static void finishColumnUniqueKeys(Iterable<Table> tables, MetadataBuildingContext context) {
		final var names = context.getMetadataCollector().getRelationalModelCorrespondences();
		final Map<Table, List<Candidate>> candidates = new LinkedHashMap<>();
		final Map<Table, Map<LogicalName, String>> declarations = new IdentityHashMap<>();
		for ( var resolved : names.uniqueKeyCandidates() ) {
			// HHH-20918: validate declarations before deduplication, naming, or PK absorption.
			if ( resolved.declarationLocation() != null && resolved.nameExplicit() ) {
				final var logicalName = context.getMetadataCollector().getDatabase().toLogicalName( resolved.name(), true );
				final var previous = declarations.computeIfAbsent( resolved.table(), ignored -> new LinkedHashMap<>() )
						.putIfAbsent( logicalName, resolved.declarationLocation() );
				if ( previous != null ) {
					if ( previous.equals( resolved.declarationLocation() ) ) {
						continue; // The same source declaration was visited more than once.
					}
					throw new org.hibernate.AnnotationException( "Duplicate explicit @UniqueConstraint name '"
							+ resolved.name() + "' on table '" + resolved.table().getName()
							+ "' declared at " + previous + " and " + resolved.declarationLocation() );
				}
			}
			candidates.computeIfAbsent( resolved.table(), ignored -> new ArrayList<>() )
					.add( resolve( resolved ) );
		}
		names.uniqueKeyCandidates().clear();
		for ( var table : tables ) {
			if ( table.areUniqueKeysFinalized() ) { continue; }
			final var keys = candidates.computeIfAbsent( table, ignored -> new ArrayList<>() );
			// Preserve already materialized contributions and their finalized names.
			for ( var key : table.getUniqueKeys().values() ) {
				keys.add( new Candidate( key, null, new ArrayList<>(), true ) );
			}
			for ( var column : table.getColumns() ) {
				if ( column.isUnique() && !table.isPrimaryKey( column ) ) {
					final var key = new UniqueKey( table );
					key.addColumn( column );
					keys.add( new Candidate( key, null, new ArrayList<>(), false ) );
				}
			}
			finish( table, keys, context );
		}
	}

	private static Candidate resolve(ResolvedUniqueKey resolved) {
		final var context = resolved.metadataBuildingContext();
		final var database = context.getMetadataCollector().getDatabase();
		final var names = context.getMetadataCollector().getRelationalModelCorrespondences().columnNames();
		final var table = resolved.table();
		final var key = new UniqueKey( table );
		key.setExplicit( resolved.explicit() );
		key.setNameExplicit( resolved.nameExplicit() );
		key.setOptions( resolved.options() );
		key.setNullsNotDistinct( resolved.nullsNotDistinct() );
		final var pairs = new ArrayList<NamingNamePair>();
		final int size = resolved.columnReferences() == null ? resolved.columns().size() : resolved.columnReferences().size();
		for ( int i = 0; i < size; i++ ) {
			Column column;
			LogicalName logical;
			if ( resolved.columnReferences() != null ) {
				final var reference = resolved.columnReferences().get( i );
				final var requested = database.toLogicalName( reference );
				if ( AttributeColumnReference.isRoleReference( reference ) ) {
					column = new AttributeColumnReference( table, context, resolved.entityName(),
							resolved.sourceRole(), resolved.collectionRole() ).resolveRole( reference );
					logical = names.findDeclarationName( table, column );
				}
				else {
					column = names.findPhysicalColumn( table, requested );
					if ( column != null ) {
						logical = names.selectReferenceName( table, column, requested );
					}
					else {
						column = table.getColumn( ColumnNameHelper.physicalName( reference, database ) );
						logical = column == null ? null : names.findDeclarationName( table, column );
					}
				}
				if ( column == null && resolved.entityName() != null ) {
					column = new AttributeColumnReference( table, context, resolved.entityName(), resolved.sourceRole(), resolved.collectionRole() )
							.resolve( reference );
					logical = column == null ? null : names.findDeclarationName( table, column );
				}
				// HHH-20917: resolve references; never construct or physically rename placeholders.
				if ( column == null ) {
					throw new org.hibernate.AnnotationException( "Unique constraint '" + resolved.name()
							+ "' on table '" + table.getName() + "' references unknown column '" + reference
							+ "' at " + resolved.sourceRole() );
				}
			}
			else {
				final var supplied = resolved.columns().get( i );
				column = table.getColumn( supplied );
				if ( column == null && !resolved.explicit()
						&& supplied.getValue() instanceof org.hibernate.mapping.BasicValue basic
						&& basic.getAggregateColumn() != null ) {
					// Preserve inferred aggregate-member UK behavior until its separate investigation.
					// This is an existing mapped member, never an annotation placeholder.
					column = supplied;
				}
				if ( column == null ) {
					throw new MappingException( "Unresolved unique-key column " + table.getName() + "." + supplied.getName() );
				}
				logical = names.findDeclarationName( table, column );
			}
			if ( logical == null ) {
				throw new MappingException( "Missing logical unique-key column dependency: " + table.getName() + "." + column.getName() );
			}
			key.addColumn( column, resolved.columnOrderings() == null ? null : resolved.columnOrderings().get( i ) );
			pairs.add( new NamingNamePair( logical, column.getPhysicalName() ) );
		}
		if ( !resolved.tableUniqueKey() && key.getColumns().size() == 1 ) {
			key.getColumn( 0 ).setUnique( true );
		}
		for ( var extra : context.getMetadataCollector().getRelationalModelCorrespondences().uniqueKeyAdditionalColumns( table ) ) {
			if ( !key.containsColumn( extra ) ) {
				key.addColumn( extra );
				// Temporal naming dependencies remain unchanged pending the separate compatibility decision.
				// The actual constraint still includes the period-start column.

			}
		}
		final var explicit = resolved.nameExplicit() && resolved.name() != null && !resolved.name().isEmpty()
				? database.toLogicalName( resolved.name(), true ) : null;
		final var candidate = new Candidate( key, explicit, pairs, false );
		candidate.indexSource = resolved.indexSource();
		return candidate;
	}

	private static void finish(Table table, List<Candidate> candidates, MetadataBuildingContext context) {
		// Duplicate @UniqueConstraint declarations were rejected before resolution.
		// Index declarations are validated before being submitted to this lifecycle.
		final var merged = new ArrayList<Candidate>();
		final Map<LogicalName, Candidate> explicitNames = new LinkedHashMap<>();
		for ( var candidate : candidates ) {
			final var previous = candidate.explicitName == null ? null : explicitNames.get( candidate.explicitName );
			if ( previous == null ) {
				merged.add( candidate );
				if ( candidate.explicitName != null ) { explicitNames.put( candidate.explicitName, candidate ); }
			}
			else {
				if ( (previous.indexSource || candidate.indexSource) && !sameDefinition( previous, candidate ) ) {
					throw new MappingException( "Unique index/constraint name collision on table '" + table.getName()
							+ "' for name '" + candidate.explicitName + "'" );
				}
				previous.indexSource |= candidate.indexSource;
				for ( int i = 0; i < candidate.key.getColumns().size(); i++ ) {
					final var column = candidate.key.getColumn( i );
					if ( !previous.key.containsColumn( column ) ) { previous.pairs.add( candidate.pairs.get( i ) ); }
					previous.key.addColumn( column, candidate.key.getColumnOrderMap().get( column ) );
				}
				if ( candidate.key.getOptions() != null && !candidate.key.getOptions().isEmpty() ) {
					previous.key.setOptions( candidate.key.getOptions() );
				}
				previous.key.setNullsNotDistinct( candidate.key.isNullsNotDistinct() );
			}
		}
		final var survivors = new ArrayList<Candidate>();
		for ( int i = 0; i < merged.size(); i++ ) {
			final var candidate = merged.get( i );
			boolean redundant = false;
			if ( !candidate.key.isExplicit() ) {
				for ( int j = 0; j < merged.size(); j++ ) {
					final var other = merged.get( j );
					if ( i != j && sameColumns( candidate.key.getColumns(), other.key.getColumns() )
							&& (other.key.isExplicit() || j > i) ) { redundant = true; break; }
				}
			}
			if ( !redundant ) { survivors.add( candidate ); }
		}
		Candidate absorbed = null;
		final var primaryKey = table.getPrimaryKey();
		if ( primaryKey != null && !primaryKey.getColumns().isEmpty() ) {
			for ( var candidate : survivors ) {
				if ( sameColumns( primaryKey.getColumns(), candidate.key.getColumns() ) ) { absorbed = candidate; }
			}
		}
		if ( absorbed != null ) {
			if ( absorbed.explicitName != null ) { name( absorbed, context ); }
			primaryKey.setOrderingUniqueKey( absorbed.key );
		}
		final Map<String, UniqueKey> finalized = new LinkedHashMap<>();
		final Map<org.hibernate.relational.naming.spi.PhysicalName, Candidate> physicalNames = new LinkedHashMap<>();
		if ( absorbed != null && absorbed.key.isNameExplicit() && absorbed.key.getName() != null ) {
			physicalNames.put( ColumnNameHelper.physicalName( absorbed.key.getName(),
					context.getMetadataCollector().getDatabase() ), absorbed );
		}
		for ( var candidate : survivors ) {
			if ( primaryKey != null && sameColumns( primaryKey.getColumns(), candidate.key.getColumns() ) ) { continue; }
			name( candidate, context );
			final var physicalName = ColumnNameHelper.physicalName( candidate.key.getName(), context.getMetadataCollector().getDatabase() );
			final var previous = physicalNames.putIfAbsent( physicalName, candidate );
			if ( previous != null && !sameDefinition( previous, candidate ) ) {
				throw new MappingException( "Unique-key naming collision on table '" + table.getName()
						+ "' for name '" + candidate.key.getName() + "': " + previous.key.getColumns()
						+ " versus " + candidate.key.getColumns() );
			}
			if ( previous == null ) { finalized.put( candidate.key.getName(), candidate.key ); }
			if ( candidate.key.getColumns().size() == 1 ) {
				final var column = candidate.key.getColumn( 0 );
				if ( column.isUnique() ) { column.setUniqueKeyName( candidate.key.getName() ); }
			}
		}
		table.finalizeUniqueKeys( finalized );
	}

	private static boolean sameDefinition(Candidate first, Candidate second) {
		if ( first.indexSource || second.indexSource ) {
			return first.key.getColumns().equals( second.key.getColumns() )
					&& first.key.getColumnOrderMap().equals( second.key.getColumnOrderMap() )
					&& java.util.Objects.equals( first.key.getOptions(), second.key.getOptions() );
		}
		return sameColumns( first.key.getColumns(), second.key.getColumns() );
	}

	private static boolean sameColumns(List<Column> first, List<Column> second) {
		return first.size() == second.size() && first.containsAll( second );
	}

	private static void name(Candidate candidate, MetadataBuildingContext context) {
		if ( candidate.finalized ) { return; }
		LogicalName logical = candidate.explicitName;
		if ( logical == null ) {
			final var names = context.getMetadataCollector().getRelationalModelCorrespondences();
			final var table = candidate.key.getTable();
			final var tableName = names.tableName( table );
			if ( !(table instanceof NamedTable namedTable) || tableName == null ) {
				throw new MappingException( "Missing named-table dependency for unique key: " + table.getName() );
			}
			if ( candidate.pairs.isEmpty() ) {
				for ( var column : candidate.key.getColumns() ) {
					final var declaration = names.columnNames().findDeclarationName( table, column );
					if ( declaration == null ) { throw new MappingException( "Missing logical unique-key column: " + column.getName() ); }
					candidate.pairs.add( new NamingNamePair( declaration, column.getPhysicalName() ) );
				}
			}
			logical = context.getBuildingPlan().getImplicitNamingStrategy().determineUniqueKeyName(
					new UniqueKeyNamingInput( new NamedTableNamingInput( new NamingNamePair(
							tableName, namedTable.getPhysicalName().objectName() ) ), candidate.pairs ),
					ImplicitNamingContextImpl.from( context ) );
		}
		candidate.key.setName( ConstraintNamingHelper.resolveLogical( logical, ConstraintNamingHelper.Kind.UNIQUE_KEY, context ) );
		candidate.finalized = true;
	}

	private static final class Candidate {
		final UniqueKey key;
		final LogicalName explicitName;
		final List<NamingNamePair> pairs;
		boolean finalized;
		boolean indexSource;
		Candidate(UniqueKey key, LogicalName explicitName, List<NamingNamePair> pairs, boolean finalized) {
			this.key = key;
			this.explicitName = explicitName;
			this.pairs = pairs;
			this.finalized = finalized;
		}
	}
}
