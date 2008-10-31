//$Id$
package org.hibernate.ejb;

import java.io.Serializable;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;

/**
 * @author Gavin King
 */
public interface HibernateEntityManagerFactory extends EntityManagerFactory, Serializable {
	public SessionFactory getSessionFactory();
}
