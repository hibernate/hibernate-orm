/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.interfaces.relation;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InterfacesRelation extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SetRefEdEntity.class, SetRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
		SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

		SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );

		em.getTransaction().commit();

		// Revision 2

		em.getTransaction().begin();

		ed1 = em.find( SetRefEdEntity.class, ed1.getId() );

		ing1.setReference( ed1 );
		em.persist( ing1 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing1 = em.find( SetRefIngEntity.class, ing1.getId() );
		ed2 = em.find( SetRefEdEntity.class, ed2.getId() );

		ing1.setReference( ed2 );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( SetRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( SetRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 2, 3 ).equals( getAuditReader().getRevisions( SetRefIngEntity.class, ing1_id ) );
	}

	@Test
	public void testHistoryOfEdIng1() {
		SetRefEdEntity ed1 = getEntityManager().find( SetRefEdEntity.class, ed1_id );
		SetRefEdEntity ed2 = getEntityManager().find( SetRefEdEntity.class, ed2_id );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 3 );

		assert rev1 == null;
		assert rev2.getReference().equals( ed1 );
		assert rev3.getReference().equals( ed2 );
	}
}