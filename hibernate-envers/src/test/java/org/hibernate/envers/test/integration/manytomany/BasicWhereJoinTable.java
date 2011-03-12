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
package org.hibernate.envers.test.integration.manytomany;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.IntNoAutoIdTestEntity;
import org.hibernate.envers.test.entities.manytomany.WhereJoinTableEntity;
import org.hibernate.envers.test.tools.TestTools;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicWhereJoinTable extends AbstractEntityTest {
    private Integer ite1_1_id;
    private Integer ite1_2_id;
    private Integer ite2_1_id;
    private Integer ite2_2_id;

    private Integer wjte1_id;
    private Integer wjte2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(WhereJoinTableEntity.class);
        cfg.addAnnotatedClass(IntNoAutoIdTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        IntNoAutoIdTestEntity ite1_1 = new IntNoAutoIdTestEntity(1, 10);
        IntNoAutoIdTestEntity ite1_2 = new IntNoAutoIdTestEntity(1, 11);
        IntNoAutoIdTestEntity ite2_1 = new IntNoAutoIdTestEntity(2, 20);
        IntNoAutoIdTestEntity ite2_2 = new IntNoAutoIdTestEntity(2, 21);

        WhereJoinTableEntity wjte1 = new WhereJoinTableEntity();
        wjte1.setData("wjte1");

        WhereJoinTableEntity wjte2 = new WhereJoinTableEntity();
        wjte1.setData("wjte2");

        // Revision 1
        em.getTransaction().begin();

        em.persist(ite1_1);
        em.persist(ite1_2);
        em.persist(ite2_1);
        em.persist(ite2_2);
        em.persist(wjte1);
        em.persist(wjte2);

        em.getTransaction().commit();
        em.clear();

        // Revision 2 (wjte1: 1_1, 2_1)

        em.getTransaction().begin();

        wjte1 = em.find(WhereJoinTableEntity.class, wjte1.getId());

        wjte1.getReferences1().add(ite1_1);
        wjte1.getReferences2().add(ite2_1);

        em.getTransaction().commit();
        em.clear();

        // Revision 3 (wjte1: 1_1, 2_1; wjte2: 1_1, 1_2)
        em.getTransaction().begin();

        wjte2 = em.find(WhereJoinTableEntity.class, wjte2.getId());

        wjte2.getReferences1().add(ite1_1);
        wjte2.getReferences1().add(ite1_2);

        em.getTransaction().commit();
        em.clear();

        // Revision 4 (wjte1: 2_1; wjte2: 1_1, 1_2, 2_2)
        em.getTransaction().begin();

        wjte1 = em.find(WhereJoinTableEntity.class, wjte1.getId());
        wjte2 = em.find(WhereJoinTableEntity.class, wjte2.getId());

        wjte1.getReferences1().remove(ite1_1);
        wjte2.getReferences2().add(ite2_2);

        em.getTransaction().commit();
        em.clear();

        //

        ite1_1_id = ite1_1.getId();
        ite1_2_id = ite1_2.getId();
        ite2_1_id = ite2_1.getId();
        ite2_2_id = ite2_2.getId();

        wjte1_id = wjte1.getId();
        wjte2_id = wjte2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assertEquals(Arrays.asList(1, 2, 4), getAuditReader().getRevisions(WhereJoinTableEntity.class, wjte1_id));
        assertEquals(Arrays.asList(1, 3, 4), getAuditReader().getRevisions(WhereJoinTableEntity.class, wjte2_id));

        assertEquals(Arrays.asList(1), getAuditReader().getRevisions(IntNoAutoIdTestEntity.class, ite1_1_id));
        assertEquals(Arrays.asList(1), getAuditReader().getRevisions(IntNoAutoIdTestEntity.class, ite1_2_id));
        assertEquals(Arrays.asList(1), getAuditReader().getRevisions(IntNoAutoIdTestEntity.class, ite2_1_id));
        assertEquals(Arrays.asList(1), getAuditReader().getRevisions(IntNoAutoIdTestEntity.class, ite2_2_id));
    }

    @Test
    public void testHistoryOfWjte1() {
        IntNoAutoIdTestEntity ite1_1 = getEntityManager().find(IntNoAutoIdTestEntity.class, ite1_1_id);
        IntNoAutoIdTestEntity ite2_1 = getEntityManager().find(IntNoAutoIdTestEntity.class, ite2_1_id);

        WhereJoinTableEntity rev1 = getAuditReader().find(WhereJoinTableEntity.class, wjte1_id, 1);
        WhereJoinTableEntity rev2 = getAuditReader().find(WhereJoinTableEntity.class, wjte1_id, 2);
        WhereJoinTableEntity rev3 = getAuditReader().find(WhereJoinTableEntity.class, wjte1_id, 3);
        WhereJoinTableEntity rev4 = getAuditReader().find(WhereJoinTableEntity.class, wjte1_id, 4);

        // Checking 1st list
        assert TestTools.checkList(rev1.getReferences1());
        assert TestTools.checkList(rev2.getReferences1(), ite1_1);
        assert TestTools.checkList(rev3.getReferences1(), ite1_1);
        assert TestTools.checkList(rev4.getReferences1());

        // Checking 2nd list
        assert TestTools.checkList(rev1.getReferences2());
        assert TestTools.checkList(rev2.getReferences2(), ite2_1);
        assert TestTools.checkList(rev3.getReferences2(), ite2_1);
        assert TestTools.checkList(rev4.getReferences2(), ite2_1);
    }

    @Test
    public void testHistoryOfWjte2() {
        IntNoAutoIdTestEntity ite1_1 = getEntityManager().find(IntNoAutoIdTestEntity.class, ite1_1_id);
        IntNoAutoIdTestEntity ite1_2 = getEntityManager().find(IntNoAutoIdTestEntity.class, ite1_2_id);
        IntNoAutoIdTestEntity ite2_2 = getEntityManager().find(IntNoAutoIdTestEntity.class, ite2_2_id);

        WhereJoinTableEntity rev1 = getAuditReader().find(WhereJoinTableEntity.class, wjte2_id, 1);
        WhereJoinTableEntity rev2 = getAuditReader().find(WhereJoinTableEntity.class, wjte2_id, 2);
        WhereJoinTableEntity rev3 = getAuditReader().find(WhereJoinTableEntity.class, wjte2_id, 3);
        WhereJoinTableEntity rev4 = getAuditReader().find(WhereJoinTableEntity.class, wjte2_id, 4);

        // Checking 1st list
        assert TestTools.checkList(rev1.getReferences1());
        assert TestTools.checkList(rev2.getReferences1());
        assert TestTools.checkList(rev3.getReferences1(), ite1_1, ite1_2);
        assert TestTools.checkList(rev4.getReferences1(), ite1_1, ite1_2);

        // Checking 2nd list
        assert TestTools.checkList(rev1.getReferences2());
        assert TestTools.checkList(rev2.getReferences2());
        assert TestTools.checkList(rev3.getReferences2());
        assert TestTools.checkList(rev4.getReferences2(), ite2_2);
    }
}