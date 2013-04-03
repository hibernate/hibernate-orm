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
package org.hibernate.loader.plan.spi;

import java.util.List;

/**
 * Describes a plan for performing a load of results.
 *
 * Generally speaking there are 3 forms of load plans:<ul>
 *     <li>
 *         An entity load plan for handling get/load handling.  This form will typically have a single
 *         return (of type {@link EntityReturn}) defined by {@link #getReturns()}, possibly defining fetches.
 *     </li>
 *     <li>
 *         A collection initializer, used to load the contents of a collection.  This form will typically have a
 *         single return (of type {@link CollectionReturn} defined by {@link #getReturns()}, possibly defining fetches
 *     </li>
 *     <li>
 *         A query load plan which can contain multiple returns of mixed type (though implementing {@link Return}).
 *         Again, may possibly define fetches.
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface LoadPlan {
	/**
	 * Convenient form of checking {@link #getReturns()} for scalar root returns.
	 *
	 * @return {@code true} if {@link #getReturns()} contained any scalar returns; {@code false} otherwise.
	 */
	public boolean hasAnyScalarReturns();

	public List<Return> getReturns();

	// todo : would also like to see "call back" style access for handling "subsequent actions" such as:
	// 		1) follow-on locking
	//		2) join fetch conversions to subselect fetches
}
