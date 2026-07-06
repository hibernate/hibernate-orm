/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.materialize.CollectionKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.DependentTableKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.IndexMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.ResolvedForeignKey;
import org.hibernate.boot.mapping.internal.materialize.ResolvedIndex;
import org.hibernate.boot.mapping.internal.materialize.ResolvedUniqueKey;
import org.hibernate.boot.mapping.internal.materialize.UniqueKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.boot.mapping.internal.sources.ToOneSource.JoinColumnOrFormulaSource;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.models.ModelsException;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;

import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;

/// Binds table keys that depend on an already-bound entity hierarchy identifier.
///
/// This phase completes mapping tables whose key columns are derived from an
/// entity identifier rather than declared independently:
///
/// - joined-subclass tables
/// - secondary tables
/// - association join tables represented as entity joins
/// - collection tables
///
/// The phase creates dependent key values and primary keys, then records
/// [TableForeignKeyBinding] work for the later foreign-key phase.  It also
/// applies collection-table indexes and unique constraints once the table's key
/// and value columns are available.
///
/// @since 9.0
/// @author Steve Ebersole
public class TableKeyBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;
	private final CollectionKeyMappingMaterializer collectionKeyMappingMaterializer;
	private final DependentTableKeyMappingMaterializer dependentTableKeyMappingMaterializer = new DependentTableKeyMappingMaterializer();
	private final IndexMappingMaterializer indexMappingMaterializer = new IndexMappingMaterializer();
	private final UniqueKeyMappingMaterializer uniqueKeyMappingMaterializer = new UniqueKeyMappingMaterializer();

	public TableKeyBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
		this.collectionKeyMappingMaterializer = new CollectionKeyMappingMaterializer( bindingState::getEntityBinding );
	}

	public void bindTableKeys() {
		if ( entityBinder.getTypeBinding() instanceof JoinedSubclass joinedSubclass ) {
			bindJoinedSubclassKey( joinedSubclass );
		}

		entityBinder.getTypeBinding().getJoins().forEach( this::bindSecondaryTableKey );
		bindingState.forEachCollectionTableBinding( (collectionTableBinding) -> {
			if ( collectionTableBinding.collection().getOwner() == entityBinder.getTypeBinding() ) {
				bindCollectionTableKey( collectionTableBinding );
			}
		} );
	}

	private void bindJoinedSubclassKey(JoinedSubclass joinedSubclass) {
		final IdentifierBinding rootIdentifierBinding = resolveIdentifierBinding();
		final DependantValue key = createDependentKeyValue(
				joinedSubclass.getTable(),
				rootIdentifierBinding,
				primaryKeyJoinColumns()
		);
		applyJoinedSubclassOnDelete( key );
		joinedSubclass.setKey( key );
		dependentTableKeyMappingMaterializer.materializePrimaryKey(
				dependentTableKeyMappingMaterializer.resolvePrimaryKey(
						entityBinder.getTypeBinding(),
						joinedSubclass.getEntityName(),
						joinedSubclass.getTable(),
						key
				)
		);
		bindingState.addTableForeignKeyBinding( new TableForeignKeyBinding(
				entityBinder.getTypeBinding(),
				key,
				entityBinder.getSuperEntityBinder().getTypeBinding().getEntityName(),
				primaryKeyJoinColumnForeignKeySource(),
				resolveTableForeignKey(
						key,
						entityBinder.getSuperEntityBinder().getTypeBinding().getEntityName(),
						rootIdentifierBinding,
						joinedSubclass.getEntityName()
				)
		) );
	}

	private void applyJoinedSubclassOnDelete(DependantValue key) {
		final OnDelete onDelete = entityBinder.getManagedType().getClassDetails().getDirectAnnotationUsage( OnDelete.class );
		if ( onDelete != null ) {
			key.setOnDeleteAction( onDelete.action() );
		}
	}

	private void bindSecondaryTableKey(Join join) {
		final IdentifierBinding rootIdentifierBinding = resolveIdentifierBinding();
		final AssociationTableBinding associationTableBinding = bindingState.getAssociationTableBinding( join );
		final org.hibernate.boot.mapping.internal.relational.SecondaryTable secondaryTable =
				bindingState.getSecondaryTable( join.getTable() );
		final DependantValue key = associationTableBinding == null
				? createDependentKeyValue(
						join.getTable(),
						rootIdentifierBinding,
						secondaryTable == null ? List.of() : secondaryTable.primaryKeyJoinColumns()
				)
				: createDependentKeyValue( join.getTable(), rootIdentifierBinding, associationTableBinding );
		join.setKey( key );
		dependentTableKeyMappingMaterializer.materializePrimaryKey(
				dependentTableKeyMappingMaterializer.resolvePrimaryKey(
						entityBinder.getTypeBinding(),
						entityBinder.getTypeBinding().getEntityName() + "." + join.getTable().getName(),
						join.getTable(),
						key
				)
		);
		if ( !join.isInverse() ) {
			bindingState.addTableForeignKeyBinding( new TableForeignKeyBinding(
					entityBinder.getTypeBinding(),
					key,
					entityBinder.getTypeBinding().getEntityName(),
					associationTableBinding == null
							? findSecondaryTableForeignKeySource( join )
							: associationTableBinding.foreignKeySource(),
					resolveTableForeignKey(
							key,
							entityBinder.getTypeBinding().getEntityName(),
							entityBinder.getTypeBinding().getEntityName() + "." + join.getTable().getName()
					)
			) );
		}
	}

	private void bindCollectionTableKey(CollectionTableBinding collectionTableBinding) {
		final IdentifierBinding rootIdentifierBinding = resolveIdentifierBinding();
		final DependantValue key = createDependentKeyValue(
				collectionTableBinding.collection().getCollectionTable(),
				rootIdentifierBinding,
				collectionTableBinding
		);
		key.setOnDeleteAction( collectionTableBinding.onDeleteAction() );
		collectionTableBinding.collection().setKey( key );
		if ( !key.hasFormula() ) {
			if ( collectionTableBinding.oneToManyAssociationTable() ) {
				final boolean elementPrimaryKey = materializeOneToManyAssociationTablePrimaryKey( collectionTableBinding );
				for ( Column column : collectionTableBinding.collection().getElement().getColumns() ) {
					column.setUnique( false );
				}
				if ( !elementPrimaryKey ) {
					uniqueKeyMappingMaterializer.materializeUniqueKey(
							ResolvedUniqueKey.from(
									(SimpleValue) collectionTableBinding.collection().getElement(),
									metadataBuildingContext(),
									collectionTableBinding.collection().getRole() + ".element"
							)
					);
				}
				applyInverseJoinColumnUniqueKeys( collectionTableBinding );
			}
			else {
				collectionKeyMappingMaterializer.materializePrimaryKeyIfNeeded(
						collectionKeyMappingMaterializer.resolveTableKey( collectionTableBinding.collection() )
				);
			}
		}
		createOneToManyBackref( collectionTableBinding, key );
		createOneToManyIndexBackref( collectionTableBinding, key );
		if ( !key.hasFormula() ) {
			bindingState.addTableForeignKeyBinding( new TableForeignKeyBinding(
					entityBinder.getTypeBinding(),
					key,
					entityBinder.getTypeBinding().getEntityName(),
					collectionTableBinding.foreignKeySource(),
					resolveTableForeignKey(
						key,
						entityBinder.getTypeBinding().getEntityName(),
						collectionTableBinding.collection().getRole()
					)
			) );
		}
		applyUniqueConstraints( collectionTableBinding );
		applyIndexes( collectionTableBinding );
	}

	private boolean materializeOneToManyAssociationTablePrimaryKey(CollectionTableBinding collectionTableBinding) {
		if ( collectionTableBinding.collection() instanceof IndexedCollection indexedCollection
				&& !indexIsPartOfElement( indexedCollection ) ) {
			final PrimaryKey primaryKey = new PrimaryKey( indexedCollection.getCollectionTable() );
			primaryKey.addColumns( indexedCollection.getKey() );
			primaryKey.addColumns( indexedCollection.getIndex() );
			indexedCollection.getCollectionTable().setPrimaryKey( primaryKey );
			return false;
		}

		collectionKeyMappingMaterializer.materializeValuePrimaryKey(
				collectionTableBinding.collection().getCollectionTable(),
				collectionTableBinding.collection().getElement(),
				collectionTableBinding.collection().getRole() + ".element"
		);
		return true;
	}

	private boolean indexIsPartOfElement(IndexedCollection collection) {
		for ( Selectable selectable : collection.getIndex().getSelectables() ) {
			if ( selectable.isFormula() || !collection.getCollectionTable().containsColumn( (Column) selectable ) ) {
				return true;
			}
		}
		return false;
	}

	private void createOneToManyBackref(CollectionTableBinding collectionTableBinding, DependantValue key) {
		if ( collectionTableBinding.collection().isInverse()
				|| key.hasFormula()
				|| key.isNullable()
				|| !( collectionTableBinding.collection().getElement() instanceof org.hibernate.mapping.OneToMany oneToMany ) ) {
			return;
		}
		final var referencedEntity = bindingState.getEntityBinding( oneToMany.getReferencedEntityName() );
		if ( referencedEntity == null ) {
			return;
		}
		if ( referencedEntity.getKey().getColumns().containsAll( key.getColumns() ) ) {
			return;
		}
		final Backref backref = new Backref();
		backref.setName(
				"_" + collectionTableBinding.collection().getRole()
						+ "_" + key.getColumns().get( 0 ).getName()
						+ "Backref"
		);
		backref.setOptional( true );
		backref.setUpdatable( false );
		backref.setSelectable( false );
		backref.setCollectionRole( collectionTableBinding.collection().getRole() );
		backref.setEntityName( collectionTableBinding.collection().getOwner().getEntityName() );
		backref.setValue( key );
		referencedEntity.addProperty( backref );
	}

	private void createOneToManyIndexBackref(CollectionTableBinding collectionTableBinding, DependantValue key) {
		if ( collectionTableBinding.collection().isInverse()
				|| key.hasFormula()
				|| key.isNullable()
				|| !( collectionTableBinding.collection() instanceof org.hibernate.mapping.List list )
				|| !( list.getElement() instanceof org.hibernate.mapping.OneToMany oneToMany ) ) {
			return;
		}
		final var referencedEntity = bindingState.getEntityBinding( oneToMany.getReferencedEntityName() );
		if ( referencedEntity == null ) {
			return;
		}
		final IndexBackref backref = new IndexBackref();
		backref.setName( "_" + collectionAttributeName( collectionTableBinding ) + "IndexBackref" );
		backref.setOptional( true );
		backref.setUpdatable( false );
		backref.setSelectable( false );
		backref.setCollectionRole( list.getRole() );
		backref.setEntityName( list.getOwner().getEntityName() );
		backref.setValue( list.getIndex() );
		referencedEntity.addProperty( backref );
	}

	private void applyUniqueConstraints(CollectionTableBinding collectionTableBinding) {
		for ( jakarta.persistence.UniqueConstraint uniqueConstraint : collectionTableBinding.uniqueConstraints() ) {
			final Table table = collectionTableBinding.collection().getCollectionTable();
			validateUniqueConstraintColumns( uniqueConstraint.columnNames(), table.getName() );
			final ArrayList<Column> uniqueKeyColumns = new ArrayList<>( uniqueConstraint.columnNames().length );
			for ( String columnName : uniqueConstraint.columnNames() ) {
				uniqueKeyColumns.add( resolveColumn( table, columnName ) );
			}
			uniqueKeyMappingMaterializer.materializeUniqueKey(
					ResolvedUniqueKey.explicit(
							table,
							uniqueKeyColumns,
							bindingState.getMetadataBuildingContext(),
							StringHelper.nullIfEmpty( uniqueConstraint.name() ),
							StringHelper.isNotEmpty( uniqueConstraint.name() ),
							uniqueConstraint.options(),
							null,
							collectionTableBinding.collection().getRole()
					)
			);
		}
	}

	private void applyInverseJoinColumnUniqueKeys(CollectionTableBinding collectionTableBinding) {
		final List<JoinColumn> inverseJoinColumns = collectionTableBinding.inverseJoinColumns();
		if ( inverseJoinColumns.isEmpty() ) {
			return;
		}

		final Table table = collectionTableBinding.collection().getCollectionTable();
		final List<Column> elementColumns = collectionTableBinding.collection().getElement().getColumns();
		for ( int i = 0; i < inverseJoinColumns.size(); i++ ) {
			final JoinColumn inverseJoinColumn = inverseJoinColumns.get( i );
			if ( !inverseJoinColumn.unique() ) {
				continue;
			}
			final Column column = StringHelper.isNotEmpty( inverseJoinColumn.name() )
					? resolveColumn( table, inverseJoinColumn.name() )
					: elementColumns.get( i );
			uniqueKeyMappingMaterializer.materializeUniqueKey(
					ResolvedUniqueKey.from( column, table, metadataBuildingContext() )
			);
		}
	}

	private void validateUniqueConstraintColumns(String[] columnNames, String tableName) {
		if ( columnNames.length == 0 ) {
			throw new AnnotationException( "Unique constraint on table '" + tableName + "' did not specify columns" );
		}
		for ( String columnName : columnNames ) {
			if ( StringHelper.isEmpty( columnName ) ) {
				throw new AnnotationException(
						"Unique constraint on table '" + tableName + "' specified an empty column name"
				);
			}
		}
	}

	private void applyIndexes(CollectionTableBinding collectionTableBinding) {
		for ( jakarta.persistence.Index indexAnn : collectionTableBinding.indexes() ) {
			if ( StringHelper.isEmpty( indexAnn.columnList() ) ) {
				continue;
			}

			final Table table = collectionTableBinding.collection().getCollectionTable();
			final String indexName = StringHelper.isEmpty( indexAnn.name() )
					? "idx_" + table.getName() + "_" + Integer.toHexString( indexAnn.columnList().hashCode() )
					: indexAnn.name();
			final List<Selectable> indexColumns = new ArrayList<>();
			final List<String> columnNames = new ArrayList<>();
			for ( String columnName : indexAnn.columnList().split( "," ) ) {
				final String trimmedColumnName = columnName.trim();
				columnNames.add( trimmedColumnName );
				indexColumns.add( resolveColumn( table, trimmedColumnName ) );
			}
			indexMappingMaterializer.materializeIndex(
					ResolvedIndex.explicit(
							table,
							indexColumns,
							columnNames,
							bindingState.getMetadataBuildingContext(),
							indexName,
							indexAnn.unique(),
							indexAnn.type(),
							indexAnn.using(),
							indexAnn.options(),
							null,
							collectionTableBinding.collection().getRole()
					)
			);
		}
	}

	private ForeignKeySource findSecondaryTableForeignKeySource(Join join) {
		final org.hibernate.boot.mapping.internal.relational.SecondaryTable secondaryTable = bindingState.getSecondaryTable( join.getTable() );
		return secondaryTable == null ? null : secondaryTable.foreignKeySource();
	}

	private Column resolveColumn(Table table, String columnName) {
		final Column column = table.getColumn( new Column( columnName ) );
		if ( column != null ) {
			return column;
		}
		final Column physicalColumn = bindingState.getRelationalModelCorrespondences()
				.columnNames()
				.findPhysicalColumn( table, bindingState.getDatabase().toIdentifier( columnName ) );
		if ( physicalColumn != null ) {
			return physicalColumn;
		}
		throw new ModelsException( "Could not resolve collection table column `" + columnName + "` on " + table.getName() );
	}

	private IdentifierBinding resolveIdentifierBinding() {
		final EntityTypeMetadata rootType = entityBinder.getManagedType().getHierarchy().getRoot();
		final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding( rootType );
		if ( entityIdentifierBinding == null ) {
			throw new ModelsException( "Identifier binding not available for " + rootType.getEntityName() );
		}
		return entityIdentifierBinding;
	}

	private ResolvedForeignKey resolveTableForeignKey(
			DependantValue key,
			String referencedEntityName,
			IdentifierBinding entityIdentifierBinding,
			String sourceRole) {
		return ResolvedForeignKey.from(
				key,
				referencedEntityName,
				SelectableOrderResolver.resolveByTargetOrder(
						key.getColumns(),
						targetIdentifierColumns( entityIdentifierBinding ),
						sourceRole
				)
		);
	}

	private ResolvedForeignKey resolveTableForeignKey(
			DependantValue key,
			String referencedEntityName,
			String sourceRole) {
		if ( key.getWrappedValue() instanceof SortableValue sortableValue ) {
			sortableValue.sortProperties();
		}
		return ResolvedForeignKey.from(
				key,
				referencedEntityName,
				SelectableOrderResolver.resolveByTargetOrder(
						key.getColumns(),
						key.getWrappedValue().getColumns(),
						sourceRole
				)
		);
	}

	private List<Column> targetIdentifierColumns(IdentifierBinding entityIdentifierBinding) {
		if ( entityIdentifierBinding.value() instanceof SortableValue sortableValue ) {
			sortableValue.sortProperties();
		}
		return entityIdentifierBinding.value().getColumns();
	}

	private DependantValue createDependentKeyValue(Table table, IdentifierBinding entityIdentifierBinding) {
		return createDependentKeyValue( table, entityIdentifierBinding, List.of() );
	}

	private List<Column> dependentTableTargetColumns(IdentifierBinding entityIdentifierBinding) {
		if ( entityBinder.getTypeBinding() instanceof JoinedSubclass joinedSubclass && joinedSubclass.getKey() != null ) {
			return joinedSubclass.getKey().getColumns();
		}
		return targetIdentifierColumns( entityIdentifierBinding );
	}

	private DependantValue createDependentKeyValue(
			Table table,
			IdentifierBinding entityIdentifierBinding,
			List<JoinColumn> joinColumns) {
		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				table,
				entityIdentifierBinding.value()
		);
		key.setNullable( false );
		key.setUpdateable( false );
		final List<Column> targetColumns = dependentTableTargetColumns( entityIdentifierBinding );
		final var orderedJoinColumns = ToOneAttributeBinder.orderJoinColumns(
				joinColumns,
				targetColumns,
				bindingState.getDatabase(),
				entityBinder.getManagedType().getClassDetails().getClassName(),
				table.getName()
		);
		for ( int i = 0; i < targetColumns.size(); i++ ) {
			final Column identifierColumn = targetColumns.get( i );
			final Column keyColumn = orderedJoinColumns.isEmpty()
					? copyKeyColumn( identifierColumn, true )
					: bindKeyColumn( table, identifierColumn, orderedJoinColumns.get( i ) );
			table.addColumn( keyColumn );
			key.addColumn( keyColumn, true, false );
		}
		key.setSorted( true );
		return key;
	}

	private List<JoinColumn> primaryKeyJoinColumns() {
		final var classDetails = entityBinder.getManagedType().getClassDetails();
		final var primaryKeyJoinColumns = classDetails.getDirectAnnotationUsage( PrimaryKeyJoinColumns.class );
		if ( primaryKeyJoinColumns != null ) {
			return primaryKeyJoinColumns( primaryKeyJoinColumns.value() );
		}

		final PrimaryKeyJoinColumn[] repeatableColumns = classDetails.getRepeatedAnnotationUsages(
				PrimaryKeyJoinColumn.class,
				entityBinder.getBindingContext().getModelsContext()
		);
		return primaryKeyJoinColumns( repeatableColumns );
	}

	private ForeignKeySource primaryKeyJoinColumnForeignKeySource() {
		final var classDetails = entityBinder.getManagedType().getClassDetails();
		final var primaryKeyJoinColumns = classDetails.getDirectAnnotationUsage( PrimaryKeyJoinColumns.class );
		if ( primaryKeyJoinColumns != null ) {
			return ForeignKeySource.firstSpecified(
					ForeignKeySource.fromFirstSpecifiedPrimaryKeyJoinColumn( primaryKeyJoinColumns.value() ),
					ForeignKeySource.from( primaryKeyJoinColumns )
			);
		}

		final PrimaryKeyJoinColumn[] repeatableColumns = classDetails.getRepeatedAnnotationUsages(
				PrimaryKeyJoinColumn.class,
				entityBinder.getBindingContext().getModelsContext()
		);
		return ForeignKeySource.fromFirstSpecifiedPrimaryKeyJoinColumn( repeatableColumns );
	}

	private List<JoinColumn> primaryKeyJoinColumns(PrimaryKeyJoinColumn[] primaryKeyJoinColumns) {
		if ( primaryKeyJoinColumns.length == 0 ) {
			return List.of();
		}
		final var result = new ArrayList<JoinColumn>( primaryKeyJoinColumns.length );
		Arrays.stream( primaryKeyJoinColumns ).forEach( (primaryKeyJoinColumn) -> result.add(
				JoinColumnJpaAnnotation.toJoinColumn(
						primaryKeyJoinColumn,
						entityBinder.getBindingContext().getModelsContext()
				)
		) );
		return result;
	}

	private DependantValue createDependentKeyValue(
			Table table,
			IdentifierBinding entityIdentifierBinding,
			AssociationTableBinding associationTableBinding) {
		final ReferencedOwnerKey referencedOwnerKey = resolveReferencedOwnerKey(
				entityBinder.getTypeBinding(),
				associationTableBinding.joinColumns(),
				associationTableBinding.join().getTable().getName()
		);
		if ( referencedOwnerKey != null ) {
			return createDependentKeyValue(
					table,
					referencedOwnerKey,
					associationTableBinding.joinColumns(),
					associationTableBinding.join().getTable().getName(),
					false
			);
		}

		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				table,
				entityIdentifierBinding.value()
		);
		key.setNullable( false );
		key.setUpdateable( false );

		final List<Column> targetColumns = targetIdentifierColumns( entityIdentifierBinding );
		final var orderedJoinColumns = ToOneAttributeBinder.orderJoinColumns(
				associationTableBinding.joinColumns(),
				targetColumns,
				bindingState.getDatabase(),
				entityBinder.getManagedType().getClassDetails().getClassName(),
				associationTableBinding.join().getTable().getName()
		);
		for ( int i = 0; i < targetColumns.size(); i++ ) {
			final Column identifierColumn = targetColumns.get( i );
			key.addColumn(
					bindKeyColumn( table, identifierColumn, orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i ) ),
					true,
					false
			);
		}
		key.setSorted( true );
		return key;
	}

	private DependantValue createDependentKeyValue(
			Table table,
			IdentifierBinding entityIdentifierBinding,
			CollectionTableBinding collectionTableBinding) {
		final ReferencedOwnerKey referencedOwnerKey = resolveReferencedOwnerKey(
				collectionTableBinding.collection().getOwner(),
				collectionTableBinding.joinColumns(),
				collectionTableBinding.collection().getRole()
		);
		if ( referencedOwnerKey != null ) {
			collectionTableBinding.collection().setReferencedPropertyName( referencedOwnerKey.property().getName() );
			bindingState.addPropertyReference(
					collectionTableBinding.collection().getOwner().getEntityName(),
					referencedOwnerKey.property().getName()
			);
			final DependantValue key = createDependentKeyValue(
					table,
					referencedOwnerKey,
					collectionTableBinding.joinColumns(),
					collectionTableBinding.collection().getRole(),
					true
			);
			applyCollectionKeyNullabilityAndMutability( collectionTableBinding, key );
			return key;
		}

		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				table,
				entityIdentifierBinding.value()
		);
		key.setNullable( collectionKeyNullable( collectionTableBinding.joinColumns() ) );
		key.setUpdateable( collectionKeyUpdatable( collectionTableBinding.joinColumns() ) );

		final List<Column> targetColumns = collectionTableTargetColumns( collectionTableBinding, entityIdentifierBinding );
		final var orderedJoinColumns = ToOneAttributeBinder.orderJoinColumnSources(
				collectionTableBinding.joinColumnOrFormulas(),
				targetColumns,
				bindingState.getDatabase(),
				entityBinder.getManagedType().getClassDetails().getClassName(),
				collectionTableBinding.collection().getRole()
		);
		for ( int i = 0; i < targetColumns.size(); i++ ) {
			final JoinColumnOrFormulaSource joinColumnOrFormula = orderedJoinColumns.isEmpty()
					? null
					: orderedJoinColumns.get( i );
			if ( joinColumnOrFormula != null && joinColumnOrFormula.formula() != null ) {
				key.addFormula( new Formula( joinColumnOrFormula.formula().value() ) );
				continue;
			}
			final JoinColumn joinColumn = joinColumnOrFormula == null ? null : joinColumnOrFormula.column();
			final Column identifierColumn = targetColumns.get( i );
			final Column keyColumn = orderedJoinColumns.isEmpty()
					? bindKeyColumn(
							table,
							identifierColumn,
							null,
							() -> implicitCollectionKeyColumnName( collectionTableBinding, identifierColumn ),
							key.isNullable()
					)
					: bindKeyColumn(
							table,
							identifierColumn,
							joinColumn,
							() -> implicitCollectionKeyColumnName( collectionTableBinding, identifierColumn ),
							key.isNullable()
					);
			table.addColumn( keyColumn );
			key.addColumn(
					keyColumn,
					joinColumn == null || joinColumn.insertable(),
					joinColumn == null || joinColumn.updatable()
			);
		}
		key.setSorted( true );
		applyCollectionKeyNullabilityAndMutability( collectionTableBinding, key );
		return key;
	}

	private void applyCollectionKeyNullabilityAndMutability(CollectionTableBinding collectionTableBinding, DependantValue key) {
		key.setNullable( collectionKeyNullable( collectionTableBinding.joinColumns() ) );
		key.setUpdateable( collectionKeyUpdatable( collectionTableBinding.joinColumns() ) );
		if ( key.hasFormula() ) {
			return;
		}
		for ( Column column : key.getColumns() ) {
			column.setNullable( key.isNullable() );
		}
	}

	private List<Column> collectionTableTargetColumns(
			CollectionTableBinding collectionTableBinding,
			IdentifierBinding entityIdentifierBinding) {
		if ( referencesJoinedSubclassKey(
				collectionTableBinding.collection().getOwner(),
				collectionTableBinding.joinColumns()
		) ) {
			return ( (JoinedSubclass) collectionTableBinding.collection().getOwner() ).getKey().getColumns();
		}
		return targetIdentifierColumns( entityIdentifierBinding );
	}

	private String implicitCollectionKeyColumnName(CollectionTableBinding collectionTableBinding, Column referencedColumn) {
		final AttributePath attributePath = collectionKeyAttributePath( collectionTableBinding );
		return bindingState.getMetadataBuildingContext()
				.getBuildingPlan()
				.getImplicitNamingStrategy()
				.determineJoinColumnName( new ImplicitJoinColumnNameSource() {
					@Override
					public Nature getNature() {
						return collectionTableBinding.collection().getElement() instanceof ManyToOne
								|| collectionTableBinding.collection().getElement() instanceof org.hibernate.mapping.OneToMany
								? Nature.ENTITY_COLLECTION
								: Nature.ELEMENT_COLLECTION;
					}

					@Override
					public EntityNaming getEntityNaming() {
						return new EntityNaming() {
							@Override
							public String getClassName() {
								return collectionTableBinding.collection().getOwner().getClassName();
							}

							@Override
							public String getEntityName() {
								return collectionTableBinding.collection().getOwner().getEntityName();
							}

							@Override
							public String getJpaEntityName() {
								return collectionTableBinding.collection().getOwner().getJpaEntityName();
							}
						};
					}

					@Override
					public AttributePath getAttributePath() {
						return attributePath;
					}

					@Override
					public Identifier getReferencedTableName() {
						return collectionTableBinding.collection().getOwner().getTable().getNameIdentifier();
					}

					@Override
					public Identifier getReferencedColumnName() {
						return referencedColumn.getNameIdentifier( bindingState.getDatabase() );
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				} )
				.getText();
	}

	private AttributePath collectionKeyAttributePath(CollectionTableBinding collectionTableBinding) {
		if ( collectionTableBinding.collection().getElement() instanceof org.hibernate.mapping.OneToMany ) {
			return AttributePath.parse( collectionRolePath( collectionTableBinding ) );
		}
		return inverseManyToManyAttributePath( collectionTableBinding );
	}

	private AttributePath inverseManyToManyAttributePath(CollectionTableBinding collectionTableBinding) {
		if ( !( collectionTableBinding.collection().getElement() instanceof ManyToOne ) ) {
			return null;
		}

		final String ownerClassName = collectionTableBinding.collection().getOwner().getClassName();
		final String collectionRolePath = collectionRolePath( collectionTableBinding );
		final AtomicReference<AttributePath> result = new AtomicReference<>();
		bindingState.forEachInversePluralAssociationBinding( (inverseBinding) -> {
			if ( result.get() == null
					&& inverseBinding.nature() == InversePluralAssociationBinding.Nature.MANY_TO_MANY
					&& ownerClassName.equals( inverseBinding.targetClassDetails().getClassName() )
					&& collectionRolePath.equals( inverseBinding.mappedBy() ) ) {
				result.set( AttributePath.parse( inverseBinding.attributeMetadata().getName() ) );
			}
		} );
		return result.get();
	}

	private String collectionRolePath(CollectionTableBinding collectionTableBinding) {
		final String role = collectionTableBinding.collection().getRole();
		final String ownerEntityPrefix = collectionTableBinding.collection().getOwner().getEntityName() + ".";
		if ( role.startsWith( ownerEntityPrefix ) ) {
			return role.substring( ownerEntityPrefix.length() );
		}
		return collectionAttributeName( collectionTableBinding );
	}

	private String collectionAttributeName(CollectionTableBinding collectionTableBinding) {
		final String role = collectionTableBinding.collection().getRole();
		final int separatorIndex = role.lastIndexOf( '.' );
		return separatorIndex < 0 ? role : role.substring( separatorIndex + 1 );
	}

	private boolean collectionKeyNullable(List<JoinColumn> joinColumns) {
		if ( joinColumns.isEmpty() ) {
			return false;
		}
		for ( JoinColumn joinColumn : joinColumns ) {
			if ( !joinColumn.nullable() ) {
				return false;
			}
		}
		return true;
	}

	private boolean collectionKeyUpdatable(List<JoinColumn> joinColumns) {
		for ( JoinColumn joinColumn : joinColumns ) {
			return joinColumn.updatable();
		}
		return true;
	}

	private DependantValue createDependentKeyValue(
			Table table,
			ReferencedOwnerKey referencedOwnerKey,
			List<JoinColumn> joinColumns,
			String sourceRole,
			boolean collectionKey) {
		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				table,
				referencedOwnerKey.value()
		);
		if ( referencedOwnerKey.value() instanceof SimpleValue simpleValue ) {
			key.copyTypeFrom( simpleValue );
		}
		key.setNullable( collectionKey ? collectionKeyNullable( joinColumns ) : false );
		key.setUpdateable( collectionKey && collectionKeyUpdatable( joinColumns ) );

		final var orderedJoinColumns = ToOneAttributeBinder.orderJoinColumns(
				joinColumns,
				referencedOwnerKey.targetColumns(),
				bindingState.getDatabase(),
				entityBinder.getManagedType().getClassDetails().getClassName(),
				sourceRole
		);
		for ( int i = 0; i < referencedOwnerKey.targetColumns().size(); i++ ) {
			final JoinColumn joinColumn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final Column targetColumn = referencedOwnerKey.targetColumns().get( i );
			final Column keyColumn = orderedJoinColumns.isEmpty()
					? bindKeyColumn( table, targetColumn, null, key.isNullable() )
					: bindKeyColumn( table, targetColumn, joinColumn, key.isNullable() );
			table.addColumn( keyColumn );
			key.addColumn(
					keyColumn,
					joinColumn == null || joinColumn.insertable(),
					joinColumn == null || joinColumn.updatable()
			);
		}
		key.setSorted( true );
		return key;
	}

	private ReferencedOwnerKey resolveReferencedOwnerKey(
			PersistentClass ownerBinding,
			List<JoinColumn> joinColumns,
			String sourceRole) {
		final List<Identifier> referencedColumnNames = referencedColumnNames( joinColumns );
		if ( referencedColumnNames.isEmpty()
				|| columnNamesReferenceSameColumns( ownerBinding.getIdentifier().getColumns(), referencedColumnNames ) ) {
			return null;
		}
		if ( referencesJoinedSubclassKey( ownerBinding, joinColumns ) ) {
			return null;
		}
		for ( Property property : referenceableProperties( ownerBinding ) ) {
			if ( property.getValue() instanceof SimpleValue simpleValue
					&& !( simpleValue instanceof ToOne )
					&& columnNamesMatch( simpleValue.getColumns(), referencedColumnNames ) ) {
				if ( simpleValue instanceof BasicValue basicValue ) {
					requireResolution( basicValue );
				}
				materializeUniqueKey( simpleValue, sourceRole );
				return new ReferencedOwnerKey( property, simpleValue, simpleValue.getColumns() );
			}
		}

		final List<Property> properties = resolveReferencedProperties( ownerBinding, referencedColumnNames, sourceRole );
		if ( !properties.isEmpty() ) {
			final Property property = createSyntheticProperty(
					ownerBinding,
					properties,
					syntheticPropertyName( ownerBinding, sourceRole ),
					sourceRole
			);
			return new ReferencedOwnerKey( property, (KeyValue) property.getValue(), property.getValue().getColumns() );
		}
		throw new MappingException(
				"Could not resolve non-primary-key table key columns " + referencedColumnNames + " - " + sourceRole
		);
	}

	private boolean referencesJoinedSubclassKey(PersistentClass ownerBinding, List<JoinColumn> joinColumns) {
		final List<Identifier> referencedColumnNames = referencedColumnNames( joinColumns );
		return !referencedColumnNames.isEmpty()
				&& ownerBinding instanceof JoinedSubclass joinedSubclass
				&& joinedSubclass.getKey() != null
				&& columnNamesReferenceSameColumns( joinedSubclass.getKey().getColumns(), referencedColumnNames );
	}

	private List<Property> referenceableProperties(PersistentClass ownerBinding) {
		final ArrayList<Property> properties = new ArrayList<>();
		if ( ownerBinding.getIdentifierProperty() != null ) {
			properties.add( ownerBinding.getIdentifierProperty() );
		}
		properties.addAll( ownerBinding.getReferenceableProperties() );
		return properties;
	}

	private List<Property> resolveReferencedProperties(
			PersistentClass ownerBinding,
			List<Identifier> referencedColumnNames,
			String sourceRole) {
		final LinkedHashSet<Property> result = new LinkedHashSet<>();
		for ( Identifier referencedColumnName : referencedColumnNames ) {
			final Property property = findPropertyContainingColumn( ownerBinding, referencedColumnName, referencedColumnNames );
			if ( property == null ) {
				return List.of();
			}
			result.add( property );
		}
		final List<Property> properties = new ArrayList<>( result );
		if ( !columnNamesMatch( collectColumns( properties ), referencedColumnNames ) ) {
			throw new MappingException(
					"Referenced table key columns span properties that do not match the requested order "
							+ referencedColumnNames + " - " + sourceRole
			);
		}
		return properties;
	}

	private Property findPropertyContainingColumn(
			PersistentClass ownerBinding,
			Identifier referencedColumnName,
			List<Identifier> referencedColumnNames) {
		for ( Property property : referenceableProperties( ownerBinding ) ) {
			final Property match = findPropertyContainingColumn( property, referencedColumnName, referencedColumnNames );
			if ( match != null ) {
				return match;
			}
		}
		return null;
	}

	private Property findPropertyContainingColumn(
			Property property,
			Identifier referencedColumnName,
			List<Identifier> referencedColumnNames) {
		if ( property.getValue() instanceof ToOne toOne
				&& columnNamesMatch( toOne.getColumns(), referencedColumnNames ) ) {
			return null;
		}
		if ( property.getValue() instanceof Component component ) {
			for ( Property subProperty : component.getProperties() ) {
				final Property match = findPropertyContainingColumn( subProperty, referencedColumnName, referencedColumnNames );
				if ( match != null ) {
					return match;
				}
			}
		}
		else if ( containsColumn( property.getValue(), referencedColumnName ) ) {
			return property;
		}
		return null;
	}

	private boolean containsColumn(Value value, Identifier referencedColumnName) {
		final Database database = bindingState.getDatabase();
		for ( Column column : value.getColumns() ) {
			if ( column.getNameIdentifier( database ).matches( referencedColumnName ) ) {
				return true;
			}
		}
		return false;
	}

	private Property createSyntheticProperty(
			PersistentClass ownerBinding,
			List<Property> properties,
			String syntheticPropertyName,
			String sourceRole) {
		final Component component = new Component( metadataBuildingContext(), ownerBinding );
		component.setComponentClassName( ownerBinding.getClassName() );
		component.setEmbedded( true );
		component.setPreservePropertyOrder( true );
		for ( Property property : properties ) {
			component.addProperty( cloneProperty( ownerBinding, property ) );
		}

		final SyntheticProperty syntheticProperty = new SyntheticProperty();
		syntheticProperty.setName( syntheticPropertyName );
		syntheticProperty.setPersistentClass( ownerBinding );
		syntheticProperty.setUpdatable( false );
		syntheticProperty.setInsertable( false );
		syntheticProperty.setValue( component );
		syntheticProperty.setPropertyAccessorName( EMBEDDED.getExternalName() );
		ownerBinding.addProperty( syntheticProperty );
		materializeUniqueKey( component, sourceRole );
		return syntheticProperty;
	}

	private Property cloneProperty(PersistentClass ownerBinding, Property property) {
		if ( property.isComposite() ) {
			final Component component = (Component) property.getValue();
			final Component copy = new Component( metadataBuildingContext(), component );
			copy.setComponentClassName( component.getComponentClassName() );
			copy.setEmbedded( component.isEmbedded() );
			for ( Property subProperty : component.getProperties() ) {
				copy.addProperty( cloneProperty( ownerBinding, subProperty ) );
			}
			copy.sortProperties();
			final SyntheticProperty result = new SyntheticProperty();
			result.setName( property.getName() );
			result.setPersistentClass( ownerBinding );
			result.setUpdatable( false );
			result.setInsertable( false );
			result.setValue( copy );
			result.setPropertyAccessorName( property.getPropertyAccessorName() );
			return result;
		}
		final SyntheticProperty clone = property.syntheticCopy();
		clone.setNaturalIdentifier( false );
		clone.setInsertable( false );
		clone.setUpdatable( false );
		return clone;
	}

	private String syntheticPropertyName(PersistentClass ownerBinding, String sourceRole) {
		return ( "_" + ownerBinding.getEntityName() + "_" + sourceRole )
				.replace( '.', '_' );
	}

	private List<Column> collectColumns(List<Property> properties) {
		final ArrayList<Column> columns = new ArrayList<>();
		for ( Property property : properties ) {
			columns.addAll( property.getValue().getColumns() );
		}
		return columns;
	}

	private void materializeUniqueKey(SimpleValue value, String sourceRole) {
		uniqueKeyMappingMaterializer.materializeUniqueKey(
				ResolvedUniqueKey.from( value, metadataBuildingContext(), sourceRole )
		);
	}

	private static BasicValue.Resolution<?> requireResolution(BasicValue basicValue) {
		final BasicValue.Resolution<?> resolution = basicValue.getResolution();
		if ( resolution == null ) {
			throw new IllegalStateException( "BasicValue resolution has not been applied: " + basicValue );
		}
		return resolution;
	}

	private MetadataBuildingContext metadataBuildingContext() {
		return bindingState.getMetadataBuildingContext();
	}

	private List<Identifier> referencedColumnNames(List<JoinColumn> joinColumns) {
		final ArrayList<Identifier> result = new ArrayList<>();
		final Database database = bindingState.getDatabase();
		for ( JoinColumn joinColumn : joinColumns ) {
			if ( StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) ) {
				result.add( database.toIdentifier( joinColumn.referencedColumnName() ) );
			}
		}
		return result;
	}

	private boolean columnNamesMatch(List<Column> columns, List<Identifier> referencedColumnNames) {
		if ( columns.size() != referencedColumnNames.size() ) {
			return false;
		}
		final Database database = bindingState.getDatabase();
		for ( int i = 0; i < columns.size(); i++ ) {
			if ( !columns.get( i ).getNameIdentifier( database ).matches( referencedColumnNames.get( i ) ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean columnNamesReferenceSameColumns(List<Column> columns, List<Identifier> referencedColumnNames) {
		if ( columns.size() != referencedColumnNames.size() ) {
			return false;
		}
		final Database database = bindingState.getDatabase();
		final boolean[] matchedReferencedColumns = new boolean[referencedColumnNames.size()];
		for ( Column column : columns ) {
			boolean matched = false;
			for ( int i = 0; i < referencedColumnNames.size(); i++ ) {
				if ( !matchedReferencedColumns[i]
						&& column.getNameIdentifier( database ).matches( referencedColumnNames.get( i ) ) ) {
					matchedReferencedColumns[i] = true;
					matched = true;
					break;
				}
			}
			if ( !matched ) {
				return false;
			}
		}
		return true;
	}

	private record ReferencedOwnerKey(Property property, KeyValue value, List<Column> targetColumns) {
	}

	private Column copyKeyColumn(Column source, boolean copyUnique) {
		// todo : is this enough detail?
		final Column result = new Column( source.getName() );
		result.setLength( source.getLength() );
		result.setPrecision( source.getPrecision() );
		result.setScale( source.getScale() );
		result.setSqlType( source.getSqlType() );
		result.setNullable( false );
		result.setUnique( copyUnique && source.isUnique() );
		return result;
	}

	private Column bindKeyColumn(Table table, Column identifierColumn, jakarta.persistence.JoinColumn joinColumn) {
		return bindKeyColumn( table, identifierColumn, joinColumn, identifierColumn::getName, false );
	}

	private Column bindKeyColumn(
			Table table,
			Column identifierColumn,
			jakarta.persistence.JoinColumn joinColumn,
			boolean nullableByDefault) {
		return bindKeyColumn( table, identifierColumn, joinColumn, identifierColumn::getName, nullableByDefault );
	}

	private Column bindKeyColumn(
			Table table,
			Column identifierColumn,
			jakarta.persistence.JoinColumn joinColumn,
			java.util.function.Supplier<String> implicitName) {
		return bindKeyColumn( table, identifierColumn, joinColumn, implicitName, false );
	}

	private Column bindKeyColumn(
			Table table,
			Column identifierColumn,
			jakarta.persistence.JoinColumn joinColumn,
			java.util.function.Supplier<String> implicitName,
			boolean nullableByDefault) {
		final Column result = ColumnBinder.bindColumn(
				ColumnSource.from( joinColumn ),
				implicitName,
				false,
				nullableByDefault
		);
		final boolean nullable = result.isNullable();
		final String options = result.getOptions();
		result.copy( identifierColumn );
		result.setNullable( nullable );
		result.setOptions( options );
		table.addColumn( result );
		final Column tableColumn = table.getColumn( result );
		if ( tableColumn != null && tableColumn != result ) {
			final boolean tableColumnNullable = tableColumn.isNullable();
			tableColumn.copy( identifierColumn );
			tableColumn.setNullable( tableColumnNullable );
			tableColumn.setOptions( options );
			return tableColumn;
		}
		return result;
	}

}
