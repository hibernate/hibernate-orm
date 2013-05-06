/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

package org.hibernate.envers.test.integration.onetomany;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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