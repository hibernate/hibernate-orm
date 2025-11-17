/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.context.spi;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 * Defines the contract for objects which are able to manage the lifecycle
 * of a {@link Session} associated with a well-defined "context" or "scope",
 * providing the concrete implementation behind the notion of the
 * {@linkplain org.hibernate.SessionFactory#getCurrentSession() current session}.
 * <p>
 * The lifecycle of the context/scope is not specified by Hibernate, and
 * varies depending on the nature of the program. The only hard restriction
 * is that the scope must be single-threaded. On the other hand, since state
 * tends to build up in the first-level cache of session, sessions should
 * typically not be associated with long-lived scopes.
 * <p>
 * The most typical example of a scope with which a current session might
 * be associated is the HTTP request context in a web application.
 * <p>
 * An implementation of this interface must:
 * <ul>
 * <li>have a constructor accepting a single argument of type
 *     {@link org.hibernate.engine.spi.SessionFactoryImplementor},
 * <li>be thread-safe,
 * <li>be fully serializable,
 * <li>guarantee that it {@link Session#close() destroys} every session it
 *    creates, and
 * <li>ensure that each session it creates is the exclusive property of a
 *     single thread, since sessions contain fragile mutable state and are
 *     <em>never</em> considered thread-safe.
 * </ul>
 * <p>
 * Every {@linkplain org.hibernate.SessionFactory session factory} has
 * exactly one instance of this interface.
 * <p>
 * An implementation may be selected by setting the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#CURRENT_SESSION_CONTEXT_CLASS}.
 *
 * @see org.hibernate.SessionFactory#getCurrentSession()
 * @see org.hibernate.cfg.AvailableSettings#CURRENT_SESSION_CONTEXT_CLASS
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
	Session currentSession() throws HibernateException;
}
