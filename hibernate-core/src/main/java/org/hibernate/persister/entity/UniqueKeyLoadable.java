/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Gavin King
 */
public interface UniqueKeyLoadable extends Loadable {
	/**
	 * Load an instance of the persistent class, by a unique key other
	 * than the primary key.
	 */
	public Object loadByUniqueKey(String propertyName, Object uniqueKey, SessionImplementor session) 
	throws HibernateException;
	/**
	 * Get the property number of the unique key property
	 */
	public int getPropertyIndex(String propertyName);

}
