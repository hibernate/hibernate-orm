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
package org.hibernate.envers.test.integration.primitive;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.PrimitiveTestEntity;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PrimitiveAddDelete extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(PrimitiveTestEntity.class);
    }

    @Test
    public void initData() {
        EntityManager em = getEntityManager();

		// Revision 1
        em.getTransaction().begin();
        PrimitiveTestEntity pte = new PrimitiveTestEntity(10, 11);
        em.persist(pte);
        id1 = pte.getId();
        em.getTransaction().commit();

		// Revision 2
        em.getTransaction().begin();
        pte = em.find(PrimitiveTestEntity.class, id1);
        pte.setNumber(20);
		pte.setNumber2(21);
        em.getTransaction().commit();

		// Revision 3
        em.getTransaction().begin();
        pte = em.find(PrimitiveTestEntity.class, id1);
        em.remove(pte);
        em.getTransaction().commit();		
    }

    @Test(dependsOnMethods = "initData")
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getAuditReader().getRevisions(PrimitiveTestEntity.class, id1));
    }

    @Test(dependsOnMethods = "initData")
    public void testHistoryOfId1() {
        PrimitiveTestEntity ver1 = new PrimitiveTestEntity(id1, 10, 0);
        PrimitiveTestEntity ver2 = new PrimitiveTestEntity(id1, 20, 0);

        assert getAuditReader().find(PrimitiveTestEntity.class, id1, 1).equals(ver1);
        assert getAuditReader().find(PrimitiveTestEntity.class, id1, 2).equals(ver2);
        assert getAuditReader().find(PrimitiveTestEntity.class, id1, 3) == null;
    }

	@Test(dependsOnMethods = "initData")
	public void testQueryWithDeleted() {
		// Selecting all entities, also the deleted ones
		List entities = getAuditReader().createQuery().forRevisionsOfEntity(PrimitiveTestEntity.class, true, true)
				.getResultList();

		assert entities.size() == 3;
		assert entities.get(0).equals(new PrimitiveTestEntity(id1, 10, 0));
		assert entities.get(1).equals(new PrimitiveTestEntity(id1, 20, 0));
		assert entities.get(2).equals(new PrimitiveTestEntity(id1, 0, 0));
	}
}