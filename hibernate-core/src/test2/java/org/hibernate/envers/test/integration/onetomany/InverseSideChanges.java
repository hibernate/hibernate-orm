/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.onetomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefIngEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InverseSideChanges extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;

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

		SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );

		em.getTransaction().commit();

		// Revision 2

		em.getTransaction().begin();

		ed1 = em.find( SetRefEdEntity.class, ed1.getId() );

		em.persist( ing1 );

		ed1.setReffering( new HashSet<SetRefIngEntity>() );
		ed1.getReffering().add( ing1 );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();

		ing1_id = ing1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( SetRefEdEntity.class, ed1_id ) );

		assert Arrays.asList( 2 ).equals( getAuditReader().getRevisions( SetRefIngEntity.class, ing1_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );

		assert rev1.getReffering().equals( Collections.EMPTY_SET );
	}

	@Test
	public void testHistoryOfEdIng1() {
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 2 );

		assert rev2.getReference() == null;
	}
}