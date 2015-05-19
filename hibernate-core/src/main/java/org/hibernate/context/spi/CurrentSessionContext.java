/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.context.spi;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 * Defines the contract for implementations which know how to scope the notion
 * of a {@link org.hibernate.SessionFactory#getCurrentSession() current session}.
 * <p/>
 * Implementations should adhere to the following:
 * <ul>
 * <li>contain a constructor accepting a single argument of type
 * {@link org.hibernate.engine.spi.SessionFactoryImplementor}
 * <li>should be thread safe
 * <li>should be fully serializable
 * </ul>
 * <p/>
 * Implementors should be aware that they are also fully responsible for
 * cleanup of any generated current-sessions.
 * <p/>
 * Note that there will be exactly one instance of the configured
 * CurrentSessionContext implementation per {@link org.hibernate.SessionFactory}.
 *
 * @author Steve Ebersole
 */
public interface CurrentSessionContext extends Serializable {
	/**
	 * Retrieve the current session according to the scoping defined
	 * by this implementation.
	 *
	 * @return The current session.
	 * @throws HibernateException Typically indicates an issue
	 * locating or creating the current session.
	 */
	public Session currentSession() throws HibernateException;
}
