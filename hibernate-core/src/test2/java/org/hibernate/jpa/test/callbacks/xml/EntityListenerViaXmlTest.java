/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.callbacks.xml;

import javax.persistence.EntityManager;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class EntityListenerViaXmlTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] { "org/hibernate/jpa/test/callbacks/xml/MyEntity.orm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9771" )
	public void testUsage() {
		JournalingListener.reset();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new MyEntity( 1, "steve" ) );
		em.getTransaction().commit();
		em.close();

		assertEquals( 1, JournalingListener.getPrePersistCount() );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete MyEntity" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}
}
