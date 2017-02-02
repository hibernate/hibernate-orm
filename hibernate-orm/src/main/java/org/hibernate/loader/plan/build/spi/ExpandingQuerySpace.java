/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.loader.plan.spi.Join;
import org.hibernate.loader.plan.spi.QuerySpace;

/**
 * Describes a query space that allows adding joins with other
 * query spaces; used while building a {@link QuerySpace}.
 *
 * @see org.hibernate.loader.plan.spi.Join
 *
 * @author Steve Ebersole
 */
public interface ExpandingQuerySpace extends QuerySpace {

	public boolean canJoinsBeRequired();

	/**
	 * Adds a join with another query space.
	 *
	 * @param join The join to add.
	 */
	public void addJoin(Join join);

	public ExpandingQuerySpaces getExpandingQuerySpaces();
}
