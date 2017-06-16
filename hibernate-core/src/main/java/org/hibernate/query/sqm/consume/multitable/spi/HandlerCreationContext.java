/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Parameter object (pattern) for contextual information possibly needed in
 * {@link IdTableStrategy#buildUpdateHandler} and
 * {@link IdTableStrategy#buildDeleteHandler}
 */
public interface HandlerCreationContext {
	/**
	 * Access to the SessionFactory
	 */
	SessionFactoryImplementor getSessionFactory();
}
