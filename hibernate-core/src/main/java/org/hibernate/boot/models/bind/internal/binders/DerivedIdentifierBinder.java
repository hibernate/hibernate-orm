/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
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
		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding(
				derivedIdentifierBinding.ownerType().getHierarchy().getRoot()
		);
		if ( identifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for derived identifier owner - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
			);
		}
		final Value identifierValue = resolveIdentifierValue( derivedIdentifierBinding, identifierBinding.value() );
		if ( !( identifierValue instanceof BasicValue
				|| identifierValue instanceof Component
				|| identifierValue instanceof ManyToOne ) ) {
			throw new UnsupportedOperationException(
					"@MapsId is only implemented for basic, component, or to-one identifier attributes - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
							+ "." + derivedIdentifierBinding.property().getName()
			);
		}

		final List<Column> identifierColumns = identifierValue.getColumns();
		final List<Column> targetColumns = resolveTargetColumns( derivedIdentifierBinding );
		if ( identifierColumns.size() != targetColumns.size() ) {
			throw new MappingException(
					"@MapsId identifier attribute column count did not match target identifier column count - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
							+ "." + derivedIdentifierBinding.property().getName()
					);
		}

		final List<Column> orderedIdentifierColumns = orderIdentifierColumns( identifierColumns, targetColumns );
		applyIdentifierColumnOverrides( derivedIdentifierBinding, identifierValue, orderedIdentifierColumns, targetColumns );
		validateJoinColumns( derivedIdentifierBinding, orderedIdentifierColumns, targetColumns );
		applyDerivedIdentifierGenerator( derivedIdentifierBinding, identifierValue );
		reorderPrimaryKeyColumns( identifierBinding.value() );
		for ( Column identifierColumn : sortedColumns( identifierValue, orderedIdentifierColumns ) ) {
			derivedIdentifierBinding.value().addColumn( identifierColumn, false, false );
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
										derivedIdentifierBinding.value().getColumns(),
										targetColumns,
										derivedIdentifierBinding.ownerBinding().getEntityName()
												+ "." + derivedIdentifierBinding.property().getName()
								)
						)
						: null
		) );
	}

	private void reorderPrimaryKeyColumns(Value identifierValue) {
		if ( identifierValue instanceof Component identifierComponent ) {
			identifierComponent.sortProperties();
			final var primaryKey = identifierComponent.getTable().getPrimaryKey();
			if ( primaryKey != null ) {
				primaryKey.reorderColumns( identifierComponent.getColumns() );
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
			applyImplicitIdentifierColumnNames( derivedIdentifierBinding, identifierColumns, targetColumns );
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
			renameIdentifierColumn(
					identifierColumns.get( i ),
					StringHelper.isNotEmpty( joinColumn.name() )
							? joinColumn.name()
							: derivedIdentifierBinding.property().getName() + "_" + targetColumns.get( i ).getName()
			);
		}
	}

	private void applyImplicitIdentifierColumnNames(
			DerivedIdentifierBinding derivedIdentifierBinding,
			List<Column> identifierColumns,
			List<Column> targetColumns) {
		for ( int i = 0; i < identifierColumns.size(); i++ ) {
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

	private List<Column> resolveTargetColumns(DerivedIdentifierBinding derivedIdentifierBinding) {
		if ( derivedIdentifierBinding.referenceToPrimaryKey() ) {
			final IdentifierBinding targetIdentifierBinding = bindingState.getIdentifierBinding(
					derivedIdentifierBinding.targetTypeBinder().getManagedType().getHierarchy().getRoot()
			);
			return targetIdentifierBinding == null
					? derivedIdentifierBinding.targetIdentifierColumns()
					: sortedColumns( targetIdentifierBinding.value(), targetIdentifierBinding.columns() );
		}

		final List<String> referencedColumnNames = ToOneAttributeBinder.referencedColumnNames(
				derivedIdentifierBinding.joinColumns()
		);
		final List<Identifier> referencedColumnIdentifiers = referencedColumnNames.stream()
				.map( bindingState.getDatabase()::toIdentifier )
				.toList();
		for ( Property property : derivedIdentifierBinding.targetTypeBinder().getTypeBinding().getProperties() ) {
			if ( columnNamesMatch( property.getValue().getColumns(), referencedColumnIdentifiers ) ) {
				return property.getValue().getColumns();
			}
		}
		throw new MappingException(
				"Could not resolve non-primary-key @MapsId target columns "
						+ referencedColumnNames + " - "
						+ derivedIdentifierBinding.ownerBinding().getEntityName()
						+ "." + derivedIdentifierBinding.property().getName()
		);
	}

	private boolean columnNamesMatch(List<Column> columns, List<Identifier> referencedColumnNames) {
		if ( columns.size() != referencedColumnNames.size() ) {
			return false;
		}
		for ( int i = 0; i < columns.size(); i++ ) {
			if ( !columns.get( i ).getNameIdentifier( bindingState.getDatabase() ).matches( referencedColumnNames.get( i ) ) ) {
				return false;
			}
		}
		return true;
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
			List<Column> identifierColumns,
			List<Column> targetColumns) {
		final List<JoinColumn> joinColumns = derivedIdentifierBinding.joinColumns();
		if ( !joinColumns.isEmpty() && joinColumns.size() != identifierColumns.size() ) {
			throw new MappingException(
					"@MapsId join column count did not match identifier attribute column count - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
							+ "." + derivedIdentifierBinding.property().getName()
			);
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
			if ( StringHelper.isNotEmpty( joinColumn.name() )
					&& !identifierColumns.get( i ).getName().equals( joinColumn.name() ) ) {
				throw new MappingException(
						"@MapsId join column name did not match identifier attribute column name `"
								+ identifierColumns.get( i ).getName() + "` - "
								+ derivedIdentifierBinding.ownerBinding().getEntityName()
								+ "." + derivedIdentifierBinding.property().getName()
				);
			}
		}
	}
}
