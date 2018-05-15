/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public class JtaGetTransactionThrowsExceptionTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.TRANSACTION_TYPE, "JTA" );
		options.put( org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "true" );
	}

	@Test(expected = IllegalStateException.class)
	@TestForIssue( jiraKey = "HHH-12487")
	public void onCloseEntityManagerTest() {
		EntityManager em = createEntityManager();
		em.close();
		em.getTransaction();
		fail( "Calling getTransaction on a JTA entity manager should throw an IllegalStateException" );
	}

	@Test(expected = IllegalStateException.class)
	@TestForIssue(jiraKey = "HHH-12487")
	public void onOpenEntityManagerTest() {
		EntityManager em = createEntityManager();
		try {
			em.getTransaction();
			fail( "Calling getTransaction on a JTA entity manager should throw an IllegalStateException" );
		}
		finally {
			em.close();
		}
	}
}

