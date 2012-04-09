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

import java.util.Comparator;
import java.util.List;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * Common contract for {@link EntityBinding} and {@link CompositeAttributeBinding} in so far as they are both
 * containers for {@link AttributeBinding} descriptors
 *
 * @author Steve Ebersole
 */
public interface AttributeBindingContainer {
	/**
	 * Obtain the path base of this container.  Intended to help uniquely identify each attribute binding.
	 *
	 * @return The path base for this container.
	 */
	public String getPathBase();

	/**
	 * Obtain the underlying domain attribute container.
	 *
	 * @return The attribute container
	 */
	public AttributeContainer getAttributeContainer();

	/**
	 * Obtain all attribute bindings
	 *
	 * @return All attribute bindings
	 */
	public Iterable<AttributeBinding> attributeBindings();

	/**
	 * Locate a specific attribute binding, by its local name.
	 *
	 * @param name The name of the attribute, local to this container.
	 *
	 * @return The attribute binding.
	 */
	public AttributeBinding locateAttributeBinding(String name);

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
	public CompositeAttributeBinding makeComponentAttributeBinding(
			SingularAttribute attribute,
			SingularAttribute parentReferenceAttribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
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
			MetaAttributeContext metaAttributeContext,
			SingularAttributeBinding referencedAttributeBinding,
			List<RelationalValueBinding> valueBindings);

	/**
	 * Factory method for bag attribute bindings.
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param nature The nature of the collection elements.
	 * @param referencedAttributeBinding
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param lazy
	 * @param metaAttributeContext
	 *
	 * @return The attribute binding instance.
	 */
	public BagBinding makeBagAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext);

	/**
	 * Factory method for bag attribute bindings.
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param nature The nature of the collection elements.
	 * @param referencedAttributeBinding
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param lazy
	 * @param metaAttributeContext
	 * @param base
	 *
	 * @return The attribute binding instance.
	 */
	public ListBinding makeListAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext,
			int base );

	/**
	 * Factory method for bag attribute bindings.
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param nature The nature of the collection elements.
	 * @param referencedAttributeBinding
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param lazy
	 * @param metaAttributeContext
	 * @param comparator
	 *
	 * @return The attribute binding instance.
	 */
	public SetBinding makeSetAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext,
			Comparator comparator);

	/**
	 * Seeks out the entity binding that is the root of this component path.
	 *
	 * @return The entity binding
	 */
	public EntityBinding seekEntityBinding();

	/**
	 * Obtain the {@link Class} reference for this attribute container.  Generally this is used to perform reflection
	 * on the attributes.
	 *
	 * @return The {@link Class} reference
	 */
	public Class<?> getClassReference();

	/**
	 * Obtain the meta-attribute context for this container.
	 *
	 * @return The meta-attribute context.
	 */
	public MetaAttributeContext getMetaAttributeContext();
}
