/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;

/// Resolves association targets that do not point at the target primary key.
///
/// An owning to-one can reference a unique property instead of the target
/// identifier by naming non-id `referencedColumnName` values.  Member binding can
/// create the association value, but it cannot reliably identify the referenced
/// property until the target entity's basic properties have been bound.  This
/// phase matches the referenced target columns to a target property and registers
/// the unique property reference for later foreign-key creation.
///
/// @since 9.0
/// @author Steve Ebersole
class AssociationTargetBinder {
	private final EntityTypeBinder entityBinder;

	AssociationTargetBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
	}

	void bindAssociationTargets() {
		entityBinder.getBindingState().forEachAssociationTargetBinding( (associationTargetBinding) -> {
			if ( associationTargetBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindAssociationTarget( associationTargetBinding );
			}
		} );
	}

	private void bindAssociationTarget(AssociationTargetBinding associationTargetBinding) {
		final Property property = resolveReferencedProperty( associationTargetBinding );
		UniquePropertyReferenceBinder.bindUniquePropertyReference(
				entityBinder.getBindingState(),
				associationTargetBinding.value(),
				property.getName()
		);
	}

	private Property resolveReferencedProperty(AssociationTargetBinding associationTargetBinding) {
		final List<Identifier> referencedColumnNames = referencedColumnNames( associationTargetBinding );
		for ( Property property : associationTargetBinding.targetTypeBinder().getTypeBinding().getProperties() ) {
			if ( property.getValue() instanceof BasicValue basicValue
					&& columnNamesMatch( basicValue.getColumns(), referencedColumnNames ) ) {
				return property;
			}
		}
		throw new MappingException(
				"Could not resolve non-primary-key association target columns "
						+ referencedColumnNames + " - " + associationTargetBinding.role()
		);
	}

	private List<Identifier> referencedColumnNames(AssociationTargetBinding associationTargetBinding) {
		final List<Identifier> result = new ArrayList<>( associationTargetBinding.referencedColumnNames().size() );
		final Database database = entityBinder.getBindingState().getDatabase();
		for ( String referencedColumnName : associationTargetBinding.referencedColumnNames() ) {
			if ( StringHelper.isEmpty( referencedColumnName ) ) {
				throw new MappingException(
						"Non-primary-key association join columns must name referenced columns - "
								+ associationTargetBinding.role()
				);
			}
			result.add( database.toIdentifier( referencedColumnName ) );
		}
		return result;
	}

	private boolean columnNamesMatch(List<Column> columns, List<Identifier> referencedColumnNames) {
		if ( columns.size() != referencedColumnNames.size() ) {
			return false;
		}
		final Database database = entityBinder.getBindingState().getDatabase();
		for ( int i = 0; i < columns.size(); i++ ) {
			if ( !columns.get( i ).getNameIdentifier( database ).matches( referencedColumnNames.get( i ) ) ) {
				return false;
			}
		}
		return true;
	}
}
