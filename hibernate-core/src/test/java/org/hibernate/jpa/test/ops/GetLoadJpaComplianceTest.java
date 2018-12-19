/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ops;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 * @author Hardy Ferentschik
 */
@RequiresDialectFeature(DialectChecks.SupportsNoColumnInsert.class)
public class GetLoadJpaComplianceTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	@SuppressWarnings( {"unchecked"})
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JPA_PROXY_COMPLIANCE, true );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12034")
	public void testLoadIdNotFound_FieldBasedAccess() {
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();
			Session s = (Session) em.getDelegate();

			assertNull( s.get( Workload.class, 999 ) );

			Workload proxy = s.load( Workload.class, 999 );
			assertFalse( Hibernate.isInitialized( proxy ) );

			proxy.getId();
			fail( "Should have failed because there is no Employee Entity with id == 999" );
		}
		catch (EntityNotFoundException ex) {
			// expected
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12034")
	public void testReferenceIdNotFound_FieldBasedAccess() {
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();

			assertNull( em.find( Workload.class, 999 ) );

			Workload proxy = em.getReference( Workload.class, 999 );
			assertFalse( Hibernate.isInitialized( proxy ) );

			proxy.getId();
			fail( "Should have failed because there is no Workload Entity with id == 999" );
		}
		catch (EntityNotFoundException ex) {
			// expected
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12034")
	public void testLoadIdNotFound_PropertyBasedAccess() {
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();
			Session s = (Session) em.getDelegate();

			assertNull( s.get( Employee.class, 999 ) );

			Employee proxy = s.load( Employee.class, 999 );
			assertFalse( Hibernate.isInitialized( proxy ) );

			proxy.getId();
			fail( "Should have failed because there is no Employee Entity with id == 999" );
		}
		catch (EntityNotFoundException ex) {
			// expected
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12034")
	public void testReferenceIdNotFound_PropertyBasedAccess() {
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();

			assertNull( em.find( Employee.class, 999 ) );

			Employee proxy = em.getReference( Employee.class, 999 );
			assertFalse( Hibernate.isInitialized( proxy ) );

			proxy.getId();
			fail( "Should have failed because there is no Employee Entity with id == 999" );
		}
		catch (EntityNotFoundException ex) {
			// expected
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Override
	protected String[] getMappings() {
		return new String[] {
				"org/hibernate/jpa/test/ops/Node.hbm.xml",
				"org/hibernate/jpa/test/ops/Employer.hbm.xml"
		};
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Workload.class };
	}
}

