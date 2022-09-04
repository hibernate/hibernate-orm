/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.HashSet;
import java.util.List;

import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import jakarta.persistence.EntityManager;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.Assert.assertEquals;

/**
 * @author Artёm Basov
 */
@TestForIssue(jiraKey = "HHH-15480")
public class HasChangedOneToManyManualFlush extends AbstractModifiedFlagsEntityTest {
	private Integer id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetRefEdEntity.class, SetRefIngEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		SetRefEdEntity entity = new SetRefEdEntity( 1, "Revision 1" );
		entity.setReffering( new HashSet<>() );
		em.persist( entity );
		em.getTransaction().commit();

		id = entity.getId();

		// Revision 2 - both properties (data and reffering) should be marked as modified.
		em.getTransaction().begin();
		entity = em.find( SetRefEdEntity.class, entity.getId() );
		entity.setData( "Revision 2" );
		SetRefIngEntity refIngEntity = new SetRefIngEntity( 1, "Revision 2", entity );
		em.persist( refIngEntity );
		entity.getReffering().add( refIngEntity );
		em.flush();
		entity.setData( "Revision 2.1" );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testNonOwningSideHasChanged() {
		List list = queryForPropertyHasChanged( SetRefEdEntity.class, id, "reffering" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );
	}
}
