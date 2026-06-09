/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.mapping.internal.model.IdentifierAttributeBinding;
import org.hibernate.boot.mapping.internal.materialize.PrimaryTableKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.ResolvedForeignKey;
import org.hibernate.boot.mapping.internal.view.EntityIdentifierBindingView;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingOptionsImpl;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ForeignKeyColumnMappings;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

import jakarta.persistence.JoinColumn;

/// Resolves association-valued identifier attributes after all identifiers exist.
///
/// This phase completes the deferred pieces of `IdClass` identifiers that include
/// owning to-one attributes.  It copies target identifier column order into the
/// owner table, adds those columns to the primary key, reorders composite primary
/// keys to match the identifier component, and records the association foreign
/// key for the later foreign-key phase.
///
/// @since 9.0
/// @author Steve Ebersole
class AssociationIdentifierBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final PrimaryTableKeyMappingMaterializer primaryTableKeyMappingMaterializer;
	private final boolean strict;

	AssociationIdentifierBinder(EntityTypeBinder entityBinder, boolean strict) {
		this.entityBinder = entityBinder;
		this.strict = strict;
		this.bindingState = entityBinder.getBindingState();
		this.bindingOptions = new BindingOptionsImpl( bindingState.getMetadataBuildingContext() );
		this.primaryTableKeyMappingMaterializer = new PrimaryTableKeyMappingMaterializer(
				bindingState.getMetadataBuildingContext()
		);
	}

	boolean bindAssociationIdentifiers() {
		final boolean[] processedAny = new boolean[1];
		bindingState.forEachAssociationIdentifierBinding( (associationIdentifierBinding) -> {
			if ( associationIdentifierBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				if ( bindAssociationIdentifier( associationIdentifierBinding ) ) {
					processedAny[0] = true;
				}
			}
		} );
		return processedAny[0];
	}

	private boolean bindAssociationIdentifier(AssociationIdentifierBinding associationIdentifierBinding) {
		if ( associationIdentifierBinding.processed().get() ) {
			return false;
		}

		final IdentifierBinding targetIdentifierBinding = bindingState.getIdentifierBinding(
				associationIdentifierBinding.targetTypeBinder().getManagedType().getHierarchy().getRoot()
		);
		if ( targetIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for association identifier target - "
							+ associationIdentifierBinding.value().getReferencedEntityName()
			);
		}
		if ( hasUnprocessedAssociationIdentifierBindings(
				targetIdentifierBinding.rootClass(),
				associationIdentifierBinding
			) ) {
			return false;
		}

		final List<JoinColumn> joinColumns = associationIdentifierBinding.joinColumns();
		final TargetColumns targetColumns = resolveTargetColumns(
				associationIdentifierBinding,
				targetIdentifierBinding,
				sortedTargetIdentifierColumns( targetIdentifierBinding )
		);
		if ( targetColumns.columns().isEmpty() ) {
			return false;
		}
		if ( !joinColumns.isEmpty() && joinColumns.size() != targetColumns.columns().size() ) {
			if ( !strict ) {
				return false;
			}
			throw new MappingException(
					"Association identifier join column count did not match target identifier column count - "
							+ associationIdentifierBinding.ownerBinding().getEntityName()
							+ "." + associationIdentifierBinding.property().getName()
			);
		}
		if ( !targetColumns.referenceToPrimaryKey() ) {
			associationIdentifierBinding.value().setReferenceToPrimaryKey( false );
			if ( !( associationIdentifierBinding.value() instanceof ManyToOne manyToOne ) ) {
				throw new MappingException(
						"Non-primary-key embedded-id association identifiers require an owning many-to-one - "
								+ associationIdentifierBinding.ownerBinding().getClassName()
								+ "." + associationIdentifierBinding.property().getName()
				);
			}
			bindingState.addAssociationTargetBinding( new AssociationTargetBinding(
					associationIdentifierBinding.ownerBinding(),
					manyToOne,
					associationIdentifierBinding.targetTypeBinder(),
					targetColumns.referencedColumnNames(),
					associationIdentifierBinding.ownerBinding().getClassName()
							+ "." + associationIdentifierBinding.property().getName()
			) );
		}
		final List<JoinColumn> orderedJoinColumns = orderJoinColumns(
				joinColumns,
				targetColumns.columns(),
				bindingState.getDatabase(),
				associationIdentifierBinding.ownerBinding().getClassName(),
				associationIdentifierBinding.property().getName()
		);
		final int columnCount = targetColumns.columns().size();
		for ( int i = 0; i < columnCount; i++ ) {
			final JoinColumn joinColumn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final String targetColumnName = targetColumns.columns().get( i ).getName();
			final Column column = ColumnBinder.bindColumn(
					org.hibernate.boot.mapping.internal.sources.ColumnSource.from( joinColumn ),
					() -> associationIdentifierBinding.property().getName() + "_" + targetColumnName,
					false,
					false,
					bindingOptions,
					bindingState
			);
			associationIdentifierBinding.ownerBinding().getTable().addColumn( column );
				associationIdentifierBinding.value().addColumn( column, true, false );
				if ( associationIdentifierBinding.identifierMapperValue().get() != null ) {
					addIdentifierColumn( associationIdentifierBinding.identifierMapperValue().get(), i, column, false );
				}
				addIdentifierColumn( associationIdentifierBinding.identifierValue(), i, column, true );
				primaryTableKeyMappingMaterializer.addIdentifierColumn(
						primaryTableKeyMappingMaterializer.resolvePrimaryKey(
								associationIdentifierBinding.ownerBinding(),
								associationIdentifierBinding.ownerBinding().getTable()
						),
						column
			);
			associationIdentifierBinding.identifierColumns().add( column );
		}
		final SelectableOrderResolution selectableOrderResolution = SelectableOrderResolver.resolveByTargetOrder(
				associationIdentifierBinding.value().getColumns(),
				targetColumns.columns(),
				associationIdentifierBinding.ownerBinding().getClassName()
						+ "." + associationIdentifierBinding.property().getName()
		);
		final ForeignKeyColumnMappings foreignKeyColumnMappings = selectableOrderResolution.foreignKeyColumnMappings();
		associationIdentifierBinding.value().setForeignKeyColumnMappings( foreignKeyColumnMappings );
		applyForeignKeyColumnMappings( associationIdentifierBinding.identifierValue(), foreignKeyColumnMappings );
		applyForeignKeyColumnMappings( associationIdentifierBinding.identifierMapperValue().get(), foreignKeyColumnMappings );
		syncEntityIdentifierBindingSelectables( associationIdentifierBinding );
		normalizePrimaryKeyColumnOrder( associationIdentifierBinding );
		associationIdentifierBinding.value().setNonUpdatable();
		associationIdentifierBinding.value().setSorted( true );
		( (SortableValue) associationIdentifierBinding.value() ).sortProperties();
		associationIdentifierBinding.processed().set( true );
		bindingState.addForeignKeyBinding( new ForeignKeyBinding(
				associationIdentifierBinding.ownerBinding(),
				associationIdentifierBinding.value(),
				associationIdentifierBinding.foreignKeySource(),
				targetColumns.referenceToPrimaryKey()
						? ResolvedForeignKey.from(
										associationIdentifierBinding.value(),
										associationIdentifierBinding.value().getReferencedEntityName(),
								selectableOrderResolution
						)
							: null
			) );
		return true;
	}

	private void applyForeignKeyColumnMappings(Value value, ForeignKeyColumnMappings foreignKeyColumnMappings) {
		if ( value instanceof ToOne toOne ) {
			if ( matchesLocalColumns( toOne, foreignKeyColumnMappings ) ) {
				toOne.setForeignKeyColumnMappings( foreignKeyColumnMappings );
			}
		}
		else if ( value instanceof Component component ) {
			for ( var property : component.getProperties() ) {
				applyForeignKeyColumnMappings( property.getValue(), foreignKeyColumnMappings );
			}
		}
	}

	private boolean matchesLocalColumns(ToOne toOne, ForeignKeyColumnMappings foreignKeyColumnMappings) {
		if ( toOne.getColumns().size() != foreignKeyColumnMappings.mappings().size() ) {
			return false;
		}
		for ( var mapping : foreignKeyColumnMappings.mappings() ) {
			if ( !toOne.getColumns().contains( mapping.column() ) ) {
				return false;
			}
		}
		return true;
	}

	private void syncEntityIdentifierBindingSelectables(AssociationIdentifierBinding associationIdentifierBinding) {
		final var entityIdentifierBinding = bindingState.getEntityIdentifierBinding(
				associationIdentifierBinding.ownerType().getHierarchy().getRoot()
		);
		if ( entityIdentifierBinding == null ) {
			return;
		}

		final IdentifierAttributeBinding attribute = entityIdentifierBinding.getAttribute(
				associationIdentifierBinding.property().getName()
		);
		if ( attribute == null || !attribute.selectableNames().isEmpty() ) {
			return;
		}

		for ( Column column : associationIdentifierBinding.value().getColumns() ) {
			attribute.addSelectableName( column.getName() );
		}
	}

	private void normalizePrimaryKeyColumnOrder(AssociationIdentifierBinding associationIdentifierBinding) {
		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding(
				associationIdentifierBinding.ownerType().getHierarchy().getRoot()
		);
		if ( identifierBinding == null || identifierBinding.columns().isEmpty() ) {
			return;
		}

		final EntityIdentifierBindingView entityIdentifierBinding = bindingState.getEntityIdentifierBindingView(
				associationIdentifierBinding.ownerType().getHierarchy().getRoot()
		);
		final List<Column> identifierColumns = entityIdentifierBinding == null
				? identifierBinding.columns()
				: orderColumnsBySelectableNames(
						identifierBinding.columns(),
						entityIdentifierBinding.identifierSelectableNames()
				);
		if ( associationIdentifierBinding.ownerBinding().getTable().getPrimaryKey().getOriginalOrder() == null ) {
			associationIdentifierBinding.ownerBinding().getTable().getPrimaryKey()
					.reorderColumns( identifierColumns );
		}
		final ArrayList<Column> orderedIdentifierColumns = new ArrayList<>( identifierColumns );
		associationIdentifierBinding.identifierColumns().clear();
		associationIdentifierBinding.identifierColumns().addAll( orderedIdentifierColumns );
	}

	private List<Column> orderColumnsBySelectableNames(List<Column> columns, List<String> selectableNames) {
		if ( selectableNames.isEmpty() || columns.size() != selectableNames.size() ) {
			return columns;
		}
		final ArrayList<Column> orderedColumns = new ArrayList<>( columns.size() );
		for ( String selectableName : selectableNames ) {
			final Column column = findColumn( columns, selectableName );
			if ( column == null ) {
				return columns;
			}
			orderedColumns.add( column );
		}
		return orderedColumns;
	}

	private Column findColumn(List<Column> columns, String selectableName) {
		for ( Column column : columns ) {
			if ( column.getName().equals( selectableName ) ) {
				return column;
			}
		}
		return null;
	}

	private boolean hasUnprocessedAssociationIdentifierBindings(
			org.hibernate.mapping.PersistentClass ownerBinding,
			AssociationIdentifierBinding currentBinding) {
		final boolean[] result = new boolean[1];
		bindingState.forEachAssociationIdentifierBinding( (associationIdentifierBinding) -> {
			if ( associationIdentifierBinding != currentBinding
					&& associationIdentifierBinding.ownerBinding() == ownerBinding
					&& !associationIdentifierBinding.processed().get() ) {
				result[0] = true;
			}
		} );
		return result[0];
	}

	private void addIdentifierColumn(Value identifierValue, int columnIndex, Column column, boolean writable) {
		if ( identifierValue == null ) {
			return;
		}
		if ( identifierValue instanceof Component component ) {
			final List<SimpleValue> leafValues = new ArrayList<>();
			collectLeafValues( component, leafValues );
			if ( columnIndex >= leafValues.size() ) {
				throw new MappingException(
						"Association identifier column count exceeded IdClass component property count - "
								+ component.getComponentClassName()
				);
			}
			leafValues.get( columnIndex ).addColumn( column, writable, writable );
		}
		else if ( identifierValue instanceof SimpleValue simpleValue ) {
			simpleValue.addColumn( column, writable, writable );
		}
	}

	private void collectLeafValues(Component component, List<SimpleValue> leafValues) {
		for ( var property : component.getProperties() ) {
			if ( property.getValue() instanceof SimpleValue simpleValue ) {
				leafValues.add( simpleValue );
			}
			else if ( property.getValue() instanceof Component nestedComponent ) {
				collectLeafValues( nestedComponent, leafValues );
			}
		}
	}

	private TargetColumns resolveTargetColumns(
			AssociationIdentifierBinding associationIdentifierBinding,
			IdentifierBinding targetIdentifierBinding,
			List<Column> targetIdentifierColumns) {
		final List<JoinColumn> joinColumns = associationIdentifierBinding.joinColumns();
		if ( joinColumns.isEmpty()
				|| joinColumns.stream().noneMatch( (joinColumn) -> StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) ) ) {
			return TargetColumns.primaryKey( targetIdentifierColumns );
		}

		if ( ToOneAttributeBinder.referencesPrimaryKey( joinColumns, targetIdentifierColumns, bindingState.getDatabase() ) ) {
			return TargetColumns.primaryKey( targetIdentifierColumns );
		}
		if ( referencesSomePrimaryKeyColumns( joinColumns, targetIdentifierColumns ) ) {
			if ( !strict ) {
				return TargetColumns.deferred();
			}
			throw new MappingException(
					"Association identifier join column count did not match target identifier column count - "
							+ associationIdentifierBinding.ownerBinding().getEntityName()
							+ "." + associationIdentifierBinding.property().getName()
			);
		}
		if ( associationIdentifierBinding.ownerBinding().hasEmbeddedIdentifier() ) {
			final List<String> referencedColumnNames = referencedColumnNames( joinColumns );
			final List<Column> targetColumns = new ArrayList<>( referencedColumnNames.size() );
			for ( String referencedColumnName : referencedColumnNames ) {
				targetColumns.add( new Column( referencedColumnName ) );
			}
			return TargetColumns.nonPrimaryKey( targetColumns, referencedColumnNames );
		}
		throw new MappingException(
				"Unable to match association identifier join column referencedColumnName to target identifier column - "
						+ associationIdentifierBinding.ownerBinding().getClassName()
						+ "." + associationIdentifierBinding.property().getName()
		);
	}

	private List<Column> sortedTargetIdentifierColumns(IdentifierBinding targetIdentifierBinding) {
		if ( targetIdentifierBinding.value() instanceof SortableValue sortableValue ) {
			sortableValue.sortProperties();
		}
		return targetIdentifierBinding.value().getColumns();
	}

	private boolean referencesSomePrimaryKeyColumns(List<JoinColumn> joinColumns, List<Column> targetIdentifierColumns) {
		for ( JoinColumn joinColumn : joinColumns ) {
			if ( StringHelper.isEmpty( joinColumn.referencedColumnName() ) ) {
				return false;
			}
			for ( Column targetIdentifierColumn : targetIdentifierColumns ) {
				if ( targetIdentifierColumn.getNameIdentifier( bindingState.getDatabase() )
						.matches( bindingState.getDatabase().toIdentifier( joinColumn.referencedColumnName() ) ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private List<String> referencedColumnNames(List<JoinColumn> joinColumns) {
		final ArrayList<String> result = new ArrayList<>( joinColumns.size() );
		for ( JoinColumn joinColumn : joinColumns ) {
			if ( StringHelper.isEmpty( joinColumn.referencedColumnName() ) ) {
				throw new MappingException( "Association identifier join columns must all name referenced columns" );
			}
			result.add( joinColumn.referencedColumnName() );
		}
		return result;
	}

	private record TargetColumns(List<Column> columns, boolean referenceToPrimaryKey, List<String> referencedColumnNames) {
		private static TargetColumns deferred() {
			return new TargetColumns( List.of(), true, List.of() );
		}

		private static TargetColumns primaryKey(List<Column> columns) {
			return new TargetColumns( columns, true, List.of() );
		}

		private static TargetColumns nonPrimaryKey(List<Column> columns, List<String> referencedColumnNames) {
			return new TargetColumns( columns, false, referencedColumnNames );
		}
	}

	private static List<JoinColumn> orderJoinColumns(
			List<JoinColumn> joinColumns,
			List<Column> targetColumns,
			Database database,
			String ownerClassName,
			String propertyName) {
		if ( joinColumns.isEmpty() || joinColumns.stream().noneMatch( (joinColumn) -> StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) ) ) {
			return joinColumns;
		}

		final ArrayList<JoinColumn> orderedJoinColumns = new ArrayList<>( targetColumns.size() );
		final ArrayList<JoinColumn> unmatchedJoinColumns = new ArrayList<>( joinColumns );
		for ( Column targetColumn : targetColumns ) {
			final JoinColumn joinColumn = findJoinColumn(
					targetColumn,
					unmatchedJoinColumns,
					database,
					ownerClassName,
					propertyName
			);
			orderedJoinColumns.add( joinColumn );
			unmatchedJoinColumns.remove( joinColumn );
		}
		return orderedJoinColumns;
	}

	private static JoinColumn findJoinColumn(
			Column targetColumn,
			List<JoinColumn> joinColumns,
			Database database,
			String ownerClassName,
			String propertyName) {
		for ( JoinColumn joinColumn : joinColumns ) {
			if ( targetColumn.getNameIdentifier( database ).matches( database.toIdentifier( joinColumn.referencedColumnName() ) ) ) {
				return joinColumn;
			}
		}

		throw new MappingException(
				"Unable to match association identifier join column referencedColumnName to target identifier column `"
						+ targetColumn.getName() + "` - " + ownerClassName + "." + propertyName
		);
	}
}
