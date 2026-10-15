package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.hibernate.AnnotationException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SortableValue;

/// Resolves source-column write ownership independently of formula association binding.
///
/// @author Steve Ebersole
final class FormulaOneToOneHelper {
	private FormulaOneToOneHelper() {
	}

	static void inferReadOnlyColumns(
			AnnotatedJoinColumns joinColumns,
			PersistentClass owner,
			PersistentClass target,
			String path,
			MetadataBuildingContext context) {
		resolveImplicitNames( joinColumns, target, path, context );
		var shared = new ArrayList<AnnotatedJoinColumn>();
		for ( var column : joinColumns.getJoinColumns() ) {
			if ( !column.isFormula() && isIdentifierColumn( column, owner ) ) {
				column.setInsertable( false );
				column.setUpdatable( false );
				shared.add( column );
			}
		}
		if ( !shared.isEmpty() ) {
			validateEffectiveFlags( joinColumns, path, shared.stream()
					.map( column -> column.getMappingColumn().getQuotedName() )
					.collect( Collectors.joining( ", " ) ) );
		}
	}

	private static boolean isIdentifierColumn(AnnotatedJoinColumn column, PersistentClass owner) {
		var explicitTable = Identifier.toIdentifier( column.getExplicitTableName() );
		for ( var entity = owner; entity != null; entity = entity.getSuperclass() ) {
			boolean sameTable = explicitTable == null
					? entity.getTable().equals( owner.getTable() )
					: explicitTable.equals( entity.getTable().getNameIdentifier() );
			if ( sameTable && entity.getKey() != null
					&& entity.getKey().getColumns().contains( column.getMappingColumn() ) ) {
				return true;
			}
		}
		return false;
	}

	private static void resolveImplicitNames(
			AnnotatedJoinColumns columns,
			PersistentClass target,
			String path,
			MetadataBuildingContext context) {
		if ( target == null ) {
			return; // The normal association binder diagnoses an unknown target.
		}
		if ( target.getIdentifier() instanceof SortableValue identifier ) {
			identifier.sortProperties();
		}
		var targetColumns = target.getIdentifier().getColumns();
		var collector = context.getMetadataCollector();
		for ( int i = 0; i < columns.getJoinColumns().size(); i++ ) {
			var column = columns.getJoinColumns().get( i );
			if ( !column.isFormula() && column.isNameDeferred() ) {
				final Column referencedColumn;
				if ( column.isReferenceImplicit() ) {
					if ( i >= targetColumns.size() ) {
						throw new AnnotationException( "Association '" + path
								+ "' has more join selectables than target identifier columns" );
					}
					referencedColumn = targetColumns.get( i );
				}
				else {
					var referencedOwner = BinderHelper.findColumnOwner( target, column.getReferencedColumn(), context );
					if ( referencedOwner == null ) {
						throw new AnnotationException( "Association '" + path + "' references unknown target column '"
								+ column.getReferencedColumn() + "'" );
					}
					referencedColumn = referencedOwner.getTable().getColumn( new Column(
							collector.getPhysicalColumnName( referencedOwner.getTable(), column.getReferencedColumn() ) ) );
				}
				column.resolveDefaultColumnName( i, referencedColumn, target );
			}
		}
	}

	private static void validateEffectiveFlags(AnnotatedJoinColumns columns, String path, String inferred) {
		AnnotatedJoinColumn first = null;
		for ( var column : columns.getJoinColumns() ) {
			if ( !column.isFormula() ) {
				if ( first == null ) {
					first = column;
				}
				else {
					String conflict = first.isInsertable() != column.isInsertable() ? "insertable"
							: first.isUpdatable() != column.isUpdatable() ? "updatable" : null;
					if ( conflict != null ) {
						throw new AnnotationException( "Association '" + path + "' has conflicting effective '" + conflict
								+ "' flags for join columns '" + first.getMappingColumn().getQuotedName() + "' and '"
								+ column.getMappingColumn().getQuotedName() + "'; shared owner primary-key column(s) "
								+ inferred + " were inferred read-only" );
					}
					if ( first.isNullable() != column.isNullable()
							|| !first.getExplicitTableName().equals( column.getExplicitTableName() ) ) {
						throw new AnnotationException( "Association '" + path
								+ "' has inconsistent nullability or table placement for join columns '"
								+ first.getMappingColumn().getQuotedName() + "' and '"
								+ column.getMappingColumn().getQuotedName() + "'" );
					}
				}
			}
		}
	}
}
