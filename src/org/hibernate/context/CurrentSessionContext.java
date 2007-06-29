package org.hibernate.context;

import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * Defines the contract for implementations which know how to
 * scope the notion of a {@link org.hibernate.SessionFactory#getCurrentSession() current session}.
 * <p/>
 * Implementations should adhere to the following:
 * <ul>
 * <li>contain a constructor accepting a single argument of type
 * {@link org.hibernate.engine.SessionFactoryImplementor}
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
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public interface CurrentSessionContext extends Serializable {
	/**
	 * Retrieve the current session according to the scoping defined
	 * by this implementation.
	 *
	 * @return The current session.
	 * @throws org.hibernate.HibernateException Typically indicates an issue
	 * locating or creating the current session.
	 */
	public org.hibernate.classic.Session currentSession() throws HibernateException;
}
