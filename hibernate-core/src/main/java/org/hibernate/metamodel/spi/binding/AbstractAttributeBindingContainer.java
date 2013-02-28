/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * @author Gail Badner
 */
public abstract class AbstractAttributeBindingContainer implements AttributeBindingContainer {

	protected abstract Map<String, AttributeBinding> attributeBindingMapInternal();

	@Override
	public AttributeBinding locateAttributeBinding(String name) {
		return attributeBindingMapInternal().get( name );
	}

	@Override
	public AttributeBinding locateAttributeBinding(List<Value> values) {
		for ( AttributeBinding attributeBinding : attributeBindingMapInternal().values() ) {
			if ( !attributeBinding.getAttribute().isSingular() ) {
				continue;
			}
			SingularAttributeBinding basicAttributeBinding = (SingularAttributeBinding) attributeBinding;

			List<org.hibernate.metamodel.spi.relational.Value> attributeValues = new ArrayList<Value>();
			for ( RelationalValueBinding relationalBinding : basicAttributeBinding.getRelationalValueBindings() ) {
				attributeValues.add( relationalBinding.getValue() );
			}

			if ( attributeValues.equals( values ) ) {
				return attributeBinding;
			}
		}
		return null;
	}

	@Override
	public Class<?> getClassReference() {
		return getAttributeContainer().getClassReference();
	}


	@Override
	public int attributeBindingSpan() {
		return attributeBindingMapInternal().size();
	}

	@Override
	public Iterable<AttributeBinding> attributeBindings() {
		return attributeBindingMapInternal().values();
	}

	protected void collectRelationalValueBindings(RelationalValueBindingContainer relationalValueBindingContainer) {
		for ( AttributeBinding subAttributeBinding : attributeBindings() ) {
			if ( AbstractSingularAttributeBinding.class.isInstance( subAttributeBinding ) ) {
				( (AbstractSingularAttributeBinding) subAttributeBinding ).collectRelationalValueBindings( relationalValueBindingContainer );
			}
		}
	}

	@Override
	public BasicAttributeBinding makeBasicAttributeBinding(
			SingularAttribute attribute,
			List<RelationalValueBinding> relationalValueBindings,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			PropertyGeneration generation) {
		final BasicAttributeBinding binding = new BasicAttributeBinding(
				this,
				attribute,
				relationalValueBindings,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				generation
		);
		registerAttributeBinding( binding );
		return binding;
	}

	protected void registerAttributeBinding(AttributeBinding attributeBinding) {
		// todo : hook this into the EntityBinding notion of "entity referencing attribute bindings"
		attributeBindingMapInternal().put( attributeBinding.getAttribute().getName(), attributeBinding );
	}

	@Override
	public CompositeAttributeBinding makeAggregatedCompositeAttributeBinding(
			SingularAttribute attribute,
			SingularAttribute parentReferenceAttribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext) {
		final CompositeAttributeBinding binding =
				CompositeAttributeBinding.createAggregatedCompositeAttributeBinding(
						this,
						attribute,
						propertyAccessorName,
						includedInOptimisticLocking,
						lazy,
						naturalIdMutability,
						metaAttributeContext,
						parentReferenceAttribute
				);
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public OneToOneAttributeBinding makeOneToOneAttributeBinding(
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding,
			boolean isConstrained,
			List<RelationalValueBinding> valueBindings) {
		final OneToOneAttributeBinding binding = new OneToOneAttributeBinding(
				this,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				referencedEntityBinding,
				referencedAttributeBinding,
				isConstrained,
				valueBindings
		);
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public ManyToOneAttributeBinding makeManyToOneAttributeBinding(
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			boolean isNotFoundAnException,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding,
			List<RelationalValueBinding> valueBindings) {
		final ManyToOneAttributeBinding binding = new ManyToOneAttributeBinding(
				this,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				isNotFoundAnException,
				naturalIdMutability,
				metaAttributeContext,
				referencedEntityBinding,
				referencedAttributeBinding,
				valueBindings
		);
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public BagBinding makeBagAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttribute.Nature.BAG );
		final BagBinding binding = new BagBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext
		);
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public ListBinding makeListAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			int base) {
		Helper.checkPluralAttributeNature( attribute, PluralAttribute.Nature.LIST );
		final ListBinding binding = new ListBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				base );
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public ArrayBinding makeArrayAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			int base) {
		Helper.checkPluralAttributeNature( attribute, PluralAttribute.Nature.ARRAY );
		final ArrayBinding binding = new ArrayBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				base );
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public MapBinding makeMapAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature elementNature,
			PluralAttributeIndexBinding.Nature indexNature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttribute.Nature.MAP );
		final MapBinding binding = new MapBinding(
				this,
				attribute,
				elementNature,
				indexNature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext );
		registerAttributeBinding( binding );
		return binding;
	}

	@Override
	public SetBinding makeSetAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttribute.Nature.SET );
		final SetBinding binding = new SetBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext
		);
		registerAttributeBinding( binding );
		return binding;
	}
}
