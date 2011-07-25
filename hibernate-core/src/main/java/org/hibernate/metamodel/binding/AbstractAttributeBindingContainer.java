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

import org.hibernate.metamodel.domain.PluralAttribute;
import org.hibernate.metamodel.domain.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeBindingContainer implements AttributeBindingContainer {
	private Map<String, AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();

	protected void registerAttributeBinding(String name, AttributeBinding attributeBinding) {
		attributeBindingMap.put( name, attributeBinding );
	}

	@Override
	public SimpleSingularAttributeBinding makeSimpleAttributeBinding(SingularAttribute attribute) {
		return makeSimpleAttributeBinding( attribute, false, false );
	}

	private SimpleSingularAttributeBinding makeSimpleAttributeBinding(SingularAttribute attribute, boolean forceNonNullable, boolean forceUnique) {
		final SimpleSingularAttributeBinding binding = new SimpleSingularAttributeBinding(
				this,
				attribute,
				forceNonNullable,
				forceUnique
		);
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
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
		final BagBinding binding = new BagBinding( this, attribute, nature );
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	@Override
	public AttributeBinding locateAttributeBinding(String name) {
		return attributeBindingMap.get( name );
	}

	@Override
	public Iterable<AttributeBinding> attributeBindings() {
		return attributeBindingMap.values();
	}

	/**
	 * Gets the number of attribute bindings defined on this class, including the
	 * identifier attribute binding and attribute bindings defined
	 * as part of a join.
	 *
	 * @return The number of attribute bindings
	 */
	public int getAttributeBindingClosureSpan() {
		// TODO: fix this after HHH-6337 is fixed; for now just return size of attributeBindingMap
		// if this is not a root, then need to include the superclass attribute bindings
		return attributeBindingMap.size();
	}

	/**
	 * Gets the attribute bindings defined on this class, including the
	 * identifier attribute binding and attribute bindings defined
	 * as part of a join.
	 *
	 * @return The attribute bindings.
	 */
	public Iterable<AttributeBinding> getAttributeBindingClosure() {
		// TODO: fix this after HHH-6337 is fixed. for now, just return attributeBindings
		// if this is not a root, then need to include the superclass attribute bindings
		return attributeBindings();
	}
}
