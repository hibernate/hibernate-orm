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
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.components.relations.ManyToOneComponent;
import org.hibernate.envers.test.entities.components.relations.ManyToOneComponentTestEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedManyToOneInComponent extends AbstractModifiedFlagsEntityTest {
	private Integer mtocte_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ManyToOneComponentTestEntity.class, StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrTestEntity ste1 = new StrTestEntity();
		ste1.setStr( "str1" );

		StrTestEntity ste2 = new StrTestEntity();
		ste2.setStr( "str2" );

		em.persist( ste1 );
		em.persist( ste2 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		ManyToOneComponentTestEntity mtocte1 = new ManyToOneComponentTestEntity(
				new ManyToOneComponent(
						ste1,
						"data1"
				)
		);

		em.persist( mtocte1 );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		mtocte1 = em.find( ManyToOneComponentTestEntity.class, mtocte1.getId() );
		mtocte1.getComp1().setEntity( ste2 );

		em.getTransaction().commit();

		mtocte_id1 = mtocte1.getId();
	}

	@Test
	public void testHasChangedId1() throws Exception {
		List list = queryForPropertyHasChanged(
				ManyToOneComponentTestEntity.class,
				mtocte_id1, "comp1"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				ManyToOneComponentTestEntity.class,
				mtocte_id1, "comp1"
		);
		assertEquals( 0, list.size() );
	}

}
