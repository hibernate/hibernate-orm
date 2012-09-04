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

import java.util.List;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * @author Gail Badner
 */
public interface MutableAttributeBindingContainer extends AttributeBindingContainer {

	/**
	 * Factory method for basic attribute bindings.
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param relationalValueBindings
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param lazy
	 * @param metaAttributeContext
	 * @param generation
	 *
	 * @return The attribute binding instance.
	 */
	public BasicAttributeBinding makeBasicAttributeBinding(
			SingularAttribute attribute,
			List<RelationalValueBinding> relationalValueBindings,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			PropertyGeneration generation);

	/**
	 * Factory method for component attribute bindings.
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param parentReferenceAttribute
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param lazy
	 * @param metaAttributeContext
	 *
	 * @return The attribute binding instance.
	 */
	public CompositeAttributeBinding makeAggregatedCompositeAttributeBinding(
			SingularAttribute attribute,
			SingularAttribute parentReferenceAttribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext);

	/**
	 * Factory method for many-to-one attribute bindings.
	 *
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param lazy
	 * @param metaAttributeContext
	 * @param referencedEntityBinding
	 * @param referencedAttributeBinding
	 * @param valueBindings
	 *
	 * @return The attribute binding instance.
	 */
	public ManyToOneAttributeBinding makeManyToOneAttributeBinding(
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding,
			List<RelationalValueBinding> valueBindings);

	/**
	 * Factory method for bag attribute bindings.
	 *
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param nature The nature of the collection elements.
	 * @param referencedAttributeBinding
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param metaAttributeContext
	 *
	 * @return The attribute binding instance.
	 */
	public BagBinding makeBagAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext);

	/**
	 * Factory method for list attribute bindings.
	 *
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param nature The nature of the collection elements.
	 * @param referencedAttributeBinding
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param metaAttributeContext
	 * @param base
	 *
	 * @return The attribute binding instance.
	 */
	public ListBinding makeListAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			int base);

	/**
	 * Factory method for map attribute bindings.
	 *
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param elementNature The nature of the collection elements.
	 * @param indexNature The nature of the collection indexes.
	 * @param referencedAttributeBinding
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param metaAttributeContext
	 *
	 * @return The attribute binding instance.
	 */
	public MapBinding makeMapAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature elementNature,
			PluralAttributeIndexBinding.Nature indexNature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext);

	/**
	 * Factory method for set attribute bindings.
	 *
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param nature The nature of the collection elements.
	 * @param referencedAttributeBinding
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param metaAttributeContext
	 *
	 * @return The attribute binding instance.
	 */
	public SetBinding makeSetAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext);
}
