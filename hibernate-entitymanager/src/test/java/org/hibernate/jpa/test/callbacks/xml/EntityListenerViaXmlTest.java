/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
