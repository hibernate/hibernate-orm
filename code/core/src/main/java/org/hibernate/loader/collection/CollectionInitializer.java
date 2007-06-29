//$Id: CollectionInitializer.java 7123 2005-06-13 20:10:20Z oneovthafew $
package org.hibernate.loader.collection;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * An interface for collection loaders
 * @see BasicCollectionLoader
 * @see OneToManyLoader
 * @author Gavin King
 */
public interface CollectionInitializer {
	/**
	 * Initialize the given collection
	 */
	public void initialize(Serializable id, SessionImplementor session) throws HibernateException;
}






