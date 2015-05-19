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

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;
import static org.junit.Assert.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7918")
public class HasChangedManualFlush extends AbstractModifiedFlagsEntityTest {
	private Integer id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity1.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		BasicTestEntity1 entity = new BasicTestEntity1( "str1", 1 );
		em.persist( entity );
		em.getTransaction().commit();

		id = entity.getId();

		// Revision 2 - both properties (str1 and long1) should be marked as modified.
		em.getTransaction().begin();
		entity = em.find( BasicTestEntity1.class, entity.getId() );
		entity.setStr1( "str2" );
		entity = em.merge( entity );
		em.flush();
		entity.setLong1( 2 );
		entity = em.merge( entity );
		em.flush();
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testHasChangedOnDoubleFlush() {
		List list = queryForPropertyHasChanged( BasicTestEntity1.class, id, "str1" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( BasicTestEntity1.class, id, "long1" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );
	}
}
