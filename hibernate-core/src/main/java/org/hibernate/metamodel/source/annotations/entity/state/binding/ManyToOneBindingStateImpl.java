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
package org.hibernate.metamodel.source.annotations.entity.state.binding;

import java.util.Set;

import org.hibernate.metamodel.binding.CascadeType;
import org.hibernate.metamodel.binding.state.ManyToOneAttributeBindingState;
import org.hibernate.metamodel.source.annotations.entity.AssociationAttribute;

/**
 * @author Hardy Ferentschik
 */
public class ManyToOneBindingStateImpl extends AttributeBindingStateImpl implements ManyToOneAttributeBindingState {
	private final AssociationAttribute associationAttribute;

	public ManyToOneBindingStateImpl(AssociationAttribute associationAttribute) {
		super( associationAttribute );
		this.associationAttribute = associationAttribute;
	}

	@Override
	public boolean isLazy() {
		return associationAttribute.isLazy();
	}

	@Override
	public Set<CascadeType> getCascadeTypes() {
		return null;
	}

	@Override
	public String getReferencedEntityName() {
		return associationAttribute.getReferencedEntityType();
	}

	@Override
	public boolean ignoreNotFound() {
		return associationAttribute.isIgnoreNotFound();
	}

	@Override
	public boolean isUnwrapProxy() {
		return false;
	}

	@Override
	public String getReferencedAttributeName() {
		return null;
	}
}


