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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
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
		implements SingularNonAssociationAttributeBinding, AttributeBindingContainer {
	private final String path;
	private final SingularAttribute parentReference;
	private Map<String, AttributeBinding> attributeBindingMap;

	public CompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference) {
		this(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				parentReference,
				null
		);
	}

	public CompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			List<SingularAttributeBinding> subAttributeBindings) {
		this(
				container,
				attribute,
				propertyAccessorName,
				false,
				false,
				naturalIdMutability,
				metaAttributeContext,
				null,
				subAttributeBindings
		);
	}

	private CompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			SingularAttribute parentReference,
			List<SingularAttributeBinding> subAttributeBindings) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext
		);

		this.parentReference = parentReference;
		this.path = container.getPathBase() + '.' + attribute.getName();

		if ( subAttributeBindings == null ) {
			attributeBindingMap = new HashMap<String, AttributeBinding>();
		}
		else {
			HashMap<String, AttributeBinding> map = new HashMap<String, AttributeBinding>();
			for ( SingularAttributeBinding attributeBinding : subAttributeBindings ) {
				attributeBindingMap.put( attributeBinding.getAttribute().getName(), attributeBinding );
			}
			attributeBindingMap = Collections.unmodifiableMap( map );
		}
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
	public IdentifierGenerator createIdentifierGenerator(
			IdGenerator idGenerator, IdentifierGeneratorFactory factory, Properties properties) {
		// for now...
		return null;
	}

	@Override
	public AttributeBinding locateAttributeBinding(String name) {
		return attributeBindingMap.get( name );
	}

	@Override
	public AttributeBinding locateAttributeBinding(List<org.hibernate.metamodel.spi.relational.Value> values) {
		for(AttributeBinding attributeBinding : attributeBindingMap.values()) {
			if(!(attributeBinding instanceof BasicAttributeBinding)) {
				continue;
			}
			BasicAttributeBinding basicAttributeBinding = (BasicAttributeBinding) attributeBinding;
			if(basicAttributeBinding.getRelationalValueBindings().equals( values )) {
				return attributeBinding;
			}
		}
		return null;
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
			NaturalIdMutability naturalIdMutability,
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
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext) {
		final CompositeAttributeBinding binding = new CompositeAttributeBinding(
				this,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
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
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			SingularAttributeBinding referencedAttributeBinding,
			List<RelationalValueBinding> valueBindings) {
		final ManyToOneAttributeBinding binding = new ManyToOneAttributeBinding(
				this,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				referencedAttributeBinding,
				valueBindings
		);
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public BagBinding makeBagAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.BAG );
		final BagBinding binding = new BagBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext
		);
		registerAttributeBinding( attribute.getName(), binding );
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
				base );
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public MapBinding makeMapAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.MAP );
		final MapBinding binding = new MapBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext );
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public SetBinding makeSetAttributeBinding(
			PluralAttribute attribute,
			PluralAttributeElementNature nature,
			SingularAttributeBinding referencedAttributeBinding,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.SET );
		final SetBinding binding = new SetBinding(
				this,
				attribute,
				nature,
				referencedAttributeBinding,
				propertyAccessorName,
				includedInOptimisticLocking,
				metaAttributeContext
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
