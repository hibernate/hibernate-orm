/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.ToOne;

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
		final ToOne value = foreignKeyBinding.value();
		final ForeignKey resolvedForeignKey = createResolvedForeignKey(
				foreignKeyBinding.resolvedForeignKey(),
				value.getReferencedEntityName()
		);
		if ( resolvedForeignKey != null ) {
			applyForeignKeySource( resolvedForeignKey, foreignKeyBinding.foreignKeySource() );
			return;
		}
		if ( value.isReferenceToPrimaryKey() ) {
			value.createForeignKey();
		}
		else if ( value instanceof ManyToOne manyToOne ) {
			manyToOne.createPropertyRefConstraints(
					entityBinder.getBindingState().getMetadataBuildingContext()
							.getMetadataCollector()
							.getEntityBindingMap()
			);
		}
		applyForeignKeySource( value, foreignKeyBinding.foreignKeySource() );
	}

	private void bindTableForeignKey(TableForeignKeyBinding tableForeignKeyBinding) {
		final ForeignKey resolvedForeignKey = createResolvedForeignKey(
				tableForeignKeyBinding.resolvedForeignKey(),
				tableForeignKeyBinding.referencedEntityName()
		);
		if ( resolvedForeignKey != null ) {
			applyForeignKeySource( resolvedForeignKey, tableForeignKeyBinding.foreignKeySource() );
			return;
		}
		final ForeignKey foreignKey = tableForeignKeyBinding.key()
				.createForeignKeyOfEntity( tableForeignKeyBinding.referencedEntityName() );
		applyForeignKeySource( foreignKey, tableForeignKeyBinding.foreignKeySource() );
	}

	private ForeignKey createResolvedForeignKey(ResolvedForeignKey resolvedForeignKey, String referencedEntityName) {
		if ( resolvedForeignKey == null ) {
			return null;
		}
		final PersistentClass referencedEntity = entityBinder.getBindingState()
				.getMetadataBuildingContext()
				.getMetadataCollector()
				.getEntityBinding( referencedEntityName );
		if ( referencedEntity == null ) {
			return null;
		}
		return resolvedForeignKey.createForeignKey( referencedEntity );
	}

	private void applyForeignKeySource(ToOne value, ForeignKeySource foreignKeySource) {
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

	private ForeignKey findForeignKey(ToOne value) {
		for ( ForeignKey foreignKey : value.getTable().getForeignKeyCollection() ) {
			if ( value.getReferencedEntityName().equals( foreignKey.getReferencedEntityName() ) ) {
				return foreignKey;
			}
		}
		return null;
	}
}
