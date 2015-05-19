/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.loader.plan.spi.EntityQuerySpace;

/**
 * Describes an entity query space that allows adding joins with other
 * query spaces; used while building an {@link EntityQuerySpace}.
 *
 * @see org.hibernate.loader.plan.spi.Join
 *
 * @author Steve Ebersole
 */
public interface ExpandingEntityQuerySpace extends EntityQuerySpace, ExpandingQuerySpace {

	/**
	 * Builds a composite query space that allows adding joins used if
	 * the entity has a composite entity identifier.
	 *
	 * @return The expanding composite query space.
	 */
	public ExpandingCompositeQuerySpace makeCompositeIdentifierQuerySpace();
}
