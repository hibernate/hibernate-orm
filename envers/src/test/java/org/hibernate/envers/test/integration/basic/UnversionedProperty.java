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
package org.hibernate.envers.test.integration.basic;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.UnversionedEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class UnversionedProperty extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(UnversionedEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        // Rev 1
        em.getTransaction().begin();
        UnversionedEntity ue1 = new UnversionedEntity("a1", "b1");
        em.persist(ue1);
        id1 = ue1.getId();
        em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();
        ue1 = em.find(UnversionedEntity.class, id1);
        ue1.setData1("a2");
        ue1.setData2("b2");
        em.getTransaction().commit();
    }

     @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(UnversionedEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        UnversionedEntity rev1 = new UnversionedEntity(id1, "a1", null);
        UnversionedEntity rev2 = new UnversionedEntity(id1, "a2", null);

        assert getAuditReader().find(UnversionedEntity.class, id1, 1).equals(rev1);
        assert getAuditReader().find(UnversionedEntity.class, id1, 2).equals(rev2);
    }
}
