//$Id$
package org.hibernate.ejb;

import java.util.Map;
import javax.persistence.PersistenceContextType;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.util.JTAHelper;

/**
 * @author Gavin King
 * @author Emmanuel Bernard
 * @deprecated no longer used since getEntityManager is no longer here
 */
public class CurrentEntityManagerImpl extends AbstractEntityManagerImpl {

	public CurrentEntityManagerImpl(
			EntityManagerFactoryImpl entityManagerFactory,
			PersistenceUnitTransactionType transactionType,
			Map properties) {
		super( entityManagerFactory, PersistenceContextType.TRANSACTION, transactionType, properties );
		postInit();
	}

	public Session getSession() {
		/**
		 * Handle non transactional mode by requesting a temporary session to the session factory
		 * This session, will aggressively use the AFTER_STATEMENT connection release mode to be
		 * sure the conenctions are released. Be aware that the session will not be closed explicitly.
		 */

		SessionFactoryImplementor sfi = (SessionFactoryImplementor) getEntityManagerFactory().getSessionFactory();
		Session s;
		if ( !JTAHelper.isTransactionInProgress( sfi ) ) {
			s = sfi.openTemporarySession();
			( (SessionImplementor) s ).setAutoClear( true );
		}
		else {
			s = sfi.getCurrentSession();
		}
		return s;
	}

	protected Session getRawSession() {
		return getSession();
	}

	public void close() {
		throw new UnsupportedOperationException( "cannot close the JTA-bound EntityManager" );
	}

	public boolean isOpen() {
		//TODO Hum contradictory comments, I'm getting mad
		//no need to force enlistment in the tx, a current session is always enlisted
		//adjustFlushMode(); //don't adjust, can't be done on closed EM
		getRawSession().isOpen(); //to force enlistment in tx
		return true;
	}

}
