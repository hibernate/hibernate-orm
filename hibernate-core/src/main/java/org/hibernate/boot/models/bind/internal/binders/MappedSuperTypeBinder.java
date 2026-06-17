/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

/// Binder for a mapped-superclass type.
///
/// Construction intentionally creates only the local {@link MappedSuperclass}
/// shell.  The coordinator then publishes that shell during the type-skeleton phase
/// before table, identifier, and member phases run for entities in the same
/// hierarchy.
/// The current phases for mapped-superclass binders are:
///
/// 1. Construction - create the local {@link MappedSuperclass} shell, linked to
/// the already-registered super mapped-superclass or entity shell when one exists.
/// 2. {@link #bindTypeSkeleton()} - register this binder with {@link BindingState},
/// publish the mapping shell to the metadata collector, and add the import.
///
/// Mapped-superclass binders do not currently participate in the entity table,
/// super-type wiring, entity metadata, identifier, or member phases.  The
/// implemented {@link TypeBindingPhase} contracts identify the phases this binder
/// participates in while the coordinator owns their ordering.
///
/// @since 9.0
/// @author Steve Ebersole
public class MappedSuperTypeBinder extends IdentifiableTypeBinder
		implements TypeBindingPhase.TypeSkeleton,
				TypeBindingPhase.Members {
	private final MappedSuperclass binding;
	private final ModelBinders modelBinders;

	public MappedSuperTypeBinder(
			MappedSuperclassTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		super( type, superType, hierarchyRelation, state, options, bindingContext );
		this.modelBinders = modelBinders;

		final IdentifiableTypeBinder superTypeBinder = getSuperTypeBinder();
		final EntityTypeBinder superEntityBinder = getSuperEntityBinder();
		final MappedSuperclass superMappedSuper;
		final PersistentClass superEntity;
		if ( superTypeBinder == superEntityBinder && superTypeBinder != null ) {
			superMappedSuper = null;
			superEntity = superEntityBinder.getTypeBinding();
		}
		else if ( superTypeBinder != null ) {
			superMappedSuper = (MappedSuperclass) superTypeBinder.getTypeBinding();
			superEntity = null;
		}
		else if ( superEntityBinder != null ) {
			superMappedSuper = null;
			superEntity = superEntityBinder.getTypeBinding();
		}
		else {
			superMappedSuper = null;
			superEntity = null;
		}

		this.binding = new MappedSuperclass( superMappedSuper, superEntity, getTable() );
		this.binding.setMappedClass( type.getClassDetails().toJavaClass() );
	}

	/// Publish the mapped-superclass skeleton for downstream binders.
	///
	/// After this phase the mapped-superclass is registered with both
	/// {@link BindingState} and the metadata collector and can be resolved as a super
	/// type by entity binders.  No table or member binding should be introduced here.
	public void bindTypeSkeleton() {
		getBindingState().registerTypeBinder( getManagedType(), this );

		getBindingState().addImport(
				StringHelper.unqualify( getManagedType().getClassDetails().getClassName() ),
				getManagedType().getClassDetails().getClassName()
		);
	}

	@Override
	public MappedSuperclass getTypeBinding() {
		return binding;
	}

	public void bindMembers() {
		prepareBinding( modelBinders );
		applyDeclaredPropertiesToNearestEntityConsumers( getManagedType() );
	}

	private void applyDeclaredPropertiesToNearestEntityConsumers(IdentifiableTypeMetadata type) {
		type.forEachSubType( (subType) -> {
			final var typeBinder = (IdentifiableTypeBinder) getBindingState().getTypeBinder( subType.getClassDetails() );
			if ( subType.getManagedTypeKind() == ManagedTypeMetadata.Kind.ENTITY ) {
				final var entityBinding = (PersistentClass) typeBinder.getTypeBinding();
				// Transitional contribution-lite bridge: until PersistentClass derives inherited mapped-superclass
				// state by traversing applied contributions, flatten each declared property only into the nearest
				// consuming entity.  Entity subclasses then inherit it through the normal entity closure.
				binding.getDeclaredProperties()
						.forEach( (property) -> applyMappedSuperclassProperty( property, entityBinding ) );
			}
			else {
				applyDeclaredPropertiesToNearestEntityConsumers( subType );
			}
		} );
	}

	private void applyMappedSuperclassProperty(Property property, PersistentClass entityBinding) {
		if ( hasDeclaredProperty( entityBinding, property.getName() ) ) {
			return;
		}

		entityBinding.addMappedSuperclassProperty( property.copy() );
	}

	private boolean hasDeclaredProperty(PersistentClass entityBinding, String propertyName) {
		for ( var declaredProperty : entityBinding.getDeclaredProperties() ) {
			if ( propertyName.equals( declaredProperty.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Table getTable() {
		final var superEntityBinder = getSuperEntityBinder();
		if ( superEntityBinder != null ) {
			return superEntityBinder.getTypeBinding().getTable();
		}

		final var rootEntityBinder = (EntityTypeBinder) getBindingState().getTypeBinder(
				getManagedType().getHierarchy().getRoot().getClassDetails()
		);
		return rootEntityBinder == null ? null : rootEntityBinder.getTypeBinding().getTable();
	}

	@Override
	public EntityTypeMetadata findSuperEntity() {
		if ( getSuperType() != null ) {
			final var superTypeBinder = getBindingState().getSuperTypeBinder( getManagedType().getClassDetails() );
			return superTypeBinder.findSuperEntity();
		}
		return null;
	}
}
