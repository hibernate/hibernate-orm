/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration;

import java.sql.SQLException;
import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.Wallet;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class DisableDiscardPersistenceContextOnCloseTest extends BaseEntityManagerFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider( false, false );

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();
		config.put( AvailableSettings.DISCARD_PC_ON_CLOSE, "false");
		config.put(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
		return config;
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
			Wallet.class,
		};
	}

	@Test
	public void testDiscardOnClose() throws SQLException {
		EntityManager em = entityManagerFactory().createEntityManager();
		Wallet wallet = new Wallet();
		wallet.setSerial( "123" );

		try {
			em.getTransaction().begin();
			em.persist( wallet );
			assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
			em.close();
			assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
			assertTrue(em.getTransaction().isActive());
		}
		finally {
			assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
			em.getTransaction().rollback();
			assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
			assertFalse(em.getTransaction().isActive());
		}
	}
}
