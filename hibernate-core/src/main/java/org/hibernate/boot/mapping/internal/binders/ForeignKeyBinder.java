/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.materialize.ForeignKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.ResolvedForeignKey;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
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
	private final ForeignKeyMappingMaterializer foreignKeyMappingMaterializer = new ForeignKeyMappingMaterializer();

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
		if ( isNoConstraint( foreignKeyBinding.foreignKeySource() ) ) {
			foreignKeyBinding.value().disableForeignKey();
			return;
		}
		final ToOne value = foreignKeyBinding.value();
		if ( value instanceof ManyToOne manyToOne && manyToOne.getNotFoundAction() != null ) {
			value.disableForeignKey();
			return;
		}
		final ForeignKey resolvedForeignKey = createResolvedForeignKey(
				applyLateOnDeleteAction( foreignKeyBinding.resolvedForeignKey(), value ),
				value.getReferencedEntityName()
		);
		if ( resolvedForeignKey != null ) {
			applyForeignKeySource( resolvedForeignKey, foreignKeyBinding.foreignKeySource() );
			return;
		}
		final ForeignKey foreignKey;
		if ( value.isReferenceToPrimaryKey() ) {
			final PersistentClass referencedEntity = entityBinder.getBindingState()
					.getEntityBinding( value.getReferencedEntityName() );
			foreignKey = referencedEntity == null
					? null
					: foreignKeyMappingMaterializer.materializeForeignKey(
							value,
							referencedEntity,
							foreignKeyBinding.ownerBinding().getEntityName() + "." + value.getTable().getName()
					);
		}
		else if ( value instanceof ManyToOne manyToOne ) {
			final PersistentClass referencedEntity = entityBinder.getBindingState()
					.getEntityBinding( manyToOne.getReferencedEntityName() );
			foreignKey = referencedEntity == null
					? null
					: materializePropertyRefForeignKey( foreignKeyBinding, manyToOne, referencedEntity );
		}
		else {
			foreignKey = null;
		}
		applyForeignKeySource( foreignKey, foreignKeyBinding.foreignKeySource() );
	}

	private ForeignKey materializePropertyRefForeignKey(
			ForeignKeyBinding foreignKeyBinding,
			ManyToOne manyToOne,
			PersistentClass referencedEntity) {
		if ( manyToOne.hasFormula() ) {
			return null;
		}
		final String sourceRole = foreignKeyBinding.ownerBinding().getEntityName() + "." + manyToOne.getTable().getName();
		if ( foreignKeyBinding.referencedColumnNames().isEmpty() ) {
			return foreignKeyMappingMaterializer.materializeForeignKey( manyToOne, referencedEntity, sourceRole );
		}
		final org.hibernate.mapping.Property referencedProperty =
				referencedEntity.getReferencedProperty( manyToOne.getReferencedPropertyName() );
		if ( referencedProperty == null ) {
			return foreignKeyMappingMaterializer.materializeForeignKey( manyToOne, referencedEntity, sourceRole );
		}
		return foreignKeyMappingMaterializer.materializeForeignKey(
				ResolvedForeignKey.from(
						manyToOne,
						manyToOne.getReferencedEntityName(),
						SelectableOrderResolver.resolveByReferencedNames(
								manyToOne.getColumns(),
								referencedProperty.getColumns(),
								foreignKeyBinding.referencedColumnNames(),
								entityBinder.getBindingState().getDatabase(),
								sourceRole
						),
						referencedProperty.getValue().getTable()
				),
				referencedEntity
		);
	}

	private void bindTableForeignKey(TableForeignKeyBinding tableForeignKeyBinding) {
		if ( isNoConstraint( tableForeignKeyBinding.foreignKeySource() ) ) {
			if ( tableForeignKeyBinding.key() instanceof SimpleValue simpleValue ) {
				simpleValue.disableForeignKey();
			}
			return;
		}
		final ForeignKey resolvedForeignKey = createResolvedForeignKey(
				tableForeignKeyBinding.resolvedForeignKey(),
				tableForeignKeyBinding.referencedEntityName()
		);
		if ( resolvedForeignKey != null ) {
			applyForeignKeySource( resolvedForeignKey, tableForeignKeyBinding.foreignKeySource() );
			return;
		}
		final PersistentClass referencedEntity = entityBinder.getBindingState()
				.getEntityBinding( tableForeignKeyBinding.referencedEntityName() );
		final ForeignKey foreignKey = referencedEntity == null
				? null
				: foreignKeyMappingMaterializer.materializeForeignKey(
						tableForeignKeyBinding.key(),
						referencedEntity,
						tableForeignKeyBinding.ownerBinding().getEntityName()
								+ "." + tableForeignKeyBinding.key().getTable().getName()
				);
		applyForeignKeySource( foreignKey, tableForeignKeyBinding.foreignKeySource() );
	}

	private ForeignKey createResolvedForeignKey(ResolvedForeignKey resolvedForeignKey, String referencedEntityName) {
		if ( resolvedForeignKey == null ) {
			return null;
		}
		final PersistentClass referencedEntity = entityBinder.getBindingState()
				.getEntityBinding( referencedEntityName );
		if ( referencedEntity == null ) {
			return null;
		}
		return foreignKeyMappingMaterializer.materializeForeignKey( resolvedForeignKey, referencedEntity );
	}

	private ResolvedForeignKey applyLateOnDeleteAction(ResolvedForeignKey resolvedForeignKey, ToOne value) {
		if ( resolvedForeignKey == null
				|| resolvedForeignKey.onDeleteAction() != null
				|| value.getOnDeleteAction() == null ) {
			return resolvedForeignKey;
		}
		return new ResolvedForeignKey(
				resolvedForeignKey.table(),
				resolvedForeignKey.foreignKeyName(),
				resolvedForeignKey.referencedEntityName(),
				resolvedForeignKey.foreignKeyDefinition(),
				resolvedForeignKey.foreignKeyOptions(),
				value.getOnDeleteAction(),
				resolvedForeignKey.selectableOrder(),
				resolvedForeignKey.referencedTable()
		);
	}

	private void applyForeignKeySource(ForeignKey foreignKey, ForeignKeySource foreignKeySource) {
		if ( foreignKey == null || foreignKeySource == null ) {
			return;
		}
		if ( isNoConstraint( foreignKeySource ) ) {
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

	private boolean isNoConstraint(ForeignKeySource foreignKeySource) {
		return foreignKeySource == null
				? entityBinder.getBindingState().getMetadataBuildingContext()
						.getBuildingOptions()
						.isNoConstraintByDefault()
				: foreignKeySource.isNoConstraint(
						entityBinder.getBindingState().getMetadataBuildingContext()
								.getBuildingOptions()
								.isNoConstraintByDefault()
				);
	}
}
