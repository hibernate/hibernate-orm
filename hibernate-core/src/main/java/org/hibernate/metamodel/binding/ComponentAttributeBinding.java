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
package org.hibernate.metamodel.binding;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.domain.AttributeContainer;
import org.hibernate.metamodel.domain.Component;
import org.hibernate.metamodel.domain.PluralAttribute;
import org.hibernate.metamodel.domain.PluralAttributeNature;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.source.MetaAttributeContext;

/**
 * @author Steve Ebersole
 */
public class ComponentAttributeBinding extends AbstractSingularAttributeBinding implements AttributeBindingContainer {
	private final String path;
	private Map<String, AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();
	private SingularAttribute parentReference;
	private MetaAttributeContext metaAttributeContext;

	public ComponentAttributeBinding(AttributeBindingContainer container, SingularAttribute attribute) {
		super( container, attribute );
		this.path = container.getPathBase() + '.' + attribute.getName();
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

	public Component getComponent() {
		return (Component) getAttribute().getSingularAttributeType();
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	public void setMetaAttributeContext(MetaAttributeContext metaAttributeContext) {
		this.metaAttributeContext = metaAttributeContext;
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
	protected void checkValueBinding() {
		// do nothing here...
	}

	@Override
	public BasicAttributeBinding makeBasicAttributeBinding(SingularAttribute attribute) {
		final BasicAttributeBinding binding = new BasicAttributeBinding(
				this,
				attribute,
				isNullable(),
				isAlternateUniqueKey() // todo : is this accurate?
		);
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	protected void registerAttributeBinding(String name, AttributeBinding attributeBinding) {
		// todo : hook this into the EntityBinding notion of "entity referencing attribute bindings"
		attributeBindingMap.put( name, attributeBinding );
	}

	@Override
	public ComponentAttributeBinding makeComponentAttributeBinding(SingularAttribute attribute) {
		final ComponentAttributeBinding binding = new ComponentAttributeBinding( this, attribute );
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public ManyToOneAttributeBinding makeManyToOneAttributeBinding(SingularAttribute attribute) {
		final ManyToOneAttributeBinding binding = new ManyToOneAttributeBinding( this, attribute );
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public BagBinding makeBagAttributeBinding(PluralAttribute attribute, CollectionElementNature nature) {
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.BAG );
		final BagBinding binding = new BagBinding( this, attribute, nature );
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public SetBinding makeSetAttributeBinding(PluralAttribute attribute, CollectionElementNature nature) {
		Helper.checkPluralAttributeNature( attribute, PluralAttributeNature.SET );
		final SetBinding binding = new SetBinding( this, attribute, nature );
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

	public void setParentReference(SingularAttribute parentReference) {
		this.parentReference = parentReference;
	}

	@Override
	public PropertyGeneration getGeneration() {
		// todo : not sure the correct thing to return here since it essentially relies on the simple sub-attributes.
		return null;
	}
}
