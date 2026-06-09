/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.List;
import java.util.Properties;
import java.util.Comparator;

import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.materialize.ResolvedForeignKey;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

import jakarta.persistence.JoinColumn;

/// Resolves derived identifier to-one columns after identifiers and members exist.
///
/// `@MapsId` is order-sensitive: the association member can be encountered before
/// the owner identifier component and target identifier columns are ready.  This
/// phase resolves the named or implicit identifier part, validates explicit join
/// columns against that identifier part, and makes the association reuse the
/// identifier columns as non-insertable and non-updatable columns.
///
/// @since 9.0
/// @author Steve Ebersole
class DerivedIdentifierBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;

	DerivedIdentifierBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
	}

	void bindDerivedIdentifiers() {
		bindDerivedIdentifiers( new java.util.HashSet<>() );
	}

	private void bindDerivedIdentifiers(java.util.Set<org.hibernate.mapping.PersistentClass> visiting) {
		bindingState.forEachDerivedIdentifierBinding( (derivedIdentifierBinding) -> {
			if ( derivedIdentifierBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindDerivedIdentifier( derivedIdentifierBinding, visiting );
			}
		} );
	}

	private void bindDerivedIdentifier(
			DerivedIdentifierBinding derivedIdentifierBinding,
			java.util.Set<org.hibernate.mapping.PersistentClass> visiting) {
		if ( !visiting.add( derivedIdentifierBinding.ownerBinding() ) ) {
			return;
		}
		try {
			bindTargetDerivedIdentifiers( derivedIdentifierBinding, visiting );
			bindDerivedIdentifier( derivedIdentifierBinding );
		}
		finally {
			visiting.remove( derivedIdentifierBinding.ownerBinding() );
		}
	}

	private void bindTargetDerivedIdentifiers(
			DerivedIdentifierBinding derivedIdentifierBinding,
			java.util.Set<org.hibernate.mapping.PersistentClass> visiting) {
		if ( derivedIdentifierBinding.targetTypeBinder().getTypeBinding() != derivedIdentifierBinding.ownerBinding() ) {
			new DerivedIdentifierBinder( derivedIdentifierBinding.targetTypeBinder() ).bindDerivedIdentifiers( visiting );
		}
	}

	private void bindDerivedIdentifier(DerivedIdentifierBinding derivedIdentifierBinding) {
		final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding(
				derivedIdentifierBinding.ownerType().getHierarchy().getRoot()
		);
		if ( entityIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for derived identifier owner - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
			);
		}
		final Value identifierValue = resolveIdentifierValue( derivedIdentifierBinding, entityIdentifierBinding.value() );
		if ( !( identifierValue instanceof BasicValue
				|| identifierValue instanceof Component
				|| identifierValue instanceof ManyToOne ) ) {
			throw new UnsupportedOperationException(
					"@MapsId is only implemented for basic, component, or to-one identifier attributes - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
							+ "." + derivedIdentifierBinding.property().getName()
			);
		}

		final List<Column> identifierColumns = sortedColumns( identifierValue, identifierValue.getColumns() );
		final List<Column> targetColumns = resolveTargetIdentifierColumns( derivedIdentifierBinding );
		if ( identifierColumns.size() != targetColumns.size() ) {
			throw new MappingException(
					"@MapsId identifier attribute column count did not match target identifier column count - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
							+ "." + derivedIdentifierBinding.property().getName()
					);
		}

		final List<Column> orderedIdentifierColumns = orderIdentifierColumns( identifierColumns, targetColumns );
		validateJoinColumns( derivedIdentifierBinding, orderedIdentifierColumns );
		if ( derivedIdentifierBinding.referenceToPrimaryKey() ) {
			applyIdentifierColumnOverrides( derivedIdentifierBinding, identifierValue, orderedIdentifierColumns, targetColumns );
		}
		final List<Column> runtimeIdentifierColumns = orderLocalColumnsByTargetOrder(
				orderedIdentifierColumns,
				targetColumns,
				resolveTargetRuntimeColumns( derivedIdentifierBinding, targetColumns )
		);
		applyDerivedIdentifierGenerator( derivedIdentifierBinding, identifierValue );
		reorderPrimaryKeyColumns( entityIdentifierBinding.value(), orderedIdentifierColumns );
		if ( derivedIdentifierBinding.referenceToPrimaryKey() ) {
			for ( Column identifierColumn : runtimeIdentifierColumns ) {
				derivedIdentifierBinding.value().addColumn( identifierColumn, false, false );
			}
			reorderAssociationColumns( derivedIdentifierBinding.value(), runtimeIdentifierColumns );
			derivedIdentifierBinding.value().setSorted( true );
		}
		derivedIdentifierBinding.property().setOptional( false );
		bindingState.addForeignKeyBinding( new ForeignKeyBinding(
				derivedIdentifierBinding.ownerBinding(),
				derivedIdentifierBinding.value(),
				derivedIdentifierBinding.foreignKeySource(),
				derivedIdentifierBinding.referenceToPrimaryKey()
						? ResolvedForeignKey.from(
								derivedIdentifierBinding.value(),
								derivedIdentifierBinding.value().getReferencedEntityName(),
								SelectableOrderResolver.resolveByTargetOrder(
										orderedIdentifierColumns,
										targetColumns,
										derivedIdentifierBinding.ownerBinding().getEntityName()
												+ "." + derivedIdentifierBinding.property().getName()
								)
						)
						: null,
				derivedIdentifierBinding.referenceToPrimaryKey()
						? List.of()
						: ToOneAttributeBinder.referencedColumnNames( derivedIdentifierBinding.joinColumns() )
		) );
	}

	private void reorderPrimaryKeyColumns(Value identifierValue, List<Column> derivedIdentifierColumns) {
		if ( identifierValue instanceof Component identifierComponent ) {
			if ( derivedIdentifierColumns.size() > identifierComponent.getColumnSpan() ) {
				return;
			}
			final var primaryKey = identifierComponent.getTable().getPrimaryKey();
			if ( primaryKey != null
					&& primaryKey.getOriginalOrder() == null
					&& primaryKey.getColumns().equals( identifierComponent.getColumns() )
					&& identifierComponent.getColumns().containsAll( derivedIdentifierColumns ) ) {
				final var reorderedColumns = new java.util.ArrayList<Column>( identifierComponent.getColumnSpan() );
				reorderedColumns.addAll( derivedIdentifierColumns );
				for ( Column column : identifierComponent.getColumns() ) {
					if ( !reorderedColumns.contains( column ) ) {
						reorderedColumns.add( column );
					}
				}
				primaryKey.reorderColumns( reorderedColumns );
			}
		}
	}

	private List<Column> sortedColumns(Value value, List<Column> fallbackColumns) {
		if ( value instanceof Component component ) {
			component.sortProperties();
			return component.getColumns();
		}
		return fallbackColumns;
	}

	private List<Column> orderIdentifierColumns(List<Column> identifierColumns, List<Column> targetColumns) {
		if ( identifierColumns.size() < 2 ) {
			return identifierColumns;
		}
		final java.util.ArrayList<Column> orderedColumns = new java.util.ArrayList<>( targetColumns.size() );
		final java.util.ArrayList<Column> unmatchedColumns = new java.util.ArrayList<>( identifierColumns );
		for ( Column targetColumn : targetColumns ) {
			final Column identifierColumn = findColumn( unmatchedColumns, targetColumn );
			if ( identifierColumn == null ) {
				return identifierColumns;
			}
			orderedColumns.add( identifierColumn );
			unmatchedColumns.remove( identifierColumn );
		}
		return orderedColumns;
	}

	private Column findColumn(List<Column> columns, Column targetColumn) {
		for ( Column column : columns ) {
			if ( column.getNameIdentifier( bindingState.getDatabase() )
					.matches( targetColumn.getNameIdentifier( bindingState.getDatabase() ) ) ) {
				return column;
			}
		}
		return null;
	}

	private List<Column> orderLocalColumnsByTargetOrder(
			List<Column> localColumns,
			List<Column> targetColumns,
			List<Column> runtimeTargetColumns) {
		if ( localColumns.size() < 2 || targetColumns == runtimeTargetColumns ) {
			return localColumns;
		}
		final java.util.ArrayList<Column> orderedColumns = new java.util.ArrayList<>( runtimeTargetColumns.size() );
		for ( Column runtimeTargetColumn : runtimeTargetColumns ) {
			final int targetPosition = findColumnPosition( targetColumns, runtimeTargetColumn );
			if ( targetPosition < 0 ) {
				return localColumns;
			}
			orderedColumns.add( localColumns.get( targetPosition ) );
		}
		return orderedColumns;
	}

	private int findColumnPosition(List<Column> columns, Column targetColumn) {
		for ( int i = 0; i < columns.size(); i++ ) {
			if ( columns.get( i ).getNameIdentifier( bindingState.getDatabase() )
					.matches( targetColumn.getNameIdentifier( bindingState.getDatabase() ) ) ) {
				return i;
			}
		}
		return -1;
	}

	private void reorderAssociationColumns(ToOne value, List<Column> runtimeIdentifierColumns) {
		final List<Column> currentColumns = value.getColumns();
		if ( currentColumns.size() < 2 || currentColumns.equals( runtimeIdentifierColumns ) ) {
			return;
		}
		final int[] targetPositions = new int[currentColumns.size()];
		for ( int i = 0; i < currentColumns.size(); i++ ) {
			final int targetPosition = findColumnPosition( runtimeIdentifierColumns, currentColumns.get( i ) );
			if ( targetPosition < 0 ) {
				return;
			}
			targetPositions[i] = targetPosition;
		}
		value.sortColumns( targetPositions );
	}

	private void applyIdentifierColumnOverrides(
			DerivedIdentifierBinding derivedIdentifierBinding,
			Value identifierValue,
			List<Column> identifierColumns,
			List<Column> targetColumns) {
		if ( !( identifierValue instanceof BasicValue || identifierValue instanceof Component ) ) {
			return;
		}
		final List<JoinColumn> joinColumns = derivedIdentifierBinding.joinColumns();
		if ( joinColumns.isEmpty() ) {
			applyImplicitIdentifierColumnNames(
					derivedIdentifierBinding,
					identifierColumns,
					targetColumns,
					identifierValue instanceof Component
			);
			return;
		}
		final List<JoinColumn> orderedJoinColumns = ToOneAttributeBinder.orderJoinColumns(
				joinColumns,
				targetColumns,
				bindingState.getDatabase(),
				derivedIdentifierBinding.ownerBinding().getClassName(),
				derivedIdentifierBinding.property().getName()
		);
		for ( int i = 0; i < orderedJoinColumns.size(); i++ ) {
			final JoinColumn joinColumn = orderedJoinColumns.get( i );
			copyReferencedColumnMetadata( identifierColumns.get( i ), targetColumns.get( i ) );
			renameIdentifierColumn(
					identifierColumns.get( i ),
					StringHelper.isNotEmpty( joinColumn.name() )
							? joinColumn.name()
							: derivedIdentifierBinding.property().getName() + "_" + targetColumns.get( i ).getName()
			);
		}
	}

	private static void copyReferencedColumnMetadata(Column identifierColumn, Column targetColumn) {
		final boolean nullable = identifierColumn.isNullable();
		identifierColumn.copy( targetColumn );
		identifierColumn.setNullable( nullable );
	}

	private void applyImplicitIdentifierColumnNames(
			DerivedIdentifierBinding derivedIdentifierBinding,
			List<Column> identifierColumns,
			List<Column> targetColumns,
			boolean forcePrefix) {
		for ( int i = 0; i < identifierColumns.size(); i++ ) {
			if ( !forcePrefix
					&& identifierColumns.get( i ).getNameIdentifier( bindingState.getDatabase() )
					.matches( targetColumns.get( i ).getNameIdentifier( bindingState.getDatabase() ) ) ) {
				continue;
			}
			renameIdentifierColumn(
					identifierColumns.get( i ),
					derivedIdentifierBinding.property().getName() + "_" + targetColumns.get( i ).getName()
			);
		}
	}

	private void renameIdentifierColumn(Column identifierColumn, String name) {
		if ( identifierColumn.getName().equals( name ) ) {
			return;
		}
		identifierColumn.setName( name );
		if ( identifierColumn.getValue() instanceof SimpleValue simpleValue ) {
			simpleValue.getTable().columnRenamed( identifierColumn );
		}
	}

	@SuppressWarnings("removal")
	private void applyDerivedIdentifierGenerator(
			DerivedIdentifierBinding derivedIdentifierBinding,
			Value identifierValue) {
		if ( !( identifierValue instanceof SimpleValue simpleIdentifierValue ) ) {
			return;
		}

		simpleIdentifierValue.setCustomIdGeneratorCreator( creationContext -> {
			final ForeignGenerator generator = new ForeignGenerator();
			final Properties parameters = new Properties();
			parameters.setProperty( ForeignGenerator.PROPERTY, derivedIdentifierBinding.property().getName() );
			parameters.setProperty( IdentifierGenerator.ENTITY_NAME, derivedIdentifierBinding.ownerBinding().getEntityName() );
			parameters.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, derivedIdentifierBinding.ownerBinding().getJpaEntityName() );
			generator.configure( creationContext, parameters );
			return generator;
		} );
	}

	private List<Column> resolveTargetIdentifierColumns(DerivedIdentifierBinding derivedIdentifierBinding) {
		final IdentifierBinding targetIdentifierBinding = bindingState.getIdentifierBinding(
				derivedIdentifierBinding.targetTypeBinder().getManagedType().getHierarchy().getRoot()
		);
		return targetIdentifierBinding == null
				? derivedIdentifierBinding.targetIdentifierColumns()
				: sortedColumns( targetIdentifierBinding.value(), targetIdentifierBinding.columns() );
	}

	private List<Column> resolveTargetRuntimeColumns(
			DerivedIdentifierBinding derivedIdentifierBinding,
			List<Column> fallbackColumns) {
		if ( derivedIdentifierBinding.referenceToPrimaryKey() ) {
			final IdentifierBinding targetIdentifierBinding = bindingState.getIdentifierBinding(
					derivedIdentifierBinding.targetTypeBinder().getManagedType().getHierarchy().getRoot()
			);
			if ( targetIdentifierBinding == null ) {
				return fallbackColumns;
			}
			if ( targetIdentifierBinding.value() instanceof Component component ) {
				return sortedComponentColumns( component );
			}
			return targetIdentifierBinding.columns();
		}
		return fallbackColumns;
	}

	private List<Column> sortedComponentColumns(Component component) {
		final java.util.ArrayList<Column> columns = new java.util.ArrayList<>();
		component.getProperties().stream()
				.sorted( Comparator.comparing( Property::getName ) )
				.forEach( property -> collectSortedComponentColumns( property.getValue(), columns ) );
		return columns;
	}

	private void collectSortedComponentColumns(Value value, List<Column> columns) {
		if ( value instanceof Component component ) {
			columns.addAll( sortedComponentColumns( component ) );
		}
		else {
			columns.addAll( value.getColumns() );
		}
	}

	private Value resolveIdentifierValue(
			DerivedIdentifierBinding derivedIdentifierBinding,
			Value identifierValue) {
		if ( StringHelper.isEmpty( derivedIdentifierBinding.mapsIdAttributeName() ) ) {
			return identifierValue;
		}
		if ( !( identifierValue instanceof Component identifierComponent ) ) {
			throw new MappingException(
					"@MapsId named identifier attribute `"
							+ derivedIdentifierBinding.mapsIdAttributeName()
							+ "` but the owner identifier is not a component - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
							+ "." + derivedIdentifierBinding.property().getName()
			);
		}
		if ( !identifierComponent.hasProperty( derivedIdentifierBinding.mapsIdAttributeName() ) ) {
			throw new MappingException(
					"@MapsId named unknown identifier attribute `"
							+ derivedIdentifierBinding.mapsIdAttributeName() + "` - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
							+ "." + derivedIdentifierBinding.property().getName()
			);
		}
		final Property identifierProperty = identifierComponent.getProperty(
				derivedIdentifierBinding.mapsIdAttributeName()
		);
		return identifierProperty.getValue();
	}

	private void validateJoinColumns(
			DerivedIdentifierBinding derivedIdentifierBinding,
			List<Column> identifierColumns) {
		final List<JoinColumn> joinColumns = derivedIdentifierBinding.joinColumns();
		if ( !joinColumns.isEmpty() && joinColumns.size() != identifierColumns.size() ) {
			throw new MappingException(
					"@MapsId join column count did not match identifier attribute column count - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
							+ "." + derivedIdentifierBinding.property().getName()
			);
		}
	}
}
