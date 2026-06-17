/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
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
		final List<String> referencedColumnNames = referencedColumnNames( associationTargetBinding );
		for ( Property property : associationTargetBinding.targetTypeBinder().getTypeBinding().getProperties() ) {
			if ( property.getValue() instanceof BasicValue basicValue
					&& columnNames( basicValue.getColumns() ).equals( referencedColumnNames ) ) {
				return property;
			}
		}
		throw new MappingException(
				"Could not resolve non-primary-key association target columns "
						+ referencedColumnNames + " - " + associationTargetBinding.role()
		);
	}

	private List<String> referencedColumnNames(AssociationTargetBinding associationTargetBinding) {
		final List<String> result = new ArrayList<>( associationTargetBinding.referencedColumnNames().size() );
		for ( String referencedColumnName : associationTargetBinding.referencedColumnNames() ) {
			if ( StringHelper.isEmpty( referencedColumnName ) ) {
				throw new MappingException(
						"Non-primary-key association join columns must name referenced columns - "
								+ associationTargetBinding.role()
				);
			}
			result.add( referencedColumnName.toLowerCase( java.util.Locale.ROOT ) );
		}
		return result;
	}

	private List<String> columnNames(List<Column> columns) {
		final List<String> result = new ArrayList<>( columns.size() );
		for ( Column column : columns ) {
			result.add( column.getName().toLowerCase( java.util.Locale.ROOT ) );
		}
		return result;
	}
}
