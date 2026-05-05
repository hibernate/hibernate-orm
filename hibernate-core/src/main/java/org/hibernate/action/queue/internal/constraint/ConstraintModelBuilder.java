/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.constraint;

import org.hibernate.action.queue.spi.PlanningOptions;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hibernate.action.queue.internal.constraint.ConstraintModel.normalizeIdentifier;
import static org.hibernate.action.queue.internal.constraint.ConstraintModel.normalizeTableExpression;
import static org.hibernate.action.queue.internal.constraint.ConstraintModel.tableNamesMatch;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/// Builds a complete ConstraintModel containing both foreign keys and unique constraints.
///
/// @author Steve Ebersole
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
		return primarySort.thenComparing( EntityTableMapping::relativePosition );
	}

	/// Main entry point creating the model from both the boot-time and run-time metadata.
	///
	/// @param mappingModel The run-time mapping model
	/// @param bootMappingModel The boot-time mapping model
	/// @param planningOptions Configuration options
	public static ConstraintModel buildConstraintModel(
			MappingMetamodelImplementor mappingModel,
			PlanningOptions planningOptions,
			MetadataImplementor bootMappingModel,
			SqlStringGenerationContext sqlStringGenerationContext) {
		return new ConstraintModelBuilder( planningOptions ).build( mappingModel, bootMappingModel, sqlStringGenerationContext );
	}

	private final PlanningOptions planningOptions;

	private ConstraintModelBuilder(PlanningOptions planningOptions) {
		this.planningOptions = planningOptions;
	}

	public ConstraintModel build(
			MappingMetamodelImplementor mappingModel,
			MetadataImplementor bootModel,
			SqlStringGenerationContext sqlStringGenerationContext) {
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
			collectFromEntityPersister(descriptor, foreignKeys, uniqueConstraints, seen, entityPersisters, bootModel, sqlStringGenerationContext);
		} );

		// Also scan collection persisters because join tables often show up there
		mappingModel.forEachCollectionDescriptor( (descriptor) -> {
			collectFromCollectionPersister(
					descriptor,
					foreignKeys,
					uniqueConstraints,
					seen,
					entityPersisters,
					bootModel,
					sqlStringGenerationContext
			);
		} );

		final var reclassifiedForeignKeys = reclassifyForeignKeyTargetTypes( foreignKeys, uniqueConstraints, entityPersisters );

		// Index unique constraints by table for fast lookup
		Map<String, List<UniqueConstraint>> uniqueConstraintsByTable = uniqueConstraints.stream()
				.collect(Collectors.groupingBy( constraint -> normalizeTableExpression( constraint.tableName() ) ));

		// Index foreign keys by target table (inbound FKs - pointing TO this table)
		Map<String, List<ForeignKey>> inboundForeignKeysByTable = reclassifiedForeignKeys.stream()
				.collect(Collectors.groupingBy( foreignKey -> normalizeTableExpression( foreignKey.targetTable() ) ));

		// Index foreign keys by key table (outbound FKs - FROM this table)
		Map<String, List<ForeignKey>> outboundForeignKeysByTable = reclassifiedForeignKeys.stream()
				.collect(Collectors.groupingBy( foreignKey -> normalizeTableExpression( foreignKey.keyTable() ) ));

		// Identify tables with cyclic foreign key relationships (bidirectional FKs)
		var tablesWithCyclicForeignKeys = identifyTablesWithCyclicForeignKeys(
				inboundForeignKeysByTable,
				outboundForeignKeysByTable
		);

		// Identify tables with self-referential associations
		var selfReferentialTables = identifySelfReferentialTables( reclassifiedForeignKeys );

		return new ConstraintModel(
				reclassifiedForeignKeys,
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
			Map<String, EntityPersister> entityPersisters,
			MetadataImplementor bootModel,
			SqlStringGenerationContext sqlStringGenerationContext) {

		// Collect unique constraint for primary key
		//		NOTE: may produce foreign-keys for composite primary-keys
		collectPrimaryKeyConstraint(descriptor, foreignKeys, uniqueConstraints, seen, entityPersisters);

		// Collect @NaturalId constraint if present
		collectNaturalIdConstraint(descriptor, uniqueConstraints);

		// Collect @Column(unique=true) constraints
		collectUniqueColumnConstraints(descriptor, uniqueConstraints, bootModel, sqlStringGenerationContext);

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
		if ( planningOptions.orderByUniqueKeySlots() ) {
			for ( EntityTableMapping tableMapping : descriptor.getTableMappings() ) {
				uniqueConstraints.add( new UniqueConstraint(
						tableMapping.getTableName(),
						"PRIMARY",
						UniqueConstraint.ConstraintType.PRIMARY_KEY,
						tableMapping.getKeyDetails(),
						determineConstraintDeferrability( tableMapping ),
						false, // Primary keys are never nullable
						null    // PK doesn't need property names (uses special extraction)
				) );
			}
		}

		// Check if identifier is an embeddable with @ManyToOne attributes (composite key scenario)
		EntityIdentifierMapping identifierMapping = descriptor.getIdentifierMapping();
		if (identifierMapping instanceof EmbeddableValuedModelPart embeddableId) {
			handleToOneAttributes( embeddableId.getEmbeddableTypeDescriptor(), foreignKeys, uniqueConstraints, seen, entityPersisters );
		}
	}

	/// Collect @NaturalId constraint if the entity has one.
	/// Phase 4: Support for @NaturalId unique constraints.
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

		var naturalIdAttributes = naturalIdMapping.getNaturalIdAttributes();

		// Extract property names from natural ID attributes
		String[] propertyNames = new String[naturalIdAttributes.size()];
		for (int i = 0; i < naturalIdAttributes.size(); i++) {
			propertyNames[i] = naturalIdAttributes.get(i).getAttributeName();
		}

		// Combine SelectableMappings from all natural ID attributes
		SelectableMappings columns = combineSelectableMappings(naturalIdAttributes);
		String tableName = determineConstraintTableName( columns, descriptor.getTableName() );

		uniqueConstraints.add(new UniqueConstraint(
				tableName,
				naturalIdMapping.getNavigableRole().getFullPath(),
				UniqueConstraint.ConstraintType.UNIQUE_KEY,
				columns,
				determineConstraintDeferrability( naturalIdMapping ),
				areColumnsNullable( columns ),
				propertyNames
		));
	}

	/// Combine multiple SelectableMappings from natural ID attributes into a single SelectableMappings.
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

	private String determineConstraintTableName(SelectableMappings columns, String fallbackTableName) {
		if ( columns == null ) {
			return fallbackTableName;
		}
		String tableName = null;
		for ( int i = 0; i < columns.getJdbcTypeCount(); i++ ) {
			final String columnTableName = columns.getSelectable( i ).getContainingTableExpression();
			if ( tableName == null ) {
				tableName = columnTableName;
			}
			else if ( !tableNamesMatch( tableName, columnTableName ) ) {
				return fallbackTableName;
			}
		}
		return tableName == null ? fallbackTableName : tableName;
	}

	/// Simple SelectableMappings implementation that combines multiple SelectableMapping objects.
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

	/// Collect @Column(unique=true) constraints.
	/// Phase 4: Support for single-column unique constraints.
	private void collectUniqueColumnConstraints(
			EntityPersister descriptor,
			List<UniqueConstraint> uniqueConstraints,
			MetadataImplementor bootModel,
			SqlStringGenerationContext sqlStringGenerationContext) {
		if ( !planningOptions.orderByUniqueKeySlots() || bootModel == null || sqlStringGenerationContext == null ) {
			return;
		}

		final PersistentClass persistentClass = bootModel.getEntityBinding( descriptor.getEntityName() );
		if ( persistentClass == null ) {
			return;
		}

		for ( Table table : getUniqueConstraintTables( persistentClass ) ) {
			final String tableName = table.getQualifiedName( sqlStringGenerationContext );
			for ( UniqueKey uniqueKey : table.getUniqueKeys().values() ) {
				addUniqueKeyConstraint(
						descriptor,
						uniqueConstraints,
						tableName,
						uniqueKey.getName(),
						uniqueKey.getColumns(),
						uniqueKey,
						persistentClass
				);
			}

			for ( Column column : table.getColumns() ) {
				if ( column.isUnique() && !table.isPrimaryKey( column ) ) {
					addUniqueKeyConstraint(
							descriptor,
							uniqueConstraints,
							tableName,
							column.getUniqueKeyName(),
							List.of( column ),
							null,
							persistentClass
					);
				}
			}
		}
	}

	private List<Table> getUniqueConstraintTables(PersistentClass persistentClass) {
		final LinkedHashSet<Table> tables = new LinkedHashSet<>( persistentClass.getTableClosure() );
		for ( var join : persistentClass.getJoinClosure() ) {
			tables.add( join.getTable() );
		}
		return List.copyOf( tables );
	}

	private void addUniqueKeyConstraint(
			EntityPersister descriptor,
			List<UniqueConstraint> uniqueConstraints,
			String tableName,
			String constraintName,
			List<Column> columns,
			org.hibernate.mapping.Constraint constraint,
			PersistentClass persistentClass) {
		final UniqueKeyRuntimeMapping runtimeMapping = resolveUniqueKeyRuntimeMapping(
				descriptor,
				persistentClass,
				tableName,
				columns
		);
		final String runtimeTableName = determineConstraintTableName( runtimeMapping == null ? null : runtimeMapping.columns(), tableName );
		if ( runtimeMapping == null || hasUniqueConstraintForColumns(
				uniqueConstraints,
				runtimeTableName,
				runtimeMapping.columns()
		) ) {
			return;
		}

		uniqueConstraints.add( new UniqueConstraint(
				runtimeTableName,
				constraintName,
				UniqueConstraint.ConstraintType.UNIQUE_KEY,
				runtimeMapping.columns(),
				determineConstraintDeferrability( constraint ),
				areColumnsNullable( runtimeMapping.columns() ),
				runtimeMapping.propertyNames()
		) );
	}

	private UniqueKeyRuntimeMapping resolveUniqueKeyRuntimeMapping(
			EntityPersister descriptor,
			PersistentClass persistentClass,
			String tableName,
			List<Column> columns) {
		if ( columns.isEmpty() ) {
			return null;
		}

		final List<SelectableMapping> selectables = new ArrayList<>( columns.size() );
		final LinkedHashSet<String> propertyNames = new LinkedHashSet<>();

		for ( Column column : columns ) {
			SelectableMatch selectableMatch = findSelectableForColumn( descriptor, tableName, column );
			if ( selectableMatch == null ) {
				selectableMatch = findSelectableForBootColumnOwner( descriptor, persistentClass, tableName, column );
			}
			if ( selectableMatch == null ) {
				return null;
			}
			selectables.add( selectableMatch.selectable() );
			propertyNames.add( selectableMatch.propertyName() );
		}

		return new UniqueKeyRuntimeMapping(
				new CompositeSelectableMappings( selectables ),
				propertyNames.toArray( String[]::new )
		);
	}

	private SelectableMatch findSelectableForColumn(
			EntityPersister descriptor,
			String tableName,
			Column column) {
		final SelectableMatch[] match = new SelectableMatch[1];
		descriptor.forEachAttributeMapping( (attributeMapping) -> {
			if ( match[0] != null || !( attributeMapping instanceof SingularAttributeMapping singularAttribute ) ) {
				return;
			}

			for ( int i = 0; i < singularAttribute.getJdbcTypeCount(); i++ ) {
				final SelectableMapping selectable = singularAttribute.getSelectable( i );
				if ( !selectable.isFormula()
						&& columnMatches( tableName, column, selectable ) ) {
					match[0] = new SelectableMatch( selectable, singularAttribute.getAttributeName() );
					return;
				}
			}
		} );
		return match[0];
	}

	private SelectableMatch findSelectableForBootColumnOwner(
			EntityPersister descriptor,
			PersistentClass persistentClass,
			String tableName,
			Column column) {
		for ( Property property : persistentClass.getPropertyClosure() ) {
			if ( !propertyContainsColumn( property, tableName, column ) ) {
				continue;
			}

			final SelectableMatch selectableMatch = findSelectableForProperty( descriptor, property.getName(), tableName, column );
			if ( selectableMatch != null ) {
				return selectableMatch;
			}
		}

		return null;
	}

	private boolean propertyContainsColumn(Property property, String tableName, Column column) {
		for ( Selectable selectable : property.getSelectables() ) {
			if ( selectable instanceof Column propertyColumn
					&& tableNamesMatch( tableName, propertyColumn.getValue().getTable().getName() )
					&& normalizeIdentifier( column.getCanonicalName() )
							.equals( normalizeIdentifier( propertyColumn.getCanonicalName() ) ) ) {
				return true;
			}
		}
		return false;
	}

	private SelectableMatch findSelectableForProperty(
			EntityPersister descriptor,
			String propertyName,
			String tableName,
			Column column) {
		if ( !( descriptor.findAttributeMapping( propertyName ) instanceof SingularAttributeMapping singularAttribute ) ) {
			return null;
		}

		for ( int i = 0; i < singularAttribute.getJdbcTypeCount(); i++ ) {
			final SelectableMapping selectable = singularAttribute.getSelectable( i );
			if ( selectable.isFormula() ) {
				continue;
			}
			if ( columnMatches( tableName, column, selectable ) ) {
				return new SelectableMatch( selectable, propertyName );
			}
		}

		return null;
	}

	private boolean columnMatches(String tableName, Column column, SelectableMapping selectable) {
		return tableNamesMatch( tableName, selectable.getContainingTableExpression() )
				&& normalizeIdentifier( column.getCanonicalName() )
						.equals( normalizeSelectableName( selectable ) );
	}

	private SelectableKey selectableKey(SelectableMapping selectable) {
		return new SelectableKey(
				normalizeTableExpression( selectable.getContainingTableExpression() ),
				normalizeSelectableName( selectable )
		);
	}

	private String normalizeSelectableName(SelectableMapping selectable) {
		final String selectableName = selectable.getSelectableName();
		return selectableName == null || selectableName.isEmpty()
				? normalizeIdentifier( selectable.getSelectionExpression() )
				: normalizeIdentifier( selectableName );
	}

	private boolean hasUniqueConstraintForColumns(
			List<UniqueConstraint> uniqueConstraints,
			String tableName,
			SelectableMappings columns) {
		for ( UniqueConstraint uniqueConstraint : uniqueConstraints ) {
			if ( tableNamesMatch( uniqueConstraint.tableName(), tableName )
					&& columnsMatch( uniqueConstraint.columns(), columns ) ) {
				return true;
			}
		}
		return false;
	}

	private record UniqueKeyRuntimeMapping(SelectableMappings columns, String[] propertyNames) {
	}

	private record SelectableMatch(SelectableMapping selectable, String propertyName) {
	}

	private record SelectableKey(String tableExpression, String selectionExpression) {
	}

	private void collectFromCollectionPersister(
			CollectionPersister descriptor,
			List<ForeignKey> foreignKeys,
			List<UniqueConstraint> uniqueConstraints,
			IdentityHashMap<ForeignKeyDescriptor, Boolean> seen,
			Map<String, EntityPersister> entityPersisters,
			MetadataImplementor bootModel,
			SqlStringGenerationContext sqlStringGenerationContext) {
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

		collectCollectionTableUniqueConstraints(
				descriptor,
				attributeMapping,
				uniqueConstraints,
				bootModel,
				sqlStringGenerationContext
		);
	}

	private void collectCollectionTableUniqueConstraints(
			CollectionPersister descriptor,
			PluralAttributeMapping attributeMapping,
			List<UniqueConstraint> uniqueConstraints,
			MetadataImplementor bootModel,
			SqlStringGenerationContext sqlStringGenerationContext) {
		if ( !planningOptions.orderByUniqueKeySlots() || bootModel == null || sqlStringGenerationContext == null ) {
			return;
		}

		final org.hibernate.mapping.Collection collectionBinding = bootModel.getCollectionBinding( descriptor.getRole() );
		if ( collectionBinding == null ) {
			return;
		}

		final Table table = collectionBinding.getCollectionTable();
		if ( table == null ) {
			return;
		}

		final String tableName = table.getQualifiedName( sqlStringGenerationContext );
		final var primaryKey = table.getPrimaryKey();
		if ( primaryKey != null && descriptor.isManyToMany() && descriptor.hasIndex() ) {
			addCollectionUniqueConstraint(
					attributeMapping,
					uniqueConstraints,
					tableName,
					"PRIMARY",
					UniqueConstraint.ConstraintType.PRIMARY_KEY,
					primaryKey.getColumns(),
					primaryKey
			);
		}

		for ( UniqueKey uniqueKey : table.getUniqueKeys().values() ) {
			addCollectionUniqueConstraint(
					attributeMapping,
					uniqueConstraints,
					tableName,
					uniqueKey.getName(),
					UniqueConstraint.ConstraintType.UNIQUE_KEY,
					uniqueKey.getColumns(),
					uniqueKey
			);
		}

		for ( Column column : table.getColumns() ) {
			if ( column.isUnique() && !table.isPrimaryKey( column ) ) {
				addCollectionUniqueConstraint(
						attributeMapping,
						uniqueConstraints,
						tableName,
						column.getUniqueKeyName(),
						UniqueConstraint.ConstraintType.UNIQUE_KEY,
						List.of( column ),
						null
				);
			}
		}
	}

	private void addCollectionUniqueConstraint(
			PluralAttributeMapping attributeMapping,
			List<UniqueConstraint> uniqueConstraints,
			String tableName,
			String constraintName,
			UniqueConstraint.ConstraintType type,
			List<Column> columns,
			org.hibernate.mapping.Constraint constraint) {
		final SelectableMappings runtimeColumns = resolveCollectionUniqueKeyRuntimeColumns(
				attributeMapping,
				tableName,
				columns
		);
		final String runtimeTableName = determineConstraintTableName( runtimeColumns, tableName );
		if ( runtimeColumns == null || hasUniqueConstraintForColumns( uniqueConstraints, runtimeTableName, runtimeColumns ) ) {
			return;
		}

		uniqueConstraints.add( new UniqueConstraint(
				runtimeTableName,
				constraintName,
				type,
				runtimeColumns,
				determineConstraintDeferrability( constraint ),
				areColumnsNullable( runtimeColumns ),
				null
		) );
	}

	private SelectableMappings resolveCollectionUniqueKeyRuntimeColumns(
			PluralAttributeMapping attributeMapping,
			String tableName,
			List<Column> columns) {
		if ( columns.isEmpty() ) {
			return null;
		}

		final List<SelectableMapping> selectables = new ArrayList<>( columns.size() );
		for ( Column column : columns ) {
			final SelectableMapping selectable = findCollectionSelectableForColumn( attributeMapping, tableName, column );
			if ( selectable == null ) {
				return null;
			}
			selectables.add( selectable );
		}

		return new CompositeSelectableMappings( selectables );
	}

	private SelectableMapping findCollectionSelectableForColumn(
			PluralAttributeMapping attributeMapping,
			String tableName,
			Column column) {
		final SelectableMapping keySelectable = findSelectableForColumn(
				attributeMapping.getKeyDescriptor().getKeyPart(),
				tableName,
				column
		);
		if ( keySelectable != null ) {
			return keySelectable;
		}

		final SelectableMapping indexSelectable = findSelectableForColumn(
				attributeMapping.getIndexDescriptor(),
				tableName,
				column
		);
		if ( indexSelectable != null ) {
			return indexSelectable;
		}

		final SelectableMapping elementSelectable = findSelectableForColumn(
				attributeMapping.getElementDescriptor(),
				tableName,
				column
		);
		if ( elementSelectable != null ) {
			return elementSelectable;
		}

		return findSelectableForColumn(
				attributeMapping.getIdentifierDescriptor(),
				tableName,
				column
		);
	}

	private SelectableMapping findSelectableForColumn(
			SelectableMappings selectableMappings,
			String tableName,
			Column column) {
		if ( selectableMappings == null ) {
			return null;
		}

		for ( int i = 0; i < selectableMappings.getJdbcTypeCount(); i++ ) {
			final SelectableMapping selectable;
			try {
				selectable = selectableMappings.getSelectable( i );
			}
			catch (ArrayIndexOutOfBoundsException ignored) {
				return null;
			}
			if ( !selectable.isFormula()
					&& columnMatches( tableName, column, selectable ) ) {
				return selectable;
			}
		}

		return null;
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
	/// See [- secondary tables].
	/// See [- joined inheritance tables].
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
				determineConstraintDeferrability( source )
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
					determineConstraintDeferrability( fk ),
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
		if ( seen.containsKey( fk ) ) {
			return;
		}
		seen.put( fk, Boolean.TRUE );
		addConstraint( fk, keySide, targetSide, nullable, foreignKeys, entityPersisters );
	}

	private void addConstraint(
			ForeignKeyDescriptor fkDesc,
			SelectableMappings keySide,
			SelectableMappings targetSide,
			boolean nullable,
			List<ForeignKey> foreignKeys,
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

		foreignKeys.add(new ForeignKey(
				fkDesc.getKeyTable(),
				fkDesc.getTargetTable(),
				keySide,
				targetSide,
				targetType,
				true,
				nullable,
				determineConstraintDeferrability( fkDesc )
		));
	}

	private Deferrability determineConstraintDeferrability(org.hibernate.mapping.Constraint constraint) {
		// Hibernate does not currently expose FK/UK deferrability in boot/runtime metadata.
		// Treat unknown as non-deferrable so planning remains conservative.
		return Deferrability.NOT_DEFERRABLE;
	}

	private Deferrability determineConstraintDeferrability(Object constraintSource) {
		// Hibernate does not currently expose FK/UK deferrability in boot/runtime metadata.
		// Treat unknown as non-deferrable so planning remains conservative.
		return Deferrability.NOT_DEFERRABLE;
	}

	/// Determine what the foreign key targets (primary key, unique key, or non-unique)
	private ForeignKey.TargetType determineTargetType(
			ForeignKeyDescriptor fkDesc,
			SelectableMappings targetSide,
			Map<String, EntityPersister> entityPersisters) {

		// Try to find the target entity persister
		String targetTable = fkDesc.getTargetTable();

		// Check each entity persister to see if it owns this table and if the FK targets its PK
		for (EntityPersister persister : entityPersisters.values()) {
			if (tableNamesMatch( persister.getTableName(), targetTable )) {
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

	private ArrayList<ForeignKey> reclassifyForeignKeyTargetTypes(
			ArrayList<ForeignKey> foreignKeys,
			ArrayList<UniqueConstraint> uniqueConstraints,
			Map<String, EntityPersister> entityPersisters) {
		final ArrayList<ForeignKey> reclassified = new ArrayList<>( foreignKeys.size() );

		for ( ForeignKey foreignKey : foreignKeys ) {
			final ForeignKey.TargetType targetType = determineTargetType(
					foreignKey,
					uniqueConstraints,
					entityPersisters
			);
			reclassified.add( new ForeignKey(
					foreignKey.keyTable(),
					foreignKey.targetTable(),
					foreignKey.keyColumns(),
					foreignKey.targetColumns(),
					targetType,
					foreignKey.isAssociation(),
					foreignKey.nullable(),
					foreignKey.deferrability()
			) );
		}

		return reclassified;
	}

	private ForeignKey.TargetType determineTargetType(
			ForeignKey foreignKey,
			ArrayList<UniqueConstraint> uniqueConstraints,
			Map<String, EntityPersister> entityPersisters) {
		final EntityPersister targetPersister = findPersisterForTable( foreignKey.targetTable(), entityPersisters );
		if ( targetPersister != null ) {
			final EntityIdentifierMapping idMapping = targetPersister.getIdentifierMapping();
			if ( idMapping != null && columnsMatch( foreignKey.targetColumns(), idMapping ) ) {
				return ForeignKey.TargetType.PRIMARY_KEY;
			}
		}

		for ( UniqueConstraint uniqueConstraint : uniqueConstraints ) {
			if ( tableNamesMatch( uniqueConstraint.tableName(), foreignKey.targetTable() )
					&& !uniqueConstraint.isPrimaryKey()
					&& columnsMatch( foreignKey.targetColumns(), uniqueConstraint.columns() ) ) {
				return ForeignKey.TargetType.UNIQUE_KEY;
			}
		}

		return ForeignKey.TargetType.NON_UNIQUE;
	}

	private EntityPersister findPersisterForTable(String tableName, Map<String, EntityPersister> entityPersisters) {
		for ( EntityPersister persister : entityPersisters.values() ) {
			if ( tableNamesMatch( persister.getTableName(), tableName ) ) {
				return persister;
			}
		}
		return null;
	}

	/// Check if two SelectableMappings refer to the same columns
	private boolean columnsMatch(SelectableMappings columns1, SelectableMappings columns2) {
		if (columns1.getJdbcTypeCount() != columns2.getJdbcTypeCount()) {
			return false;
		}

		for ( int i = 0; i < columns1.getJdbcTypeCount(); i++ ) {
			if ( !selectableKey( columns1.getSelectable( i ) ).equals( selectableKey( columns2.getSelectable( i ) ) ) ) {
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
				if (tableNamesMatch( keyTable, targetTable )) {
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
					if (tableNamesMatch( outbound.targetTable(), sourceTable )) {
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
