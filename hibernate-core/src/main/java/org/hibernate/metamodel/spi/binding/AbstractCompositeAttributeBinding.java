/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCompositeAttributeBinding
		extends AbstractSingularAttributeBinding
		implements SingularNonAssociationAttributeBinding, AttributeBindingContainer {
	private final String path;

	protected AbstractCompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext
		);

		this.path = container.getPathBase() + '.' + attribute.getName();

	}

	@Override
	public List<RelationalValueBinding> getRelationalValueBindings() {
		final List<RelationalValueBinding> bindings = new ArrayList<RelationalValueBinding>();
		collectRelationalValueBindings( bindings );
		return bindings;
	}

	@Override
	protected void collectRelationalValueBindings(List<RelationalValueBinding> valueBindings) {
		for ( AttributeBinding subAttributeBinding : attributeBindings() ) {
			if ( AbstractSingularAttributeBinding.class.isInstance( subAttributeBinding ) ) {
				( (AbstractSingularAttributeBinding) subAttributeBinding ).collectRelationalValueBindings( valueBindings );
			}
		}
	}

	@Override
	public EntityBinding seekEntityBinding() {
		return getContainer().seekEntityBinding();
	}

	@Override
	public String getPathBase() {
		return path;
	}

	@Override
	public AttributeContainer getAttributeContainer() {
		return (AttributeContainer) getAttribute().getSingularAttributeType();
	}

	public abstract boolean isAggregated();

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	public boolean hasDerivedValue() {
		// todo : not sure this is even relevant for components
		return false;
	}

	@Override
	public boolean isNullable() {
		// return false if there are any singular attributes are non-nullable
		for ( AttributeBinding attributeBinding : attributeBindings() ) {
			// only check singular attributes
			if ( attributeBinding.getAttribute().isSingular() &&
					! ( (SingularAttributeBinding) attributeBinding ).isNullable() ) {
				return false;
			}
		}
		return true;
	}


	@Override
	public boolean isIncludedInInsert() {
		// if the attribute is synthetic, this attribute binding (as a whole) is not insertable;
		if ( getAttribute().isSynthetic() ) {
			return false;
		}
		// otherwise, return true if there are any singular attributes that are included in the insert.
		for ( AttributeBinding attributeBinding : attributeBindings() ) {
			// only check singular attributes
			if ( attributeBinding.getAttribute().isSingular() &&
					( (SingularAttributeBinding) attributeBinding ).isIncludedInInsert() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isIncludedInUpdate() {
		// if the attribute is synthetic, this attribute binding (as a whole) is not updateable;
		if ( getAttribute().isSynthetic() ) {
			return false;
		}
		// otherwise, return true if there are any singular attributes that are updatable;
		for ( AttributeBinding attributeBinding : attributeBindings() ) {
			// only check singular attributes
			if ( attributeBinding.getAttribute().isSingular() &&
					( (SingularAttributeBinding) attributeBinding ).isIncludedInUpdate() ) {
				return true;
			}
		}
		return false;
	}

	protected abstract Map<String, AttributeBinding> attributeBindingMapInternal();

	@Override
	public AttributeBinding locateAttributeBinding(String name) {
		return attributeBindingMapInternal().get( name );
	}

	@Override
	public AttributeBinding locateAttributeBinding(List<org.hibernate.metamodel.spi.relational.Value> values) {
		for ( final AttributeBinding attributeBinding : attributeBindings() ) {
			if ( !BasicAttributeBinding.class.isInstance( attributeBinding ) ) {
				continue;
			}
			final BasicAttributeBinding basicAttributeBinding = (BasicAttributeBinding) attributeBinding;
			if ( basicAttributeBinding.getRelationalValueBindings().equals( values ) ) {
				return attributeBinding;
			}
		}
		return null;
	}

	public int attributeBindingSpan() {
		return attributeBindingMapInternal().size();
	}

	@Override
	public Iterable<AttributeBinding> attributeBindings() {
		return attributeBindingMapInternal().values();
	}

	@Override
	public Class<?> getClassReference() {
		return getAttributeContainer().getClassReference();
	}
}
