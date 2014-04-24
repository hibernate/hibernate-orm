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
import java.util.Map;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;
import org.hibernate.metamodel.spi.PluralAttributeNature;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.tuple.component.ComponentTuplizer;

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
	public SingularAttributeBinding locateAttributeBinding(TableSpecification table, List<? extends Value> values) {
		for ( AttributeBinding attributeBinding : attributeBindingMapInternal().values() ) {
			if ( !attributeBinding.getAttribute().isSingular() ) {
				continue;
			}
			SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;
			if ( hasEqualValues( table, values, singularAttributeBinding.getRelationalValueBindings() ) ) {
				return singularAttributeBinding;
			}
		}
		return null;
	}

	private static boolean hasEqualValues(
			TableSpecification table,
			List<? extends Value> values,
			List<RelationalValueBinding> relationalValueBindings) {
		if ( values.size() != relationalValueBindings.size() ) {
			return false;
		}
		for ( int i = 0 ; i < values.size() ; i++ ) {
			final RelationalValueBinding relationalValueBinding = relationalValueBindings.get( i );
			if ( !table.equals( relationalValueBinding.getTable() ) ||
					!values.get( i ).equals( relationalValueBinding.getValue() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int attributeBindingSpan() {
		return attributeBindingMapInternal().size();
	}

	@Override
	public Iterable<AttributeBinding> attributeBindings() {
		return attributeBindingMapInternal().values();
	}

	public void collectRelationalValueBindings(RelationalValueBindingContainer relationalValueBindingContainer) {
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
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
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
				attributeRole,
				attributePath,
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
	public EmbeddedAttributeBinding makeAggregatedCompositeAttributeBinding(
			SingularAttribute attribute,
			SingularAttribute parentReferenceAttribute,
			Class<? extends ComponentTuplizer> tuplizerClass,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath) {
		final EmbeddedAttributeBinding binding = EmbeddedAttributeBinding.createEmbeddedAttributeBinding(
				this,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				attributeRole,
				attributePath,
				parentReferenceAttribute,
				tuplizerClass
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
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding,
			boolean isConstrained) {
		final OneToOneAttributeBinding binding = new OneToOneAttributeBinding(
				this,
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
		registerAttributeBinding( binding );
		return binding;
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
		final ManyToOneAttributeBinding binding = new ManyToOneAttributeBinding(
				this,
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
		registerAttributeBinding( binding );
		return binding;
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
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.BAG );
		final BagBinding binding = new BagBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				attributeRole,
				attributePath
		);
		registerAttributeBinding( binding );
		return binding;
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
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.LIST );
		final ListBinding binding = new ListBinding(
				this,
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
		registerAttributeBinding( binding );
		return binding;
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
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.ARRAY );
		final ArrayBinding binding = new ArrayBinding(
				this,
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
		registerAttributeBinding( binding );
		return binding;
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
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.MAP );
		final MapBinding binding = new MapBinding(
				this,
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
		registerAttributeBinding( binding );
		return binding;
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
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.SET );
		final SetBinding binding = new SetBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				attributeRole,
				attributePath
		);
		registerAttributeBinding( binding );
		return binding;
	}
}
