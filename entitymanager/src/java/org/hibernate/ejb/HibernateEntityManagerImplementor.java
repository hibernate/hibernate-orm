//$Id$
package org.hibernate.ejb;

import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;

/**
 * @author Emmanuel Bernard
 */
public interface HibernateEntityManagerImplementor extends HibernateEntityManager {
	boolean isTransactionInProgress();

	public void throwPersistenceException(PersistenceException e);

	public void throwPersistenceException(HibernateException e);

	public PersistenceException wrapStaleStateException(StaleStateException e);
}
