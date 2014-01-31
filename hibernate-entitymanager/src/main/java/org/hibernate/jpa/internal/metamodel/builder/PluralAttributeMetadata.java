/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal.metamodel.builder;

import javax.persistence.metamodel.PluralAttribute;

/**
 * Attribute metadata contract for a plural attribute.
 * @param <X> The owner type
 * @param <Y> The attribute type (the collection type)
 * @param <E> The collection element type
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
interface PluralAttributeMetadata<X,Y,E> extends AttributeMetadata<X,Y> {
	/**
	 * Retrieve the JPA collection type classification for this attribute
	 *
	 * @return The JPA collection type classification
	 */
	public PluralAttribute.CollectionType getAttributeCollectionType();

	/**
	 * Retrieve the value context for the collection's elements.
	 *
	 * @return The value context for the collection's elements.
	 */
	public AttributeTypeDescriptor getElementAttributeTypeDescriptor();

	/**
	 * Retrieve the value context for the collection's keys (if a map, null otherwise).
	 *
	 * @return The value context for the collection's keys (if a map, null otherwise).
	 */
	public AttributeTypeDescriptor getMapKeyAttributeTypeDescriptor();
}
