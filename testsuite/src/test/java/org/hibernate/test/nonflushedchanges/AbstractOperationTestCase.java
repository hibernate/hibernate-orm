package org.hibernate.test.nonflushedchanges;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.NonFlushedChanges;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.StatefulPersistenceContext;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.test.tm.ConnectionProviderImpl;
import org.hibernate.test.tm.TransactionManagerLookupImpl;
import org.hibernate.transaction.CMTTransactionFactory;
import org.hibernate.util.SerializationHelper;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole, Gail Badner (adapted this from "ops" tests version)
 */
public abstract class AbstractOperationTestCase extends FunctionalTestCase {
	private Map oldToNewEntityRefs = new HashMap();

	public AbstractOperationTestCase(String name) {
		super( name );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CONNECTION_PROVIDER, ConnectionProviderImpl.class.getName() );
		cfg.setProperty( Environment.TRANSACTION_MANAGER_STRATEGY, TransactionManagerLookupImpl.class.getName() );
		cfg.setProperty( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName() );
		cfg.setProperty( Environment.AUTO_CLOSE_SESSION, "true" );
		cfg.setProperty( Environment.FLUSH_BEFORE_COMPLETION, "true" );
		cfg.setProperty( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_STATEMENT.toString() );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	public String[] getMappings() {
		return new String[] {
				"nonflushedchanges/Node.hbm.xml",
				"nonflushedchanges/Employer.hbm.xml",
				"nonflushedchanges/OptLockEntity.hbm.xml",
				"nonflushedchanges/OneToOne.hbm.xml",
				"nonflushedchanges/Competition.hbm.xml"
		};
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	protected void clearCounts() {
		getSessions().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = ( int ) getSessions().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = ( int ) getSessions().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = ( int ) getSessions().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}

	protected void assertFetchCount(int count) {
		int fetches = ( int ) getSessions().getStatistics().getEntityFetchCount();
		assertEquals( count, fetches );
	}

	protected Session applyNonFlushedChangesToNewSessionCloseOldSession(Session oldSession) {
		NonFlushedChanges nfc = ( ( SessionImplementor ) oldSession ).getNonFlushedChanges();
		byte[] bytes = SerializationHelper.serialize( nfc );
		NonFlushedChanges nfc2 = ( NonFlushedChanges ) SerializationHelper.deserialize( bytes );
		Session newSession = openSession();
		( ( SessionImplementor ) newSession ).applyNonFlushedChanges( nfc2 );
		oldToNewEntityRefs.clear();
		for ( Iterator it = ( ( SessionImplementor ) oldSession ).getPersistenceContext()
				.getEntitiesByKey()
				.entrySet()
				.iterator(); it.hasNext(); ) {
			Map.Entry entry = ( Map.Entry ) it.next();
			EntityKey entityKey = ( EntityKey ) entry.getKey();
			Object oldEntityRef = entry.getValue();
			oldToNewEntityRefs.put(
					oldEntityRef, ( ( SessionImplementor ) newSession ).getPersistenceContext().getEntity( entityKey )
			);
		}
		for ( Iterator it = ( ( StatefulPersistenceContext ) ( ( SessionImplementor ) oldSession ).getPersistenceContext() )
				.getProxiesByKey()
				.entrySet()
				.iterator(); it.hasNext(); ) {
			Map.Entry entry = ( Map.Entry ) it.next();
			EntityKey entityKey = ( EntityKey ) entry.getKey();
			Object oldProxyRef = entry.getValue();
			oldToNewEntityRefs.put(
					oldProxyRef, ( ( SessionImplementor ) newSession ).getPersistenceContext().getProxy( entityKey )
			);
		}

		oldSession.clear();
		oldSession.close();
		return newSession;
	}

	protected void applyNonFlushedChangesToClearedSession(Session s) {
		NonFlushedChanges nfc = ( ( SessionImplementor ) s ).getNonFlushedChanges();
		byte[] bytes = SerializationHelper.serialize( nfc );
		NonFlushedChanges nfc2 = ( NonFlushedChanges ) SerializationHelper.deserialize( bytes );
		s.clear();
		( ( SessionImplementor ) s ).applyNonFlushedChanges( nfc2 );
	}

	protected Map getOldToNewEntityRefMap() {
		return Collections.unmodifiableMap( oldToNewEntityRefs );
	}
}
