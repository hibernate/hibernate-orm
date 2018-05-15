/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Strategy for instantiating representation structure instances.
 *
 * @author Steve Ebersole
 */
public interface Instantiator<J> {
	/**
	 * Create an instance of the managed embedded value structure.
	 */
	J instantiate(SharedSessionContractImplementor session);

	/**
	 * Performs and "instance of" check to see if the given object is an
	 * instance of managed structure
	 */
	boolean isInstance(Object object, SessionFactoryImplementor sessionFactory);
}
