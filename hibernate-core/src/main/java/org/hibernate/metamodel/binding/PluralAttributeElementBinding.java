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
package org.hibernate.metamodel.binding;

import org.hibernate.metamodel.relational.Value;

/**
 * Common information pertaining to the binding of the various plural attribute natures (one-to-many, basic, etc).
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElementBinding {
	/**
	 * Retrieves the plural attribute binding descriptor whose element binding is described here.
	 *
	 * @return The plural attribute binding descriptor.
	 */
	public PluralAttributeBinding getPluralAttributeBinding();

	/**
	 * Retrieve the relational aspect of the element binding. Essentially describes the column(s) to which the
	 * binding maps the elements
	 *
	 * @return The relation information.
	 */
	public Value getRelationalValue();

	/**
	 * Retrieves an enumeration describing the mapping nature of the collection's elements.
	 * 
	 * @return The nature enum.
	 */
	public PluralAttributeElementNature getPluralAttributeElementNature();

	/**
	 * Retrieve the Hibernate type descriptor describing the mapping-typing of the elements.
	 *
	 * @return The element type descriptor.
	 */
	public HibernateTypeDescriptor getHibernateTypeDescriptor();
}
