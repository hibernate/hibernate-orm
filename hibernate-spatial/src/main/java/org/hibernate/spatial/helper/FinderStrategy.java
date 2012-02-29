/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.hibernate.spatial.helper;

/**
 * A <code>FinderStrategy</code> is used to find a specific feature. It is
 * useful in cases where reflection is used to determine some property of a
 * class.
 *
 * @param <T> the return type of the <code>find</code> method
 * @param <S> the type of subject
 * @author Karel Maesen
 */
public interface FinderStrategy<T, S> {

	/**
	 * Find a feature or property of a subject
	 *
	 * @param subject the object that is being searched
	 * @return the object sought
	 * @throws FinderException thrown when the feature can be found;
	 */
	public T find(S subject) throws FinderException;

}
