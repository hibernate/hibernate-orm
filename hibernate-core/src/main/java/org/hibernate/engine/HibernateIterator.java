/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine;

import java.util.Iterator;

import org.hibernate.JDBCException;

/**
 * Hibernate-specific iterator that may be closed
 *
 * @see org.hibernate.Query#iterate()
 * @see org.hibernate.Hibernate#close(java.util.Iterator)
 *
 * @author Gavin King
 */
public interface HibernateIterator extends Iterator, AutoCloseable {
	/**
	 * Close the Hibernate query result iterator
	 *
	 * @throws JDBCException Indicates a problem releasing the underlying JDBC resources.
	 */
	void close() throws JDBCException;
}
