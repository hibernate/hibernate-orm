/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;

/// Base binder for entity and mapped-superclass types.
///
/// The mapping model does not have a single concrete "identifiable type" class,
/// so this base class keeps shared binding state for entity and mapped-superclass
/// binders while leaving the actual mapping object type abstract.  It also owns
/// common member-binding support used by entity binders after identifiers and
/// tables have been established.
///
/// @since 9.0
/// @author Steve Ebersole
public abstract class IdentifiableTypeBinder extends ManagedTypeBinder {
	private final IdentifiableTypeMetadata superType;
	private final EntityHierarchy.HierarchyRelation hierarchyRelation;

	private final List<AttributeBinder> attributeBinders;
	private final IdentifiableTypeBinder superTypeBinder;

	public IdentifiableTypeBinder(
			IdentifiableTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		super( type, state, options, bindingContext );
		this.superType = superType;
		this.hierarchyRelation = hierarchyRelation;
		this.superTypeBinder = superType == null ? null : (IdentifiableTypeBinder) state.getTypeBinder( superType.getClassDetails() );
		this.attributeBinders = new ArrayList<>( type.getNumberOfAttributes() );
	}

	public abstract EntityTypeMetadata findSuperEntity();

	public EntityTypeBinder getSuperEntityBinder() {
		IdentifiableTypeBinder check = superTypeBinder;
		if ( check == null ) {
			return null;
		}

		do {
			if ( check.getTypeBinding() instanceof PersistentClass ) {
				return (EntityTypeBinder) check;
			}
			check = check.getSuperTypeBinder();
		} while ( check != null );

		return null;
	}

	public IdentifiableTypeBinder getSuperTypeBinder() {
		return superTypeBinder;
	}

	public abstract IdentifiableTypeClass getTypeBinding();

	public IdentifiableTypeMetadata getSuperType() {
		return superType;
	}

	public EntityHierarchy.HierarchyRelation getHierarchyRelation() {
		return hierarchyRelation;
	}

	public abstract Table getTable();

	@Override
	public IdentifiableTypeMetadata getManagedType() {
		return (IdentifiableTypeMetadata) super.getManagedType();
	}

	@Override
	protected void prepareBinding(ModelBinders modelBinders) {
		bindDeclaredAttributes(
				modelBinders,
				getManagedType(),
				getManagedType(),
				resolveAttributeOwnerBinding(),
				getTable(),
				this::addDeclaredProperty
		);

		super.prepareBinding( modelBinders );
	}

	protected void bindDeclaredAttributes(
			ModelBinders modelBinders,
			IdentifiableTypeMetadata sourceType,
			IdentifiableTypeMetadata ownerType,
			PersistentClass attributeOwnerBinding,
			Table primaryTable,
			Consumer<Property> propertyConsumer) {
		bindDeclaredAttributes( modelBinders, sourceType, ownerType, attributeOwnerBinding, primaryTable, propertyConsumer, true );
	}

	protected void bindDeclaredAttributes(
			ModelBinders modelBinders,
			IdentifiableTypeMetadata sourceType,
			IdentifiableTypeMetadata ownerType,
			PersistentClass attributeOwnerBinding,
			Table primaryTable,
			Consumer<Property> propertyConsumer,
			boolean includePluralAttributes) {
		sourceType.forEachAttribute( (index, attributeMetadata) -> {
			if ( sourceType.getHierarchy().getIdMapping().contains( attributeMetadata )
					|| attributeMetadata.getMember().hasDirectAnnotationUsage( Id.class )
					|| attributeMetadata.getMember().hasDirectAnnotationUsage( EmbeddedId.class )
					|| sourceType.getHierarchy().getVersionAttribute() == attributeMetadata
					|| sourceType.getHierarchy().getTenantIdAttribute() == attributeMetadata ) {
				return;
			}
			if ( !includePluralAttributes && isPlural( attributeMetadata.getNature() ) ) {
				return;
			}
			if ( overridesSuperAttribute( sourceType, attributeMetadata ) ) {
				return;
			}

			final var attributeBinder = new AttributeBinder(
					ownerType,
					attributeOwnerBinding,
					attributeMetadata,
					primaryTable,
					modelBinders,
					getBindingState(),
					getOptions(),
					getBindingContext()
			);

			final var property = attributeBinder.getBinding();
			applyGenericPropertyMarkers( sourceType, ownerType, attributeMetadata, property );
			final var value = property.getValue();

			attributeBinders.add( attributeBinder );
			final Table attributeTable = value.getTable();
			if ( attributeTable == primaryTable || value instanceof org.hibernate.mapping.Collection ) {
				propertyConsumer.accept( property );
			}
			else {
				final Join join = findJoin( attributeOwnerBinding, attributeTable );
				join.addProperty( property );
			}
			CustomMappingBinder.callAttributeBinders(
					attributeMetadata.getMember(),
					attributeOwnerBinding,
					property,
					getBindingState(),
					getBindingContext()
			);
		} );
	}

	private boolean isPlural(AttributeNature nature) {
		return nature == AttributeNature.ELEMENT_COLLECTION
			|| nature == AttributeNature.MANY_TO_MANY
			|| nature == AttributeNature.ONE_TO_MANY
			|| nature == AttributeNature.MANY_TO_ANY;
	}

	private void applyGenericPropertyMarkers(
			IdentifiableTypeMetadata sourceType,
			IdentifiableTypeMetadata ownerType,
			AttributeMetadata attributeMetadata,
			Property property) {
		final TypeDetails declaredType = attributeMetadata.getMember().getType();
		if ( declaredType.isResolved() ) {
			return;
		}

		if ( sourceType.getClassDetails().getName().equals( ownerType.getClassDetails().getName() ) ) {
			property.setGeneric( true );
			property.setReturnedClassName( declaredType.getName() );
		}
		else {
			final TypeDetails resolvedType = attributeMetadata.getMember().resolveRelativeType( ownerType.getClassDetails() );
			property.setGeneric( false );
			property.setGenericSpecialization( true );
			property.setReturnedClassName( resolvedType.getName() );
		}
	}

	private boolean overridesSuperAttribute(
			IdentifiableTypeMetadata sourceType,
			AttributeMetadata attributeMetadata) {
		final var sourceSuperType = sourceType.getSuperType();
		if ( sourceSuperType == null || sourceSuperType.findAttribute( attributeMetadata.getName() ) == null ) {
			return false;
		}
		return attributeMetadata.getMember()
				.getDeclaringType()
				.getClassName()
				.equals( sourceType.getClassDetails().getClassName() );
	}

	protected PersistentClass resolveAttributeOwnerBinding() {
		if ( getTypeBinding() instanceof PersistentClass persistentClass ) {
			return persistentClass;
		}

		final EntityTypeBinder superEntityBinder = getSuperEntityBinder();
		if ( superEntityBinder != null ) {
			return superEntityBinder.getTypeBinding();
		}

		final EntityTypeBinder rootEntityBinder = (EntityTypeBinder) getBindingState().getTypeBinder(
				getManagedType().getHierarchy().getRoot().getClassDetails()
		);
		return rootEntityBinder == null ? null : rootEntityBinder.getTypeBinding();
	}

	private void addDeclaredProperty(Property property) {
		final IdentifiableTypeClass typeBinding = getTypeBinding();
		if ( typeBinding instanceof PersistentClass persistentClass ) {
			persistentClass.addProperty( property );
		}
		else if ( typeBinding instanceof MappedSuperclass mappedSuperclass ) {
			mappedSuperclass.addDeclaredProperty( property );
		}
		else {
			throw new IllegalStateException( "Unexpected identifiable mapping type: " + typeBinding );
		}
	}

	private Join findJoin(PersistentClass attributeOwnerBinding, Table attributeTable) {
		final List<Join> joins = attributeOwnerBinding.getJoinClosure();
		for ( int i = 0; i < joins.size(); i++ ) {
			if ( joins.get( i ).getTable() == attributeTable ) {
				return joins.get( i );
			}
		}
		throw new IllegalArgumentException( "Could not locate Table for name - " + attributeTable.getName() );
	}
}
