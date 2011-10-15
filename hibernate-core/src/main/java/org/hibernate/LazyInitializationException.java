/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate;
import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;

/**
 * Indicates access to unfetched data outside of a session context.
 * For example, when an uninitialized proxy or collection is accessed
 * after the session was closed.
 *
 * @see Hibernate#initialize(java.lang.Object)
 * @see Hibernate#isInitialized(java.lang.Object)
 * @author Gavin King
 */
public class LazyInitializationException extends HibernateException {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, LazyInitializationException.class.getName() );

	public LazyInitializationException(String msg) {
		super( msg );
		LOG.trace( msg, this );
	}

}
