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

import org.hibernate.metamodel.spi.PluralAttributeElementNature;


/**
 * Common "descriptor" for describing the elements of a persistent collection.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElementBinding {
	/**
	 * Retrieves the plural attribute binding descriptor whose element binding is described here.
	 *
	 * @return The plural attribute binding descriptor.
	 */
	PluralAttributeBinding getPluralAttributeBinding();

	/**
	 * Retrieves an enumeration describing the mapping nature of the collection's elements.
	 *
	 * @return The nature enum.
	 */
	PluralAttributeElementNature getNature();

	/**
	 * Retrieve the Hibernate type descriptor describing the mapping-typing of the elements.
	 *
	 * @return The element type descriptor.
	 */
	HibernateTypeDescriptor getHibernateTypeDescriptor();

	// not a fan of this, but for now...

	RelationalValueBindingContainer getRelationalValueContainer();
}
