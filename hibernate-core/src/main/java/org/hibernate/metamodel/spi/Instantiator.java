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
 * Strategy for instantiating a managed type
 *
 * @author Steve Ebersole
 */
@Incubating
public interface Instantiator {
	/**
	 * Performs and "instance of" check to see if the given object is an
	 * instance of managed structure
	 * @see Class#isInstance
	 */
	boolean isInstance(Object object, SessionFactoryImplementor sessionFactory);

	/**
	 * @see Class#equals
	 */
	boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory);}
