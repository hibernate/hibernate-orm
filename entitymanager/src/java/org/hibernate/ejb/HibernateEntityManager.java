//$Id$
package org.hibernate.ejb;

import javax.persistence.EntityManager;

import org.hibernate.Session;

/**
 * @author Gavin King
 */
public interface HibernateEntityManager extends EntityManager {
	public Session getSession();
}
