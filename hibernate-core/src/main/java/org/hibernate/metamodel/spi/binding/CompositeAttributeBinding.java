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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.tuple.component.ComponentTuplizer;

/**
 * A specialized binding contract for a singular attribute binding that
 * contains other attribute bindings.
 *
 * @author Gail Badner
 */
public class CompositeAttributeBinding
		extends AbstractSingularAttributeBinding
		implements SingularNonAssociationAttributeBinding, CompositeAttributeBindingContainer, Cascadeable {

	private final AbstractCompositeAttributeBindingContainer compositeAttributeBindingContainer;
	private Class<? extends ComponentTuplizer> customComponentTuplizerClass = null;
	private CompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AbstractCompositeAttributeBindingContainer compositeAttributeBindingContainer) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext
		);
		this.compositeAttributeBindingContainer = compositeAttributeBindingContainer;
	}

	public static CompositeAttributeBinding createAggregatedCompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference) {
		AbstractCompositeAttributeBindingContainer compositeAttributeBindingContainer =
				new AbstractCompositeAttributeBindingContainer(
						container.seekEntityBinding(),
						(AttributeContainer) attribute.getSingularAttributeType(),
						container.getPrimaryTable(),
						createContainerPath( container, attribute ),
						metaAttributeContext,
						parentReference) {
					private final Map<String,AttributeBinding> attributeBindingMap =
							new LinkedHashMap<String, AttributeBinding>();

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
				};
		if ( ! attribute.getSingularAttributeType().isAggregate() ) {
			throw new IllegalArgumentException(
					"Cannot create an aggregated CompositeAttributeBindingContainer with a non-aggregate attribute type"
			);
		}
		return new CompositeAttributeBinding(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				compositeAttributeBindingContainer
		);
	}

	// TODO: Get rid of this when non-aggregated composite IDs is no longer modelled as a CompositeAttributeBinding.
	public static CompositeAttributeBinding createNonAggregatedCompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute syntheticAttribute,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			final List<SingularAttributeBinding> subAttributeBindings) {
		AbstractCompositeAttributeBindingContainer compositeAttributeBindingContainer =
				new AbstractCompositeAttributeBindingContainer(
						container.seekEntityBinding(),
						(AttributeContainer) syntheticAttribute.getSingularAttributeType(),
						container.getPrimaryTable(),
						createContainerPath( container, syntheticAttribute ),
						metaAttributeContext,
						null) {
					private final Map<String, AttributeBinding> attributeBindingMap = createUnmodifiableAttributeBindingMap( subAttributeBindings );

					@Override
					protected boolean isModifiable() {
						return false;
					}

					@Override
					protected Map<String, AttributeBinding> attributeBindingMapInternal() {
						return this.attributeBindingMap;
					}

					@Override
					public boolean isAggregated() {
						return false;
					}
				};
		if ( syntheticAttribute.getSingularAttributeType().isAggregate() ) {
			throw new IllegalArgumentException(
					"Cannot create a non-aggregated CompositeAttributeBindingContainer with an aggregate attribute type"
			);
		}
		return new CompositeAttributeBinding(
				container,
				syntheticAttribute,
				"embedded",  // TODO: get rid of "magic" string.
				false,
				false,
				naturalIdMutability,
				metaAttributeContext,
				compositeAttributeBindingContainer
		);
	}

	private static Map<String, AttributeBinding> createUnmodifiableAttributeBindingMap(
			List<SingularAttributeBinding> subAttributeBindings) {
		Map<String, AttributeBinding> map = new LinkedHashMap<String, AttributeBinding>( subAttributeBindings.size() );
		for ( AttributeBinding subAttributeBinding : subAttributeBindings ) {
			map.put( subAttributeBinding.getAttribute().getName(), subAttributeBinding );
		}
		return Collections.unmodifiableMap( map );
	}

	private static String createContainerPath(AttributeBindingContainer container, SingularAttribute attribute) {
		return StringHelper.isEmpty( container.getPathBase() ) ?
				attribute.getName() :
				container.getPathBase() + '.' + attribute.getName();

	}

	/**
	 * Can the composite attribute be mapped to a single entity
	 * attribute by means of an actual component class that aggregates
	 * the tuple values?
	 *
	 * @return true, if the attribute can be mapped to a single entity
	 * attribute by means of an actual component class that aggregates
	 * the tuple values; false, otherwise.
	 */
	public boolean isAggregated() {
		return compositeAttributeBindingContainer.isAggregated();
	}

	public SingularAttribute getParentReference() {
		return compositeAttributeBindingContainer.getParentReference();
	}

	@Override
	public Class<? extends ComponentTuplizer> getCustomTuplizerClass() {
		return customComponentTuplizerClass;
	}

	public void setCustomComponentTuplizerClass(Class<? extends ComponentTuplizer> customComponentTuplizerClass) {
		this.customComponentTuplizerClass = customComponentTuplizerClass;
	}

	@Override
	protected RelationalValueBindingContainer getRelationalValueBindingContainer() {
		return compositeAttributeBindingContainer.getRelationalValueBindingContainer();
	}

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
	public boolean isCascadeable() {
		for ( AttributeBinding attributeBinding : attributeBindings() ) {
			if ( attributeBinding.isCascadeable() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public CascadeStyle getCascadeStyle() {
		for ( AttributeBinding attributeBinding : attributeBindings() ) {
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

	@Override
	protected void collectRelationalValueBindings(RelationalValueBindingContainer relationalValueBindingContainer) {
		compositeAttributeBindingContainer.collectRelationalValueBindings( relationalValueBindingContainer );
	}

	@Override
	public String getPathBase() {
		return compositeAttributeBindingContainer.getPathBase();
	}

	@Override
	public AttributeContainer getAttributeContainer() {
		return compositeAttributeBindingContainer.getAttributeContainer();
	}

	@Override
	public Iterable<AttributeBinding> attributeBindings() {
		return compositeAttributeBindingContainer.attributeBindings();
	}

	@Override
	public int attributeBindingSpan() {
		return compositeAttributeBindingContainer.attributeBindingSpan();
	}

	@Override
	public AttributeBinding locateAttributeBinding(String name) {
		return compositeAttributeBindingContainer.locateAttributeBinding( name );
	}

	@Override
	public SingularAttributeBinding locateAttributeBinding(
			TableSpecification table,
			List<? extends Value> values) {
		return compositeAttributeBindingContainer.locateAttributeBinding( table, values );
	}

	@Override
	public EntityBinding seekEntityBinding() {
		return compositeAttributeBindingContainer.seekEntityBinding();
	}

	@Override
	public Class<?> getClassReference() {
		return compositeAttributeBindingContainer.getClassReference();
	}

	@Override
	public TableSpecification getPrimaryTable() {
		return compositeAttributeBindingContainer.getPrimaryTable();
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
			PropertyGeneration generation) {
		return compositeAttributeBindingContainer.makeBasicAttributeBinding(
				attribute,
				relationalValueBindings,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				generation
		);
	}

	@Override
	public CompositeAttributeBinding makeAggregatedCompositeAttributeBinding(
			SingularAttribute attribute,
			SingularAttribute parentReferenceAttribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext) {
		return compositeAttributeBindingContainer.makeAggregatedCompositeAttributeBinding(
				attribute,
				parentReferenceAttribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext
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
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding,
			boolean isConstrained) {
		return compositeAttributeBindingContainer.makeOneToOneAttributeBinding(
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
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
			boolean isNotFoundAnException,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding) {
		return compositeAttributeBindingContainer.makeManyToOneAttributeBinding(
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				isNotFoundAnException,
				naturalIdMutability,
				metaAttributeContext,
				referencedEntityBinding,
				referencedAttributeBinding
		);
	}

	@Override
	public BagBinding makeBagAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		return compositeAttributeBindingContainer.makeBagAttributeBinding(
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext
		);
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
		return compositeAttributeBindingContainer.makeListAttributeBinding(
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				base
		);
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
		return compositeAttributeBindingContainer.makeArrayAttributeBinding(
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext,
				base
		);
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
		return compositeAttributeBindingContainer.makeMapAttributeBinding(
				attribute,
				elementNature,
				indexNature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext
		);
	}

	@Override
	public SetBinding makeSetAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementBinding.Nature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		return compositeAttributeBindingContainer.makeSetAttributeBinding(
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext
		);
	}
}
