/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

/**
 * @author Chris Cranford
 */
public class EntityManagerUnwrapTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	@TestForIssue(jiraKey = "HHH-13281")
	public void testUnwrapEjbHibernateEntityManagerInterface() {
		org.hibernate.ejb.HibernateEntityManager em = getOrCreateEntityManager().unwrap( org.hibernate.ejb.HibernateEntityManager.class );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13281")
	public void testUnwrapJpaHibernateEntityManagerInterface() {
		org.hibernate.jpa.HibernateEntityManager em = getOrCreateEntityManager().unwrap( org.hibernate.jpa.HibernateEntityManager.class );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13281")
	public void testUnwrapSessionImplementor() {
		SessionImplementor session = getOrCreateEntityManager().unwrap( SessionImplementor.class );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13281")
	public void testUnwrapSession() {
		Session session = getOrCreateEntityManager().unwrap( Session.class );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13281")
	public void testUnwrapSharedSessionContractImplementor() {
		SharedSessionContractImplementor session = getOrCreateEntityManager().unwrap( SharedSessionContractImplementor.class );
	}
}
