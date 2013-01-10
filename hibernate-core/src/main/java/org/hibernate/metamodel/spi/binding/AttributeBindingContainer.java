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

import java.util.List;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
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
	 * Return the number of attribute bindings returned by
	 * {@link #attributeBindings()}.
	 *
	 * @return the number of attribute bindings returned by
	 * {@link #attributeBindings()}.
	 */
	public int attributeBindingSpan();

	/**
	 * Locate a specific attribute binding, by its local name.
	 *
	 * @param name The name of the attribute, local to this container.
	 *
	 * @return The attribute binding or {@code null} if none could be found.
	 */
	public AttributeBinding locateAttributeBinding(String name);

	/**
	 * Locate a specific attribute binding, by its values.
	 *
	 * @param values The list of values
	 *
	 * @return The attribute binding or {@code null} if none could be found.
	 */
	public AttributeBinding locateAttributeBinding(List<Value> values);

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


	/**
	 * Return the table specification for the relational values of attributes
	 * directly contained by this attribute binding container.
	 *
	 * @return the table specification.
	 */
	public abstract TableSpecification getPrimaryTable();

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
	 * Factory method for one-to-one attribute bindings.
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param lazy
	 * @param metaAttributeContext
	 * @param referencedEntityBinding
	 * @param referencedAttributeBinding
	 * @param isConstrained
	 * @return The attribute binding instance.
	 */
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
			List<RelationalValueBinding> valueBindings);


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
			boolean isNotFoundAnException,
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
	 * Factory method for array attribute bindings.
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
	public ArrayBinding makeArrayAttributeBinding(
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
