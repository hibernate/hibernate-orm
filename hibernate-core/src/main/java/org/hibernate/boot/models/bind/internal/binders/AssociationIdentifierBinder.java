/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;

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

	AssociationIdentifierBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
	}

	void bindAssociationIdentifiers() {
		bindingState.forEachAssociationIdentifierBinding( (associationIdentifierBinding) -> {
			if ( associationIdentifierBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindAssociationIdentifier( associationIdentifierBinding );
			}
		} );
	}

	private void bindAssociationIdentifier(AssociationIdentifierBinding associationIdentifierBinding) {
		final IdentifierBinding targetIdentifierBinding = bindingState.getIdentifierBinding(
				associationIdentifierBinding.targetTypeBinder().getManagedType().getHierarchy().getRoot()
		);
		if ( targetIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for association identifier target - "
							+ associationIdentifierBinding.value().getReferencedEntityName()
			);
		}

		final List<JoinColumn> joinColumns = associationIdentifierBinding.joinColumns();
		if ( !joinColumns.isEmpty() && joinColumns.size() != targetIdentifierBinding.columns().size() ) {
			throw new MappingException(
					"Association identifier join column count did not match target identifier column count - "
							+ associationIdentifierBinding.ownerBinding().getEntityName()
							+ "." + associationIdentifierBinding.property().getName()
			);
		}

		final List<JoinColumn> orderedJoinColumns = orderJoinColumns(
				joinColumns,
				targetIdentifierBinding.columns(),
				associationIdentifierBinding.ownerBinding().getClassName(),
				associationIdentifierBinding.property().getName()
		);
		final int columnCount = targetIdentifierBinding.columns().size();
		for ( int i = 0; i < columnCount; i++ ) {
			final JoinColumn joinColumn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final String targetColumnName = targetIdentifierBinding.columns().get( i ).getName();
			final Column column = ColumnBinder.bindColumn(
					org.hibernate.boot.models.bind.internal.sources.ColumnSource.from( joinColumn ),
					() -> associationIdentifierBinding.property().getName() + "_" + targetColumnName,
					true,
					false
			);
			associationIdentifierBinding.ownerBinding().getTable().addColumn( column );
			associationIdentifierBinding.value().addColumn( column, true, false );
			associationIdentifierBinding.ownerBinding().getTable().getPrimaryKey().addColumn( column );
			associationIdentifierBinding.identifierColumns().add( column );
		}
		if ( associationIdentifierBinding.ownerBinding().getIdentifier() instanceof Component identifierComponent ) {
			final List<Column> orderedIdentifierColumns = identifierComponent.getColumns();
			associationIdentifierBinding.ownerBinding().getTable().getPrimaryKey().reorderColumns( orderedIdentifierColumns );
			associationIdentifierBinding.identifierColumns().clear();
			associationIdentifierBinding.identifierColumns().addAll( orderedIdentifierColumns );
		}
		associationIdentifierBinding.value().setNonUpdatable();
		bindingState.addForeignKeyBinding( new ForeignKeyBinding(
				associationIdentifierBinding.ownerBinding(),
				associationIdentifierBinding.value(),
				associationIdentifierBinding.foreignKeySource()
		) );
	}

	private static List<JoinColumn> orderJoinColumns(
			List<JoinColumn> joinColumns,
			List<Column> targetColumns,
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
			String ownerClassName,
			String propertyName) {
		for ( JoinColumn joinColumn : joinColumns ) {
			if ( targetColumn.getName().equals( joinColumn.referencedColumnName() ) ) {
				return joinColumn;
			}
		}

		throw new MappingException(
				"Unable to match association identifier join column referencedColumnName to target identifier column `"
						+ targetColumn.getName() + "` - " + ownerClassName + "." + propertyName
		);
	}
}
