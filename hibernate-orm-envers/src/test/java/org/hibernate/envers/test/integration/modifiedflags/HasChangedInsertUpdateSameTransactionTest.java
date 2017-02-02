/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.basic.BasicTestEntity1;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11582")
public class HasChangedInsertUpdateSameTransactionTest extends AbstractModifiedFlagsEntityTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicTestEntity1.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {
			// Revision 1
			entityManager.getTransaction().begin();
			BasicTestEntity1 entity = new BasicTestEntity1( "str1", 1 );
			entityManager.persist( entity );
			entity.setStr1( "str2" );
			entityManager.merge( entity );
			entityManager.getTransaction().commit();
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testPropertyChangedInsrtUpdateSameTransaction() {
		// this was only flagged as changed as part of the persist
		List list = queryForPropertyHasChanged( BasicTestEntity1.class, 1, "long1" );
		assertEquals( 1, list.size() );
	}
}
