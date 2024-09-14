/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract for instantiating embeddable values.
 *
 * @apiNote Incubating until the proposed
 * {@code instantiate(IntFunction valueAccess, SessionFactoryImplementor sessionFactory)}
 * form can be implemented.
 *
 * @see org.hibernate.annotations.EmbeddableInstantiator
 */
@Incubating
public interface EmbeddableInstantiator extends Instantiator {
	/**
	 * Create an instance of the embeddable
	 */
	Object instantiate(ValueAccess valueAccess, SessionFactoryImplementor sessionFactory);

}
