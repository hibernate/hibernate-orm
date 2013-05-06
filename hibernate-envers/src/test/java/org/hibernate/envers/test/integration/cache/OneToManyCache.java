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
package org.hibernate.envers.test.integration.cache;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefIngEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"ObjectEquality"})
public class OneToManyCache extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

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

		SetRefIngEntity ing1 = new SetRefIngEntity( 1, "data_ing_1" );
		SetRefIngEntity ing2 = new SetRefIngEntity( 2, "data_ing_2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );

		ing1.setReference( ed1 );
		ing2.setReference( ed1 );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ing1 = em.find( SetRefIngEntity.class, ing1.getId() );
		ing2 = em.find( SetRefIngEntity.class, ing2.getId() );
		ed2 = em.find( SetRefEdEntity.class, ed2.getId() );

		ing1.setReference( ed2 );
		ing2.setReference( ed2 );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testCacheReferenceAccessAfterFind() {
		SetRefEdEntity ed1_rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );

		SetRefIngEntity ing1_rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
		SetRefIngEntity ing2_rev1 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 1 );

		// It should be exactly the same object
		assert ing1_rev1.getReference() == ed1_rev1;
		assert ing2_rev1.getReference() == ed1_rev1;
	}

	@Test
	public void testCacheReferenceAccessAfterCollectionAccessRev1() {
		SetRefEdEntity ed1_rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );

		// It should be exactly the same object
		assert ed1_rev1.getReffering().size() == 2;
		for ( SetRefIngEntity setRefIngEntity : ed1_rev1.getReffering() ) {
			assert setRefIngEntity.getReference() == ed1_rev1;
		}
	}

	@Test
	public void testCacheReferenceAccessAfterCollectionAccessRev2() {
		SetRefEdEntity ed2_rev2 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 2 );

		assert ed2_rev2.getReffering().size() == 2;
		for ( SetRefIngEntity setRefIngEntity : ed2_rev2.getReffering() ) {
			assert setRefIngEntity.getReference() == ed2_rev2;
		}
	}

	@Test
	public void testCacheFindAfterCollectionAccessRev1() {
		SetRefEdEntity ed1_rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );

		// Reading the collection
		assert ed1_rev1.getReffering().size() == 2;

		SetRefIngEntity ing1_rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
		SetRefIngEntity ing2_rev1 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 1 );

		for ( SetRefIngEntity setRefIngEntity : ed1_rev1.getReffering() ) {
			assert setRefIngEntity == ing1_rev1 || setRefIngEntity == ing2_rev1;
		}
	}
}