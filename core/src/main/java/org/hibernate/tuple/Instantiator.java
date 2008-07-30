/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tuple;


import java.io.Serializable;

/**
 * Contract for implementors responsible for instantiating entity/component instances.
 *
 * @author Steve Ebersole
 */
public interface Instantiator extends Serializable {

	/**
	 * Perform the requested entity instantiation.
	 * <p/>
	 * This form is never called for component instantiation, only entity instantiation.
	 *
	 * @param id The id of the entity to be instantiated.
	 * @return An appropriately instantiated entity.
	 */
	public Object instantiate(Serializable id);

	/**
	 * Perform the requested instantiation.
	 *
	 * @return The instantiated data structure. 
	 */
	public Object instantiate();

	/**
	 * Performs check to see if the given object is an instance of the entity
	 * or component which this Instantiator instantiates.
	 *
	 * @param object The object to be checked.
	 * @return True is the object does respresent an instance of the underlying
	 * entity/component.
	 */
	public boolean isInstance(Object object);
}
