/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.constraint;

import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ManyToManyCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Builds a complete ConstraintModel containing both foreign keys and unique constraints.
 *
 * @author Steve Ebersole
 */
public final class ConstraintModelBuilder {
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

	public static ConstraintModel buildConstraintModel(MappingMetamodelImplementor mappingModel, PlanningOptions planningOptions) {
		return new ConstraintModelBuilder( planningOptions ).build( mappingModel );
	}

	private final PlanningOptions planningOptions;

	public ConstraintModelBuilder(PlanningOptions planningOptions) {
		this.planningOptions = planningOptions;
	}

	public ConstraintModel build(MappingMetamodelImplementor mappingModel) {
		// NOTE: for the time being, we always treat foreign-keys as non-deferrable
		// because atm we just do not know.

		final var foreignKeys = new ArrayList<ForeignKey>();
		final var uniqueConstraints = new ArrayList<UniqueConstraint>();
		final var seen = new IdentityHashMap<ForeignKeyDescriptor, Boolean>();
		final var entityPersisters = new HashMap<String, EntityPersister>();

		// First pass: collect entity persisters for later reference
		mappingModel.forEachEntityDescriptor( (descriptor) -> {
			entityPersisters.put(descriptor.getEntityName(), descriptor);
		} );

		// Second pass: collect foreign keys and unique constraints
		mappingModel.forEachEntityDescriptor( (descriptor) -> {
			collectFromEntityPersister(descriptor, foreignKeys, uniqueConstraints, seen, entityPersisters);
		} );

		// Also scan collection persisters because join tables often show up there
		mappingModel.forEachCollectionDescriptor( (descriptor) -> {
			collectFromCollectionPersister(descriptor, foreignKeys, seen, entityPersisters);
		} );

		// Index unique constraints by table for fast lookup
		Map<String, List<UniqueConstraint>> uniqueConstraintsByTable = uniqueConstraints.stream()
				.collect(Collectors.groupingBy(UniqueConstraint::tableName));

		// Index foreign keys by target table (inbound FKs - pointing TO this table)
		Map<String, List<ForeignKey>> inboundForeignKeysByTable = foreignKeys.stream()
				.collect(Collectors.groupingBy(ForeignKey::targetTable));

		// Index foreign keys by key table (outbound FKs - FROM this table)
		Map<String, List<ForeignKey>> outboundForeignKeysByTable = foreignKeys.stream()
				.collect(Collectors.groupingBy(ForeignKey::keyTable));

		// Identify tables with cyclic foreign key relationships (bidirectional FKs)
		var tablesWithCyclicForeignKeys = identifyTablesWithCyclicForeignKeys(
				inboundForeignKeysByTable,
				outboundForeignKeysByTable
		);

		// Identify tables with self-referential associations
		var selfReferentialTables = identifySelfReferentialTables( foreignKeys );

		return new ConstraintModel(
				foreignKeys,
				uniqueConstraints,
				uniqueConstraintsByTable,
				inboundForeignKeysByTable,
				outboundForeignKeysByTable,
				tablesWithCyclicForeignKeys,
				selfReferentialTables
		);
	}

	private void collectFromEntityPersister(
			EntityPersister descriptor,
			List<ForeignKey> foreignKeys,
			List<UniqueConstraint> uniqueConstraints,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen,
			Map<String, EntityPersister> entityPersisters) {

		// Collect unique constraint for primary key
		//		NOTE: may produce foreign-keys for composite primary-keys
		collectPrimaryKeyConstraint(descriptor, foreignKeys, uniqueConstraints, seen, entityPersisters);

		// Collect @NaturalId constraint if present
		collectNaturalIdConstraint(descriptor, uniqueConstraints);

		// Collect @Column(unique=true) constraints
		collectUniqueColumnConstraints(descriptor, uniqueConstraints);

		collectEntityTableGroupConstraints(descriptor, foreignKeys, seen, entityPersisters);

		// next look through attributes for to-associations with join tables
		handleToOneAttributes( descriptor, foreignKeys, uniqueConstraints, seen, entityPersisters );
	}

	private void collectPrimaryKeyConstraint(
			EntityPersister descriptor,
			List<ForeignKey> foreignKeys,
			List<UniqueConstraint> uniqueConstraints,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen,
			Map<String, EntityPersister> entityPersisters) {
		// Add primary key as a unique constraint
		String tableName = descriptor.getTableName();
		EntityIdentifierMapping identifierMapping = descriptor.getIdentifierMapping();

		if (identifierMapping != null && planningOptions.orderByUniqueKeySlots() ) {
			uniqueConstraints.add(new UniqueConstraint(
					tableName,
					"PRIMARY",
					UniqueConstraint.ConstraintType.PRIMARY_KEY,
					identifierMapping,
					false,  // Primary keys are never deferrable
					false, // Primary keys are never nullable
					null    // PK doesn't need property names (uses special extraction)
			));
		}

		// Check if identifier is an embeddable with @ManyToOne attributes (composite key scenario)
		if (identifierMapping instanceof EmbeddableValuedModelPart embeddableId) {
			handleToOneAttributes( embeddableId.getEmbeddableTypeDescriptor(), foreignKeys, uniqueConstraints, seen, entityPersisters );
		}
	}

	/**
	 * Collect @NaturalId constraint if the entity has one.
	 * Phase 4: Support for @NaturalId unique constraints.
	 */
	private void collectNaturalIdConstraint(
			EntityPersister descriptor,
			List<UniqueConstraint> uniqueConstraints) {
		if ( !planningOptions.orderByUniqueKeySlots() ) {
			return;
		}

		var naturalIdMapping = descriptor.getNaturalIdMapping();
		if (naturalIdMapping == null) {
			return;
		}

		String tableName = descriptor.getTableName();
		var naturalIdAttributes = naturalIdMapping.getNaturalIdAttributes();

		// Extract property names from natural ID attributes
		String[] propertyNames = new String[naturalIdAttributes.size()];
		for (int i = 0; i < naturalIdAttributes.size(); i++) {
			propertyNames[i] = naturalIdAttributes.get(i).getAttributeName();
		}

		// Combine SelectableMappings from all natural ID attributes
		SelectableMappings columns = combineSelectableMappings(naturalIdAttributes);

		uniqueConstraints.add(new UniqueConstraint(
				tableName,
				naturalIdMapping.getNavigableRole().getFullPath(),
				UniqueConstraint.ConstraintType.UNIQUE_KEY,
				columns,
				false,  // Natural IDs are typically not deferrable
				areColumnsNullable( columns ),
				propertyNames
		));
	}

	/**
	 * Combine multiple SelectableMappings from natural ID attributes into a single SelectableMappings.
	 */
	private SelectableMappings combineSelectableMappings(List<SingularAttributeMapping> attributes) {
		List<SelectableMapping> allSelectables = new ArrayList<>();

		for (SingularAttributeMapping attribute : attributes) {
			// Each attribute is a SelectableMappings
			SelectableMappings attrMappings = attribute;
			for (int i = 0; i < attrMappings.getJdbcTypeCount(); i++) {
				allSelectables.add(attrMappings.getSelectable(i));
			}
		}

		// Create a composite SelectableMappings
		return new CompositeSelectableMappings(allSelectables);
	}

	/**
	 * Simple SelectableMappings implementation that combines multiple SelectableMapping objects.
	 */
	private static class CompositeSelectableMappings implements SelectableMappings {
		private final List<SelectableMapping> selectables;

		CompositeSelectableMappings(List<SelectableMapping> selectables) {
			this.selectables = selectables;
		}

		@Override
		public int getJdbcTypeCount() {
			return selectables.size();
		}

		@Override
		public SelectableMapping getSelectable(int columnIndex) {
			return selectables.get(columnIndex);
		}

		@Override
		public int forEachSelectable(int offset, SelectableConsumer consumer) {
			for (int i = 0; i < selectables.size(); i++) {
				consumer.accept(offset + i, selectables.get(i));
			}
			return selectables.size();
		}
	}

	/**
	 * Collect @Column(unique=true) constraints.
	 * Phase 4: Support for single-column unique constraints.
	 */
	private void collectUniqueColumnConstraints(
			EntityPersister descriptor,
			List<UniqueConstraint> uniqueConstraints) {
		String tableName = descriptor.getTableName();
		String[] propertyNames = descriptor.getPropertyNames();

		for (int i = 0; i < propertyNames.length; i++) {
			String propertyName = propertyNames[i];

			// Check if property has unique constraint
			// Unfortunately, EntityPersister doesn't directly expose unique column metadata
			// We would need to check the property's column metadata
			// For now, this is a simplified implementation
			// Full implementation would inspect the property metadata

			// TODO: Access property metadata to check for @Column(unique=true)
			// This requires deeper inspection of the EntityPersister's property mappings
		}
	}

	private void collectFromCollectionPersister(
			CollectionPersister descriptor,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen,
			Map<String, EntityPersister> entityPersisters) {
		var attributeMapping = descriptor.getAttributeMapping();

		// key FK (collection table -> owner)
		final ForeignKeyDescriptor keyFk = attributeMapping.getKeyDescriptor();
		final boolean keyNullable = areColumnsNullable(keyFk.getKeyPart());
		addIfConstraint(keyFk, keyFk.getKeyPart(), keyFk.getTargetPart(), keyNullable, foreignKeys, seen, entityPersisters);

		// many-to-many join table has element FK too (collection table -> child)
		if (attributeMapping.getElementDescriptor() instanceof ManyToManyCollectionPart m2m) {
			final ForeignKeyDescriptor elementFk = m2m.getForeignKeyDescriptor();
			final boolean inverseKeyNullable = areColumnsNullable(elementFk.getKeyPart());
			addIfConstraint(elementFk, elementFk.getKeyPart(), elementFk.getTargetPart(), inverseKeyNullable, foreignKeys, seen, entityPersisters);
		}
	}

	private boolean areColumnsNullable(SelectableMappings columns) {
		// nullable if ALL columns are nullable
		for ( int i = 0; i < columns.getJdbcTypeCount(); i++ ) {
			if ( !columns.getSelectable( i ).isNullable() ) {
				return false;
			}
		}
		return true;
	}

	/// Collect foreign-keys between tables "within" the entity mapping.
	/// These include
	///  - secondary tables
	///  - joined inheritance tables
	private void collectEntityTableGroupConstraints(
			EntityPersister descriptor,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor,Boolean> seen,
			Map<String, EntityPersister> entityPersisters) {

		// order the entity tables for processing
		//	- primarily identifier-table first (only one per group)
		//	- secondarily by relative position
		List<EntityTableMapping> ordered = Stream.of(descriptor.getTableMappings())
				.sorted( ENTITY_TABLE_MAPPING_COMPARATOR )
				.toList();
		if ( ordered.size() > 1 ) {
			final EntityTableMapping identifierTableMapping = ordered.get( 0 );

			// Determine the correct target for each non-identifier table:
			// - Secondary tables (@SecondaryTable) -> always reference identifier table
			// - Joined inheritance tables -> reference previous table in the chain
			for ( int x = 1; x < ordered.size(); x++ ) {
				final EntityTableMapping keyTable = ordered.get( x );
				final EntityTableMapping targetTable;

				if ( keyTable.isSecondaryTable() ) {
					// Secondary table: always reference the identifier table
					targetTable = identifierTableMapping;
				}
				else {
					// Joined inheritance: reference the previous table in the chain
					targetTable = ordered.get( x - 1 );
				}

				addEntityTableGroupConstraint(
						descriptor,
						keyTable,
						targetTable,
						foreignKeys,
						seen,
						entityPersisters
				);
			}
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
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen,
			Map<String, EntityPersister> entityPersisters) {

		// Determine target type - these always target the primary key
		ForeignKey.TargetType targetType = ForeignKey.TargetType.PRIMARY_KEY;

		// Always add the FK - intra-entity table constraints are critical for correct DELETE ordering
		foreignKeys.add(new ForeignKey(
				source.getTableName(),
				target.getTableName(),
				source.getKeyDetails(),
				target.getKeyDetails(),
				targetType,
				false,  // isAssociation
				false,  // nullable
				false   // deferrable
		));
	}

	private void handleToOneAttributes(
			ManagedMappingType declaringType,
			List<ForeignKey> foreignKeys,
			List<UniqueConstraint> uniqueConstraints,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen,
			Map<String, EntityPersister> entityPersisters) {
		declaringType.forEachAttributeMapping( (attributeMapping) -> {
			// To-one
			if (attributeMapping instanceof ToOneAttributeMapping toOne) {
				collectFromToOne( toOne, foreignKeys, uniqueConstraints, seen, entityPersisters );
			}
			// Embedded / component: recurse into its attributes
			if (attributeMapping instanceof EmbeddableValuedModelPart emb) {
				handleToOneAttributes( emb.getEmbeddableTypeDescriptor(),  foreignKeys, uniqueConstraints, seen, entityPersisters );
			}
		} );
	}

	private void collectFromToOne(
			ToOneAttributeMapping toOne,
			List<ForeignKey> foreignKeys,
			List<UniqueConstraint> uniqueConstraints,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen,
			Map<String, EntityPersister> entityPersisters) {
		final ForeignKeyDescriptor fk = toOne.getForeignKeyDescriptor();

		// Check if this is a one-to-one (unique FK) based on cardinality
		boolean isOneToOne = toOne.getCardinality() == ToOneAttributeMapping.Cardinality.ONE_TO_ONE
				|| toOne.getCardinality() == ToOneAttributeMapping.Cardinality.LOGICAL_ONE_TO_ONE;

		if ( isOneToOne
				&& fk != null
				&& fk.hasConstraint()
				&& planningOptions.orderByUniqueKeySlots() ) {
			// Add unique constraint for the FK column
			uniqueConstraints.add(new UniqueConstraint(
					fk.getKeyTable(),
					toOne.getNavigableRole().getFullPath(),
					UniqueConstraint.ConstraintType.UNIQUE_FOREIGN_KEY,
					fk.getKeyPart(),
					false,  // FK unique constraints are typically not deferrable
					areColumnsNullable( fk.getKeyPart() ),
					new String[] { toOne.getAttributeName() }  // Property name for value extraction
			));
		}

		addIfConstraint(fk, fk.getKeyPart(), fk.getTargetPart(), toOne.isNullable(), foreignKeys, seen, entityPersisters);
	}

	private void addIfConstraint(
			ForeignKeyDescriptor fk,
			SelectableMappings keySide,
			SelectableMappings targetSide,
			boolean nullable,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen,
			Map<String, EntityPersister> entityPersisters) {
		if ( fk == null ) {
			return;
		}
		if ( !fk.hasConstraint() ) {
			return;
		}
		addConstraint( fk, keySide, targetSide, nullable, foreignKeys, seen, entityPersisters );
	}

	private void addConstraint(
			ForeignKeyDescriptor fkDesc,
			SelectableMappings keySide,
			SelectableMappings targetSide,
			boolean nullable,
			List<ForeignKey> foreignKeys,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen,
			Map<String, EntityPersister> entityPersisters) {
		List<String> keyColumns = arrayList( fkDesc.getAssociationKey().columns().size() );
		List<String> targetColumns = arrayList( fkDesc.getAssociationKey().columns().size() );

		fkDesc.getKeyPart().forEachSelectable( (index, selectable) -> {
			if ( !selectable.isFormula() ) {
				keyColumns.add( selectable.getSelectableName() );
				targetColumns.add( fkDesc.getTargetPart().getSelectable( index ).getSelectableName() );
			}
		} );

		// Determine target type
		ForeignKey.TargetType targetType = determineTargetType(fkDesc, targetSide, entityPersisters);

		final boolean deferrable = false;

		foreignKeys.add(new ForeignKey(
				fkDesc.getKeyTable(),
				fkDesc.getTargetTable(),
				keySide,
				targetSide,
				targetType,
				true,
				nullable,
				deferrable
		));
	}

	/**
	 * Determine what the foreign key targets (primary key, unique key, or non-unique)
	 */
	private ForeignKey.TargetType determineTargetType(
			ForeignKeyDescriptor fkDesc,
			SelectableMappings targetSide,
			Map<String, EntityPersister> entityPersisters) {

		// Try to find the target entity persister
		String targetTable = fkDesc.getTargetTable();

		// Check each entity persister to see if it owns this table and if the FK targets its PK
		for (EntityPersister persister : entityPersisters.values()) {
			if (persister.getTableName().equals(targetTable)) {
				// Check if targeting primary key
				EntityIdentifierMapping idMapping = persister.getIdentifierMapping();
				if (idMapping != null && columnsMatch(targetSide, idMapping)) {
					return ForeignKey.TargetType.PRIMARY_KEY;
				}

				// TODO: Check for unique keys (natural IDs, @Column(unique=true), etc.)
				// For now, we'll conservatively mark everything else as NON_UNIQUE
				// This can be enhanced later to detect unique constraints
			}
		}

		// Default to NON_UNIQUE if we can't determine
		return ForeignKey.TargetType.NON_UNIQUE;
	}

	/**
	 * Check if two SelectableMappings refer to the same columns
	 */
	private boolean columnsMatch(SelectableMappings columns1, SelectableMappings columns2) {
		if (columns1.getJdbcTypeCount() != columns2.getJdbcTypeCount()) {
			return false;
		}

		for (int i = 0; i < columns1.getJdbcTypeCount(); i++) {
			String name1 = columns1.getSelectable(i).getSelectableName();
			String name2 = columns2.getSelectable(i).getSelectableName();
			if (!name1.equals(name2)) {
				return false;
			}
		}

		return true;
	}


	/// Identifies tables that have self-referential associations (FK from table to itself).
	/// These tables need special grouping treatment to avoid creating false cycles in the graph.
	private java.util.Set<String> identifySelfReferentialTables(ArrayList<ForeignKey> foreignKeys) {
		final java.util.Set<String> tables = new java.util.HashSet<>();
		for (var fk : foreignKeys) {
			if (fk.isAssociation()) {
				String keyTable = (fk.keyTable());
				String targetTable = (fk.targetTable());
				if (keyTable.equals(targetTable)) {
					tables.add(keyTable);
				}
			}
		}
		return tables;
	}

	/// Identifies tables that participate in cyclic foreign key relationships.
	/// A table has cyclic FKs if it has bidirectional FK relationships - i.e., it has
	/// an FK pointing to another table AND that table has an FK pointing back to it.
	///
	/// Example: Person table has FK to Address AND Address table has FK to Person.
	/// When deleting entities from different cascade paths, batching their DELETEs
	/// by shape alone can create false cycles.
	private java.util.Set<String> identifyTablesWithCyclicForeignKeys(
			Map<String, List<ForeignKey>> inboundForeignKeysByTable,
			Map<String, List<ForeignKey>> outboundForeignKeysByTable) {
		final java.util.Set<String> tables = new java.util.HashSet<>();

		// For each table, check if it has bidirectional FK relationships
		for (String table : inboundForeignKeysByTable.keySet()) {
			List<ForeignKey> inboundFKs = inboundForeignKeysByTable.getOrDefault(table, List.of());
			List<ForeignKey> outboundFKs = outboundForeignKeysByTable.getOrDefault(table, List.of());

			// Check for bidirectional relationships
			for (ForeignKey inbound : inboundFKs) {
				String sourceTable = inbound.keyTable();
				for (ForeignKey outbound : outboundFKs) {
					if (outbound.targetTable().equals(sourceTable)) {
						// Found bidirectional FK: table <-> sourceTable
						tables.add(table);
						break;
					}
				}
				if (tables.contains(table)) {
					break;
				}
			}
		}

		return tables;
	}
}
