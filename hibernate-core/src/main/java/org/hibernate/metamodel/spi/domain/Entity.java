/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in nthe hope that it will be useful,
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
package org.hibernate.metamodel.spi.domain;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;

/**
 * Models the notion of an entity
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class Entity extends AbstractAttributeContainer implements IdentifiableType {
	/**
	 * Constructor for the entity
	 *
	 * @param typeDescriptor The reflite {@link org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor} describing this entity's class
	 * @param superType The super type for this entity. If there is not super type {@code null} needs to be passed.
	 */
	public Entity(JavaTypeDescriptor typeDescriptor, Hierarchical superType) {
		super( typeDescriptor, superType );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Specifically here we are returning the name of the java type.  This is
	 * not (usually) the same as the entity-name.
	 */
	@Override
	public String getName() {
		return super.getName();
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	@Override
	public boolean isAggregate() {
		return false;
	}

	@Override
	public String getRoleBaseName() {
		return getName();
	}
}
