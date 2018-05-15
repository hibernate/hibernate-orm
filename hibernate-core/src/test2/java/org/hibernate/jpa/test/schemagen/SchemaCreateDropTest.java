/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.schemagen;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.util.List;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10104")
public class SchemaCreateDropTest extends BaseEntityManagerFunctionalTestCase {

	private EntityManager em;

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {Document.class};
	}

	@Before
	public void setUp() {
		em = getOrCreateEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.persist( new Document( "hibernate" ) );
		tx.commit();
	}

	@Test
	public void testQueryWithoutTransaction() {
		TypedQuery<String> query = em.createQuery( "SELECT d.name FROM Document d", String.class );
		List<String> results = query.getResultList();
		assertThat( results.size(), is( 1 ) );
	}

}
