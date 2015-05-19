/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.loader.plan.spi.CompositeQuerySpace;

/**
 * Describes a composite query space that allows adding joins with other
 * query spaces; used while building a {@link CompositeQuerySpace}.
 *
 * @see org.hibernate.loader.plan.spi.Join

 * @author Gail Badner
 */
public interface ExpandingCompositeQuerySpace extends CompositeQuerySpace, ExpandingQuerySpace {
}
