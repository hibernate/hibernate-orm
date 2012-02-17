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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.Composite;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.PluralAttributeNature;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * @author Steve Ebersole
 */
public class CompositeAttributeBinding
		extends AbstractSingularAttributeBinding
		implements AttributeBindingContainer {
	private final String path;
	private final SingularAttribute parentReference;
	private Map<String, AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();

	public CompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				metaAttributeContext
		);
		this.parentReference = parentReference;
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
		return getComponent();
	}

	public Composite getComponent() {
		return (Composite) getAttribute().getSingularAttributeType();
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
	public boolean isNullable() {
		// todo : not sure this is even relevant for components
		return false;
	}

	@Override
	public AttributeBinding locateAttributeBinding(String name) {
		return attributeBindingMap.get( name );
	}

	@Override
	public Iterable<AttributeBinding> attributeBindings() {
		return attributeBindingMap.values();
	}

	@Override
	public BasicAttributeBinding makeBasicAttributeBinding(
			SingularAttribute attribute,
			List<RelationalValueBinding> relationalValueBindings,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext,
			PropertyGeneration generation) {
		final BasicAttributeBinding binding = new BasicAttributeBinding(
				this,
				attribute,
				relationalValueBindings,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				metaAttributeContext,
				generation
		);
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	protected void registerAttributeBinding(String name, AttributeBinding attributeBinding) {
		// todo : hook this into the EntityBinding notion of "entity referencing attribute bindings"
		attributeBindingMap.put( name, attributeBinding );
	}

	@Override
	public CompositeAttributeBinding makeComponentAttributeBinding(
			SingularAttribute attribute,
			SingularAttribute parentReferenceAttribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext) {
		final CompositeAttributeBinding binding = new CompositeAttributeBinding(
				this,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				metaAttributeContext,
				parentReferenceAttribute
		);
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public ManyToOneAttributeBinding makeManyToOneAttributeBinding(
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext,
			String referencedEntityName,
			String referencedEntityAttributeName,
			List<RelationalValueBinding> valueBindings) {
		final ManyToOneAttributeBinding binding = new ManyToOneAttributeBinding(
				this,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				metaAttributeContext,
				valueBindings
		);
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public BagBinding makeBagAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.BAG );
		final BagBinding binding = new BagBinding(
				this,
				attribute,
				nature,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				metaAttributeContext
		);
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public SetBinding makeSetAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext,
			Comparator comparator) {
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.SET );
		final SetBinding binding = new SetBinding(
				this,
				attribute,
				nature,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				metaAttributeContext,
				comparator
		);
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public Class<?> getClassReference() {
		return getComponent().getClassReference();
	}

	public SingularAttribute getParentReference() {
		return parentReference;
	}
}
