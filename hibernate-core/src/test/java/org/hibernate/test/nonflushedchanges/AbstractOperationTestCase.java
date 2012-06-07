/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.test.nonflushedchanges;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.NonFlushedChanges;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 *  (adapted this from "ops" tests version)
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public abstract class AbstractOperationTestCase extends BaseCoreFunctionalTestCase {
	private Map oldToNewEntityRefs = new HashMap();

	public void configure(Configuration cfg) {
		super.configure( cfg );
		TestingJtaBootstrap.prepare( cfg.getProperties() );
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

	@Override
    public String getCacheConcurrencyStrategy() {
		return null;
	}

	protected void clearCounts() {
		sessionFactory().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = ( int ) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = ( int ) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = ( int ) sessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}

	protected void assertFetchCount(int count) {
		int fetches = ( int ) sessionFactory().getStatistics().getEntityFetchCount();
		assertEquals( count, fetches );
	}

	@SuppressWarnings( {"unchecked"})
	protected Session applyNonFlushedChangesToNewSessionCloseOldSession(Session oldSession) {
		NonFlushedChanges nfc = ( ( SessionImplementor ) oldSession ).getNonFlushedChanges();
		byte[] bytes = SerializationHelper.serialize( nfc );
		NonFlushedChanges nfc2 = ( NonFlushedChanges ) SerializationHelper.deserialize( bytes );
		Session newSession = openSession();
		( ( SessionImplementor ) newSession ).applyNonFlushedChanges( nfc2 );
		oldToNewEntityRefs.clear();
		for ( Object o : ((SessionImplementor) oldSession).getPersistenceContext()
				.getEntitiesByKey()
				.entrySet() ) {
			Map.Entry entry = (Map.Entry) o;
			EntityKey entityKey = (EntityKey) entry.getKey();
			Object oldEntityRef = entry.getValue();
			oldToNewEntityRefs.put(
					oldEntityRef,
					((SessionImplementor) newSession).getPersistenceContext().getEntity( entityKey )
			);
		}
		for ( Object o : ((StatefulPersistenceContext) ((SessionImplementor) oldSession).getPersistenceContext())
				.getProxiesByKey()
				.entrySet() ) {
			Map.Entry entry = (Map.Entry) o;
			EntityKey entityKey = (EntityKey) entry.getKey();
			Object oldProxyRef = entry.getValue();
			oldToNewEntityRefs.put(
					oldProxyRef,
					((SessionImplementor) newSession).getPersistenceContext().getProxy( entityKey )
			);
		}

		oldSession.clear();
		oldSession.close();
		return newSession;
	}

//	protected void applyNonFlushedChangesToClearedSession(Session s) {
//		NonFlushedChanges nfc = ( ( SessionImplementor ) s ).getNonFlushedChanges();
//		byte[] bytes = SerializationHelper.serialize( nfc );
//		NonFlushedChanges nfc2 = ( NonFlushedChanges ) SerializationHelper.deserialize( bytes );
//		s.clear();
//		( ( SessionImplementor ) s ).applyNonFlushedChanges( nfc2 );
//	}

	@SuppressWarnings( {"unchecked"})
	protected Map getOldToNewEntityRefMap() {
		return Collections.unmodifiableMap( oldToNewEntityRefs );
	}
}
