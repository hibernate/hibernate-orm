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
 * Lesser General License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.binding;

import java.util.List;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.tuple.Tuplizer;
import org.hibernate.tuple.component.ComponentTuplizer;

/**
 * Common contract for {@link EntityBinding} and {@link EmbeddedAttributeBinding} in so far as they are both
 * containers for {@link AttributeBinding} descriptors
 *
 * @author Steve Ebersole
 */
public interface AttributeBindingContainer {
	public AttributePath getPathBase();
	public AttributeRole getRoleBase();

	/**
	 * Obtain the underlying domain attribute container.
	 *
	 * @return The attribute container
	 */
	AttributeContainer getAttributeContainer();

	/**
	 * Obtain all attribute bindings
	 *
	 * @return All attribute bindings
	 */
	Iterable<AttributeBinding> attributeBindings();

	/**
	 * Return the number of attribute bindings returned by
	 * {@link #attributeBindings()}.
	 *
	 * @return the number of attribute bindings returned by
	 * {@link #attributeBindings()}.
	 */
	int attributeBindingSpan();

	/**
	 * Return the custom tuplizer class.
	 * @return
	 */
	Class<? extends Tuplizer> getCustomTuplizerClass();

	/**
	 * Locate a specific attribute binding, by its local name.
	 *
	 * @param name The name of the attribute, local to this container.
	 *
	 * @return The attribute binding or {@code null} if none could be found.
	 */
	AttributeBinding locateAttributeBinding(String name);

	/**
	 * Locate a specific attribute binding, by its values.
	 *
	 * @param table The table containing the values
	 * @param values The list of values
	 *
	 * @return The attribute binding or {@code null} if none could be found.
	 */
	SingularAttributeBinding locateAttributeBinding(TableSpecification table, List<? extends Value> values);

	/**
	 * Seeks out the entity binding that is the root of this component path.
	 *
	 * @return The entity binding
	 */
	EntityBinding seekEntityBinding();

	/**
	 * Obtain the meta-attribute context for this container.
	 *
	 * @return The meta-attribute context.
	 */
	MetaAttributeContext getMetaAttributeContext();


	/**
	 * Return the table specification for the relational values of attributes
	 * directly contained by this attribute binding container.
	 *
	 * @return the table specification.
	 */
	TableSpecification getPrimaryTable();

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
	BasicAttributeBinding makeBasicAttributeBinding(
			SingularAttribute attribute,
			List<RelationalValueBinding> relationalValueBindings,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			PropertyGeneration generation);

	/**
	 * Factory method for component attribute bindings.
	 *
	 * @param attribute The attribute for which to make a binding.
	 * @param parentReferenceAttribute
	 * @param tuplizerClass
	 * @param propertyAccessorName
	 * @param includedInOptimisticLocking
	 * @param lazy
	 * @param metaAttributeContext
	 *
	 * @return The attribute binding instance.
	 */
	EmbeddedAttributeBinding makeAggregatedCompositeAttributeBinding(
			SingularAttribute attribute,
			SingularAttribute parentReferenceAttribute,
			Class<? extends ComponentTuplizer> tuplizerClass,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath);

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
	OneToOneAttributeBinding makeOneToOneAttributeBinding(
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding,
			boolean isConstrained);


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
	 *
	 * @return The attribute binding instance.
	 */
	ManyToOneAttributeBinding makeManyToOneAttributeBinding(
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			boolean isIgnoreNotFound,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding);

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
	BagBinding makeBagAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath);

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
	ListBinding makeListAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
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
	ArrayBinding makeArrayAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
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
	MapBinding makeMapAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature elementNature,
			PluralAttributeIndexNature indexNature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath);

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
	SetBinding makeSetAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath);
}
