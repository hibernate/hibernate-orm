/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.util.Map;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;

/**
 * Binding for an {@linkplain jakarta.persistence.metamodel.IdentifiableType identifiable type}
 *
 * @author Steve Ebersole
 */
public abstract class IdentifiableTypeBinding extends ManagedTypeBinding {
	protected final IdentifiableTypeMetadata typeMetadata;
	protected final IdentifiableTypeBinding superTypeBinding;
	protected final IdentifiableTypeMetadata superTypeMetadata;

	private final Map<String, AttributeBinding> attributeBindings;

	public IdentifiableTypeBinding(
			IdentifiableTypeMetadata typeMetadata,
			IdentifiableTypeBinding superTypeBinding,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		super( typeMetadata.getClassDetails(), bindingOptions, bindingState, bindingContext );
		this.typeMetadata = typeMetadata;
		this.superTypeBinding = superTypeBinding;
		this.superTypeMetadata = superTypeBinding == null ? null : superTypeBinding.getTypeMetadata();

		// NOTE: slightly over-sized (id, version, ...), but that's ok
		this.attributeBindings = CollectionHelper.linkedMapOfSize( typeMetadata.getNumberOfAttributes() );
	}

	public IdentifiableTypeMetadata getTypeMetadata() {
		return typeMetadata;
	}

	public IdentifiableTypeMetadata getSuperTypeMetadata() {
		return superTypeMetadata;
	}

	public IdentifiableTypeBinding getSuperTypeBinding() {
		return superTypeBinding;
	}

	@Override
	public abstract IdentifiableTypeClass getBinding();

	@Override
	public Map<String, AttributeBinding> getAttributeBindings() {
		return attributeBindings;
	}

	public EntityTypeMetadata findSuperEntityMetadata() {
		final EntityBinding superEntityBinder = getSuperEntityBinding();
		if ( superEntityBinder == null ) {
			return null;
		}

		return superEntityBinder.getTypeMetadata();
	}

	public PersistentClass resolveSuperEntityPersistentClass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass ) {
			return (PersistentClass) superTypeBinding;
		}

		if ( superTypeBinding.getSuperType() != null ) {
			return resolveSuperEntityPersistentClass( superTypeBinding.getSuperType() );
		}

		throw new ModelsException( "Unable to resolve super PersistentClass for " + superTypeBinding );
	}

	public EntityBinding getSuperEntityBinding() {
		IdentifiableTypeBinding check = superTypeBinding;
		if ( check == null ) {
			return null;
		}

		do {
			if ( check.getBinding() instanceof PersistentClass ) {
				return (EntityBinding) check;
			}
			check = check.getSuperTypeBinding();
		} while ( check != null );

		return null;
	}

	protected void prepareAttributeBindings(Table primaryTable) {
		typeMetadata.forEachAttribute( (index, attributeMetadata) -> {
			if ( excludeAttributeFromPreparation( attributeMetadata ) ) {
				return;
			}

			final var attributeBinding = createAttributeBinding( attributeMetadata, primaryTable );
			attributeBindings.put( attributeMetadata.getName(), attributeBinding );
			getBinding().applyProperty( attributeBinding.getProperty() );
		} );
	}


	protected boolean excludeAttributeFromPreparation(AttributeMetadata attributeMetadata) {
		return false;
	}

	protected abstract AttributeBinding createAttributeBinding(AttributeMetadata attributeMetadata, Table primaryTable);
}
