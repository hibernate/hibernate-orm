/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration;

import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.Wallet;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class EnableDiscardPersistenceContextOnCloseTest extends BaseEntityManagerFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider( false, false );

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();
		config.put( AvailableSettings.DISCARD_PC_ON_CLOSE, "true");
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
	public void testDiscardOnClose() {
		EntityManager em = entityManagerFactory().createEntityManager();
		Wallet wallet = new Wallet();
		wallet.setSerial( "123" );

		try {
			em.getTransaction().begin();
			em.persist( wallet );
			assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
			em.close();
			assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
			assertTrue(em.getTransaction().isActive());
		}
		finally {
			try {
				em.getTransaction().rollback();
				fail("Should throw IllegalStateException because the Connection is already closed!");
			}
			catch ( IllegalStateException expected ) {
			}
		}
	}
}
