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
import org.hibernate.tuple.component.ComponentTuplizer;

/**
 * A container for attribute bindings that make up composite grouping
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public abstract class AbstractEmbeddableBinding
		extends AbstractAttributeBindingContainer
		implements EmbeddableBindingImplementor {
	private final EntityBinding entityBinding;
	private final AttributeContainer attributeContainer;
	private final TableSpecification primaryTable;
	private final AttributeRole roleBase;
	private final AttributePath pathBase;
	private final MetaAttributeContext metaAttributeContext;
	private final SingularAttribute parentReference;
	private final Class<? extends ComponentTuplizer> tuplizerClass;

	protected AbstractEmbeddableBinding(
			EntityBinding entityBinding,
			AttributeContainer attributeContainer,
			TableSpecification primaryTable,
			AttributeRole roleBase,
			AttributePath pathBase,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference,
			Class<? extends ComponentTuplizer> tuplizerClass) {
		this.entityBinding = entityBinding;
		this.attributeContainer = attributeContainer;
		this.primaryTable = primaryTable;
		this.roleBase = roleBase;
		this.pathBase = pathBase;
		this.metaAttributeContext = metaAttributeContext;
		this.parentReference = parentReference;
		this.tuplizerClass = tuplizerClass;
	}

	public SingularAttribute getParentReference() {
		return parentReference;
	}

	@Override
	public AttributePath getPathBase() {
		return pathBase;
	}

	@Override
	public AttributeRole getRoleBase() {
		return roleBase;
	}

	@Override
	public AttributeContainer getAttributeContainer() {
		return attributeContainer;
	}

	@Override
	public EntityBinding seekEntityBinding() {
		return entityBinding;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	@Override
	public TableSpecification getPrimaryTable() {
		return primaryTable;
	}

	@Override
	public Class<? extends ComponentTuplizer> getCustomTuplizerClass() {
		return tuplizerClass;
	}

	protected abstract boolean isModifiable();

	@Override
	public RelationalValueBindingContainer getRelationalValueBindingContainer() {
		final RelationalValueBindingContainer bindingContainer = new RelationalValueBindingContainer();
		collectRelationalValueBindings( bindingContainer );
		return bindingContainer;
	}

	@Override
	public BasicAttributeBinding makeBasicAttributeBinding(
			SingularAttribute attribute,
			List<RelationalValueBinding> relationalValueBindings,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			PropertyGeneration generation) {
		if ( !isModifiable() ) {
			throw new UnsupportedOperationException( "Attribute bindings are read-only and cannot be modified." );
		}
		return super.makeBasicAttributeBinding(
				attribute,
				relationalValueBindings,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				attributeRole,
				attributePath,
				generation
		);
	}

	@Override
	public EmbeddedAttributeBinding makeAggregatedCompositeAttributeBinding(
			SingularAttribute attribute,
			SingularAttribute parentReferenceAttribute,
			Class<? extends ComponentTuplizer> tuplizerClass, String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath) {
		if ( !isModifiable() ) {
			throw new UnsupportedOperationException( "Attribute bindings are read-only and cannot be modified." );
		}
		return super.makeAggregatedCompositeAttributeBinding(
				attribute,
				parentReferenceAttribute,
				tuplizerClass,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				attributeRole,
				attributePath
		);
	}

	@Override
	public OneToOneAttributeBinding makeOneToOneAttributeBinding(
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
			boolean isConstrained) {
		if ( !isModifiable() ) {
			throw new UnsupportedOperationException( "Attribute bindings are read-only and cannot be modified." );
		}
		return super.makeOneToOneAttributeBinding(
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				attributeRole,
				attributePath,
				referencedEntityBinding,
				referencedAttributeBinding,
				isConstrained
		);
	}

	@Override
	public ManyToOneAttributeBinding makeManyToOneAttributeBinding(
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
			SingularAttributeBinding referencedAttributeBinding) {
		if ( !isModifiable() ) {
			throw new UnsupportedOperationException( "Attribute bindings are read-only and cannot be modified." );
		}
		return super.makeManyToOneAttributeBinding(
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				isIgnoreNotFound,
				naturalIdMutability,
				metaAttributeContext,
				attributeRole,
				attributePath,
				referencedEntityBinding,
				referencedAttributeBinding
		);
	}

	@Override
	public BagBinding makeBagAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath) {
		if ( !isModifiable() ) {
			throw new UnsupportedOperationException( "Attribute bindings are read-only and cannot be modified." );
		}
		return super.makeBagAttributeBinding(
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				attributeRole,
				attributePath
		);
	}

	@Override
	public ListBinding makeListAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			int base) {
		if ( !isModifiable() ) {
			throw new UnsupportedOperationException( "Attribute bindings are read-only and cannot be modified." );
		}
		return super.makeListAttributeBinding(
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				attributeRole,
				attributePath,
				base
		);
	}

	@Override
	public ArrayBinding makeArrayAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			int base) {
		if ( !isModifiable() ) {
			throw new UnsupportedOperationException( "Attribute bindings are read-only and cannot be modified." );
		}
		return super.makeArrayAttributeBinding(
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				attributeRole,
				attributePath,
				base
		);
	}

	@Override
	public MapBinding makeMapAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature elementNature,
			PluralAttributeIndexNature indexNature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath) {
		if ( !isModifiable() ) {
			throw new UnsupportedOperationException( "Attribute bindings are read-only and cannot be modified." );
		}
		return super.makeMapAttributeBinding(
				attribute,
				elementNature,
				indexNature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				attributeRole,
				attributePath
		);
	}

	@Override
	public SetBinding makeSetAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath) {
		if ( !isModifiable() ) {
			throw new UnsupportedOperationException( "Attribute bindings are read-only and cannot be modified." );
		}
		return super.makeSetAttributeBinding(
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				attributeRole,
				attributePath
		);
	}
}