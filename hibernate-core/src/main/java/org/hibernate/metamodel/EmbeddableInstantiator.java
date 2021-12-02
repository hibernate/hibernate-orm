/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel;

import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.Instantiator;

/**
 * Contract for instantiating embeddable values
 * 
 * NOTE : incubating until the proposed 
 * `instantiate(IntFunction<Object> valueAccess, SessionFactoryImplementor sessionFactory)`
 * form can be implemented
 *
 * @see org.hibernate.annotations.EmbeddableInstantiator
 */
@Incubating
public interface EmbeddableInstantiator extends Instantiator {
	/**
	 * Create an instance of the embeddable
	 */
	Object instantiate(Supplier<Object[]> valuesAccess, SessionFactoryImplementor sessionFactory);

//	default Object instantiate(IntFunction<Object> valueAccess, SessionFactoryImplementor sessionFactory) {
//		throw new NotYetImplementedFor6Exception( getClass() );
//	}
}
