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
package org.hibernate.metamodel.domain;

import org.hibernate.internal.util.ValueHolder;

/**
 * Models the concept of a (intermediate) superclass
 *
 * @author Steve Ebersole
 */
public class Superclass extends AbstractAttributeContainer {
	/**
	 * Constructor for the entity
	 *
	 * @param entityName The name of the entity
	 * @param className The name of this entity's java class
	 * @param classReference The reference to this entity's {@link Class}
	 * @param superType The super type for this entity. If there is not super type {@code null} needs to be passed.
	 */
	public Superclass(String entityName, String className, ValueHolder<Class<?>> classReference, Hierarchical superType) {
		super( entityName, className, classReference, superType );
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	@Override
	public boolean isComponent() {
		return false;
	}
}
