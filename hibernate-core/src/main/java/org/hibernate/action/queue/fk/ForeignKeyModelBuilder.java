/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.fk;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.ManyToManyCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/// Finds all foreign-keys defined in the domain model and creates [ForeignKey] descriptor for each.
///
/// @author Steve Ebersole
public final class ForeignKeyModelBuilder {
	private static final Comparator<EntityTableMapping> ENTITY_TABLE_MAPPING_COMPARATOR
			= buildEntityTableMappingComparator();

	private static Comparator<EntityTableMapping> buildEntityTableMappingComparator() {
		// primarily by the `identifier` flag to put the identifier-holding table first
		final Comparator<EntityTableMapping> primarySort = (t1, t2) -> {
			if ( t1.isIdentifierTable() ) {
				// put t1 first
				return -1;
			}
			else if ( t2.isIdentifierTable() ) {
				// put t2 first
				return 1;
			}
			return 0;
		};
		return primarySort.thenComparing( EntityTableMapping::getRelativePosition );
	}

	public static ForeignKeyModel buildForeignKeyGraphModel(MappingMetamodelImplementor mappingModel) {
		return new ForeignKeyModelBuilder().build( mappingModel );
	}

	public ForeignKeyModel build(MappingMetamodelImplementor mappingModel) {
		// NOTE: for the time being, we always treat foreign-keys as non-deferrable
		// because atm we just do not know.

		final var foreignKeys = new ArrayList<ForeignKey>();
		final var seen = new IdentityHashMap<ForeignKeyDescriptor, Boolean>();

		mappingModel.forEachEntityDescriptor( (descriptor) -> {
			collectFromEntityPersister(descriptor, foreignKeys, seen);
		} );

		// Also scan collection persisters because join tables often show up there
		mappingModel.forEachCollectionDescriptor( (descriptor) -> {
			collectFromCollectionPersister(descriptor, foreignKeys, seen);
		} );

		return new ForeignKeyModel(foreignKeys);
	}

	private void collectFromEntityPersister(
			EntityPersister descriptor,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen) {
		// these are the tables for the entity including
		// 	- secondary tables
		//  - joined inheritance tables
		for ( int i = 0; i < descriptor.getTableMappings().length; i++ ) {
			// order them for processing
			//	- primarily identifier-table first (only one per group)
			//	- secondarily by relative position
			List<EntityTableMapping> ordered = Stream.of(descriptor.getTableMappings())
					.sorted( ENTITY_TABLE_MAPPING_COMPARATOR )
					.toList();
			if ( ordered.size() > 1 ) {
				final EntityTableMapping identifierTableMapping = ordered.get( 0 );
				for ( int x = 1; x < ordered.size(); x++ ) {
					final EntityTableMapping nonIdentifierTableMapping = ordered.get( x );
					addEntityTableGroupConstraint(
							descriptor,
							nonIdentifierTableMapping,
							identifierTableMapping,
							foreignKeys,
							seen
					);
				}
			}
		}

		// next look through attributes for to-associations with join tables
		handleToOneAttributes( descriptor, foreignKeys, seen );
	}

	private void collectFromCollectionPersister(
			CollectionPersister descriptor,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen) {
		var attributeMapping = descriptor.getAttributeMapping();

		// key FK (collection table -> owner)
		final ForeignKeyDescriptor keyFk = attributeMapping.getKeyDescriptor();
		addIfConstraint(keyFk, false, foreignKeys, seen);

		// many-to-many join table has element FK too
		if (attributeMapping.getElementDescriptor() instanceof ManyToManyCollectionPart m2m) {
			final ForeignKeyDescriptor elementFk = m2m.getForeignKeyDescriptor();
			addIfConstraint(elementFk, m2m.isOptional(), foreignKeys, seen);
		}
	}

	/// Create foreignKeys for tables within an entity mapping.
	/// These always refer to the "identifier table" - here `target`.
	///
	/// NOTE: these do not define ForeignKeyDescriptor, so we need to create one
	/// but that is super trivial given the details we do have from EntityTableMapping
	private void addEntityTableGroupConstraint(
			EntityPersister descriptor,
			EntityTableMapping source,
			EntityTableMapping target,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen) {
		foreignKeys.add(new ForeignKey(
				source.getTableName(),
				target.getTableName(),
				source.getKeyDetails().getKeyColumns().stream().map( TableDetails.KeyColumn::getColumnName ).toList(),
				target.getKeyDetails().getKeyColumns().stream().map( TableDetails.KeyColumn::getColumnName ).toList(),
				false,
				false,
				false
		));
	}

	private void handleToOneAttributes(
			ManagedMappingType declaringType,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen) {
		declaringType.forEachAttributeMapping( (attributeMapping) -> {
			// To-one
			if (attributeMapping instanceof ToOneAttributeMapping toOne) {
				collectFromToOne( toOne, foreignKeys, seen );
			}
			// Embedded / component: recurse into its attributes
			if (attributeMapping instanceof EmbeddableValuedModelPart emb) {
				handleToOneAttributes( emb.getEmbeddableTypeDescriptor(),  foreignKeys, seen );
			}
		} );
	}

	private void collectFromToOne(
			ToOneAttributeMapping toOne,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen) {
		final ForeignKeyDescriptor fk = toOne.getForeignKeyDescriptor();
		addIfConstraint(fk, toOne.isNullable(), foreignKeys, seen);
	}

	private void addIfConstraint(
			ForeignKeyDescriptor fk,
			boolean nullable,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen) {
		if (fk == null) return;
		if (!fk.hasConstraint()) return;
		addConstraint( fk, nullable, foreignKeys, seen );
	}

	private void addConstraint(
			ForeignKeyDescriptor fkDesc,
			boolean nullable,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen) {
		List<String> keyColumns = arrayList( fkDesc.getAssociationKey().columns().size() );
		List<String> targetColumns = arrayList( fkDesc.getAssociationKey().columns().size() );

		fkDesc.getKeyPart().forEachSelectable( (index, selectable) -> {
			if ( !selectable.isFormula() ) {
				keyColumns.add( selectable.getSelectableName() );
				targetColumns.add( fkDesc.getTargetPart().getSelectable( index ).getSelectableName() );
			}
		} );

		final boolean deferrable = false;

		foreignKeys.add(new ForeignKey(
				fkDesc.getKeyTable(),
				fkDesc.getTargetTable(),
				keyColumns,
				targetColumns,
				true,
				nullable,
				deferrable
		));
	}
}
