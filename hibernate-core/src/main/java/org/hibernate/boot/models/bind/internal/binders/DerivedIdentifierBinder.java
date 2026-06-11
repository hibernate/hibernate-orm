/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
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
		bindingState.forEachDerivedIdentifierBinding( (derivedIdentifierBinding) -> {
			if ( derivedIdentifierBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindDerivedIdentifier( derivedIdentifierBinding );
			}
		} );
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
		if ( !( identifierBinding.value() instanceof Component identifierComponent ) ) {
			throw new UnsupportedOperationException(
					"@MapsId is only implemented for component identifiers - "
							+ derivedIdentifierBinding.ownerBinding().getEntityName()
							+ "." + derivedIdentifierBinding.property().getName()
					);
		}

		final Value identifierValue = resolveIdentifierValue( derivedIdentifierBinding, identifierComponent );
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

		validateJoinColumns( derivedIdentifierBinding, identifierColumns, targetColumns );
		for ( Column identifierColumn : identifierColumns ) {
			derivedIdentifierBinding.value().addColumn( identifierColumn, false, false );
		}
		derivedIdentifierBinding.property().setOptional( false );
	}

	private List<Column> resolveTargetColumns(DerivedIdentifierBinding derivedIdentifierBinding) {
		if ( derivedIdentifierBinding.referenceToPrimaryKey() ) {
			return derivedIdentifierBinding.targetIdentifierColumns();
		}

		final List<String> referencedColumnNames = ToOneAttributeBinder.referencedColumnNames(
				derivedIdentifierBinding.joinColumns()
		);
		for ( Property property : derivedIdentifierBinding.targetTypeBinder().getTypeBinding().getProperties() ) {
			if ( columnNames( property.getValue().getColumns() ).equals( referencedColumnNames ) ) {
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

	private List<String> columnNames(List<Column> columns) {
		return columns.stream().map( Column::getName ).toList();
	}

	private Value resolveIdentifierValue(
			DerivedIdentifierBinding derivedIdentifierBinding,
			Component identifierComponent) {
		if ( StringHelper.isEmpty( derivedIdentifierBinding.mapsIdAttributeName() ) ) {
			return identifierComponent;
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
