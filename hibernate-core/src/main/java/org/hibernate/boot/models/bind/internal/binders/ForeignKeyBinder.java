/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;

/// Creates physical foreign-key constraints for pending association and table keys.
///
/// Earlier phases create value/key columns and record source metadata, but do not
/// immediately create constraints.  Deferring constraint creation keeps the
/// order-sensitive parts explicit: property-ref associations need their target
/// property resolved, table keys need identifier-derived columns, and source
/// `@ForeignKey` customization should be applied in one place.
///
/// @since 9.0
/// @author Steve Ebersole
class ForeignKeyBinder {
	private final EntityTypeBinder entityBinder;

	ForeignKeyBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
	}

	void bindForeignKeys() {
		entityBinder.getBindingState().forEachForeignKeyBinding( (foreignKeyBinding) -> {
			if ( foreignKeyBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindForeignKey( foreignKeyBinding );
			}
		} );
		entityBinder.getBindingState().forEachTableForeignKeyBinding( (tableForeignKeyBinding) -> {
			if ( tableForeignKeyBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindTableForeignKey( tableForeignKeyBinding );
			}
		} );
	}

	private void bindForeignKey(ForeignKeyBinding foreignKeyBinding) {
		final ManyToOne value = foreignKeyBinding.value();
		if ( value.isReferenceToPrimaryKey() ) {
			value.createForeignKey();
		}
		else {
			value.createPropertyRefConstraints(
					entityBinder.getBindingState().getMetadataBuildingContext()
							.getMetadataCollector()
							.getEntityBindingMap()
			);
		}
		applyForeignKeySource( value, foreignKeyBinding.foreignKeySource() );
	}

	private void bindTableForeignKey(TableForeignKeyBinding tableForeignKeyBinding) {
		final ForeignKey foreignKey = tableForeignKeyBinding.key()
				.createForeignKeyOfEntity( tableForeignKeyBinding.referencedEntityName() );
		applyForeignKeySource( foreignKey, tableForeignKeyBinding.foreignKeySource() );
	}

	private void applyForeignKeySource(ManyToOne value, ForeignKeySource foreignKeySource) {
		if ( foreignKeySource == null ) {
			return;
		}

		final ForeignKey foreignKey = findForeignKey( value );
		applyForeignKeySource( foreignKey, foreignKeySource );
	}

	private void applyForeignKeySource(ForeignKey foreignKey, ForeignKeySource foreignKeySource) {
		if ( foreignKey == null || foreignKeySource == null ) {
			return;
		}
		if ( foreignKeySource.isNoConstraint() ) {
			foreignKey.disableCreation();
		}
		if ( StringHelper.isNotEmpty( foreignKeySource.name() ) ) {
			foreignKey.setName( foreignKeySource.name() );
		}
		if ( StringHelper.isNotEmpty( foreignKeySource.definition() ) ) {
			foreignKey.setKeyDefinition( foreignKeySource.definition() );
		}
		if ( StringHelper.isNotEmpty( foreignKeySource.options() ) ) {
			foreignKey.setOptions( foreignKeySource.options() );
		}
	}

	private ForeignKey findForeignKey(ManyToOne value) {
		for ( ForeignKey foreignKey : value.getTable().getForeignKeyCollection() ) {
			if ( value.getReferencedEntityName().equals( foreignKey.getReferencedEntityName() ) ) {
				return foreignKey;
			}
		}
		return null;
	}
}
