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
import org.hibernate.envers.test.integration.onetoone.bidirectional.BiRefEdEntity;
import org.hibernate.envers.test.integration.onetoone.bidirectional.BiRefIngEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedBidirectional2 extends AbstractModifiedFlagsEntityTest {
	private Integer ed1_id;
	private Integer ed2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BiRefEdEntity.class, BiRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		BiRefEdEntity ed1 = new BiRefEdEntity( 1, "data_ed_1" );
		BiRefEdEntity ed2 = new BiRefEdEntity( 2, "data_ed_2" );

		BiRefIngEntity ing1 = new BiRefIngEntity( 3, "data_ing_1" );
		BiRefIngEntity ing2 = new BiRefIngEntity( 4, "data_ing_2" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ed1 = em.find( BiRefEdEntity.class, ed1.getId() );

		ing1.setReference( ed1 );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ed1 = em.find( BiRefEdEntity.class, ed1.getId() );
		ing1 = em.find( BiRefIngEntity.class, ing1.getId() );
		ing2 = em.find( BiRefIngEntity.class, ing2.getId() );

		ing1.setReference( null );
		ing2.setReference( ed1 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		ed2 = em.find( BiRefEdEntity.class, ed2.getId() );
		ing1 = em.find( BiRefIngEntity.class, ing1.getId() );
		ing2 = em.find( BiRefIngEntity.class, ing2.getId() );

		ing1.setReference( ed2 );
		ing2.setReference( null );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

	}

	@Test
	public void testHasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				BiRefEdEntity.class, ed1_id,
				"referencing"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 2, 3, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged(
				BiRefEdEntity.class, ed2_id,
				"referencing"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 4 ), extractRevisionNumbers( list ) );
	}
}