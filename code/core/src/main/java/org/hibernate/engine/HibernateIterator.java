//$Id: HibernateIterator.java 4782 2004-11-21 00:11:27Z pgmjsd $
package org.hibernate.engine;

import org.hibernate.JDBCException;

import java.util.Iterator;

/**
 * An iterator that may be "closed"
 * @see org.hibernate.Hibernate#close(java.util.Iterator)
 * @author Gavin King
 */
public interface HibernateIterator extends Iterator {
	public void close() throws JDBCException;
}
