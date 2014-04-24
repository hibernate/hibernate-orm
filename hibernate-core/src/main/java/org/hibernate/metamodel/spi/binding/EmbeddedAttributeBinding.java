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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.tuple.component.ComponentTuplizer;

/**
 * Models the binding information for embeddable/composite values.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class EmbeddedAttributeBinding
		extends AbstractSingularAttributeBinding
		implements SingularNonAssociationAttributeBinding, Cascadeable, EmbeddableBindingContributor {

	private final EmbeddableBindingImplementor embeddableBinding;

	public EmbeddedAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			EmbeddableBindingImplementor embeddableBinding) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				attributeRole,
				attributePath
		);
		this.embeddableBinding = embeddableBinding;

		getHibernateTypeDescriptor().setJavaTypeDescriptor( embeddableBinding.getAttributeContainer().getDescriptor() );
	}

	public static EmbeddedAttributeBinding createEmbeddedAttributeBinding(
			final AttributeBindingContainer container,
			final SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			SingularAttribute parentReference,
			Class<? extends ComponentTuplizer> tuplizerClass) {
		final AbstractEmbeddableBinding embeddableBinding = new AbstractEmbeddableBinding(
				container.seekEntityBinding(),
				(AttributeContainer) attribute.getSingularAttributeType(),
				container.getPrimaryTable(),
				attributeRole,
				attributePath,
				metaAttributeContext,
				parentReference,
				tuplizerClass) {
			private final Map<String,AttributeBinding> attributeBindingMap = new LinkedHashMap<String, AttributeBinding>();

			@Override
			protected boolean isModifiable() {
				return true;
			}

			@Override
			protected Map<String, AttributeBinding> attributeBindingMapInternal() {
				return attributeBindingMap;
			}

			@Override
			public boolean isAggregated() {
				return true;
			}

			@Override
			public JavaTypeDescriptor getTypeDescriptor() {
				return attribute.getSingularAttributeType().getDescriptor();
			}
		};

		if ( ! attribute.getSingularAttributeType().isAggregate() ) {
			throw new IllegalArgumentException(
					"Cannot create an aggregated EmbeddableBinding with a non-aggregate attribute type"
			);
		}
		return new EmbeddedAttributeBinding(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				attributeRole,
				attributePath,
				embeddableBinding
		);
	}
//
//	// TODO: Get rid of this when non-aggregated composite IDs is no longer modelled as a EmbeddedAttributeBinding.
//	public static EmbeddedAttributeBinding createVirtualEmbeddedAttributeBinding(
//			final AttributeBindingContainer container,
//			SingularAttribute syntheticAttribute,
//			NaturalIdMutability naturalIdMutability,
//			MetaAttributeContext metaAttributeContext,
//			AttributeRole attributeRole,
//			AttributePath attributePath,
//			final List<SingularAttributeBinding> subAttributeBindings) {
//		AbstractEmbeddableBinding embeddableBinding = new AbstractEmbeddableBinding(
//				container.seekEntityBinding(),
//				(AttributeContainer) syntheticAttribute.getSingularAttributeType(),
//				container.getPrimaryTable(),
//				attributeRole,
//				attributePath,
//				metaAttributeContext,
//				null,
//				null) {
//			private final Map<String, AttributeBinding> attributeBindingMap = createUnmodifiableAttributeBindingMap( subAttributeBindings );
//
//			@Override
//			protected boolean isModifiable() {
//				return false;
//			}
//
//			@Override
//			protected Map<String, AttributeBinding> attributeBindingMapInternal() {
//				return this.attributeBindingMap;
//			}
//
//			@Override
//			public boolean isAggregated() {
//				return false;
//			}
//		};
//
//		if ( syntheticAttribute.getSingularAttributeType().isAggregate() ) {
//			throw new IllegalArgumentException(
//					"Cannot create a non-aggregated EmbeddableBinding with an aggregate attribute type"
//			);
//		}
//		return new EmbeddedAttributeBinding(
//				container,
//				syntheticAttribute,
//				"embedded",  // TODO: get rid of "magic" string.
//				false,
//				false,
//				naturalIdMutability,
//				metaAttributeContext,
//				attributeRole,
//				attributePath,
//				embeddableBinding
//		);
//	}

	private static Map<String, AttributeBinding> createUnmodifiableAttributeBindingMap(
			List<SingularAttributeBinding> subAttributeBindings) {
		Map<String, AttributeBinding> map = new LinkedHashMap<String, AttributeBinding>( subAttributeBindings.size() );
		for ( AttributeBinding subAttributeBinding : subAttributeBindings ) {
			map.put( subAttributeBinding.getAttribute().getName(), subAttributeBinding );
		}
		return Collections.unmodifiableMap( map );
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	protected RelationalValueBindingContainer getRelationalValueBindingContainer() {
		return embeddableBinding.getRelationalValueBindingContainer();
	}

	@Override
	public boolean hasDerivedValue() {
		// todo : not sure this is even relevant for components
		return false;
	}

	@Override
	public boolean isCascadeable() {
		for ( AttributeBinding attributeBinding : embeddableBinding.attributeBindings() ) {
			if ( attributeBinding.isCascadeable() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public CascadeStyle getCascadeStyle() {
		for ( AttributeBinding attributeBinding : embeddableBinding.attributeBindings() ) {
			if ( attributeBinding.isCascadeable() ) {
				final Cascadeable cascadeable;
				if ( attributeBinding.getAttribute().isSingular() ) {
					cascadeable = Cascadeable.class.cast( attributeBinding );
				}
				else {
					cascadeable = Cascadeable.class.cast( ( (PluralAttributeBinding) attributeBinding ).getPluralAttributeElementBinding() );
				}
				CascadeStyle cascadeStyle = cascadeable.getCascadeStyle();
				if ( cascadeStyle != CascadeStyles.NONE ) {
					return CascadeStyles.ALL;
				}
			}
		}
		return CascadeStyles.NONE;
	}

	@Override
	public void setCascadeStyle(CascadeStyle cascadeStyle) {
		throw new IllegalAccessError( "Composite attribute is not supposed to have cascade" );
	}

	@Override
	public boolean isNullable() {
		// return false if there are any singular attributes are non-nullable
		for ( AttributeBinding attributeBinding : embeddableBinding.attributeBindings() ) {
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
		for ( AttributeBinding attributeBinding : embeddableBinding.attributeBindings() ) {
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
		for ( AttributeBinding attributeBinding : embeddableBinding.attributeBindings() ) {
			// only check singular attributes
			if ( attributeBinding.getAttribute().isSingular() &&
					( (SingularAttributeBinding) attributeBinding ).isIncludedInUpdate() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void collectRelationalValueBindings(RelationalValueBindingContainer relationalValueBindingContainer) {
		embeddableBinding.collectRelationalValueBindings( relationalValueBindingContainer );
	}

	@Override
	public EmbeddableBinding getEmbeddableBinding() {
		return embeddableBinding;
	}
}
