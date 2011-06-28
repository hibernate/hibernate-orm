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
package org.hibernate.metamodel.source.annotations.attribute.state.binding;

import java.util.Map;
import java.util.Set;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.CascadeType;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.source.annotations.attribute.SimpleAttribute;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;

/**
 * Implementation of the attribute binding state via annotation configuration.
 *
 * @author Hardy Ferentschik
 * @todo in the end we can maybe just let MappedAttribute implement SimpleAttributeBindingState. (HF)
 */
public class AttributeBindingStateImpl implements SimpleAttributeBindingState {
	private final SimpleAttribute mappedAttribute;

	public AttributeBindingStateImpl(SimpleAttribute mappedAttribute) {
		this.mappedAttribute = mappedAttribute;
	}

	@Override
	public String getAttributeName() {
		return mappedAttribute.getName();
	}

	@Override
	public PropertyGeneration getPropertyGeneration() {
		return mappedAttribute.getPropertyGeneration();
	}

	@Override
	public boolean isInsertable() {
		return mappedAttribute.isInsertable();
	}

	@Override
	public boolean isUpdatable() {
		return mappedAttribute.isUpdatable();
	}

	@Override
	public String getTypeName() {
		return mappedAttribute.getType();
	}

	@Override
	public Map<String, String> getTypeParameters() {
		return mappedAttribute.getTypeParameters();
	}

	@Override
	public boolean isLazy() {
		return mappedAttribute.isLazy();
	}

	@Override
	public boolean isOptimisticLockable() {
		return mappedAttribute.isOptimisticLockable();
	}

	@Override
	public boolean isKeyCascadeDeleteEnabled() {
		return false;
	}

	// TODO find out more about these methods. How are they relevant for a simple attribute
	@Override
	public String getUnsavedValue() {
		return null;
	}

	@Override
	public String getPropertyAccessorName() {
		return null;
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return false;
	}

	@Override
	public Set<CascadeType> getCascadeTypes() {
		return null;
	}

	@Override
	public String getNodeName() {
		return null;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return null;
	}
}


