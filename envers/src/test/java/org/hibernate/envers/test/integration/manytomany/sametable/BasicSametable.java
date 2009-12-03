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
package org.hibernate.envers.test.integration.manytomany.sametable;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.manytomany.sametable.ParentEntity;
import org.hibernate.envers.test.entities.manytomany.sametable.Child1Entity;
import org.hibernate.envers.test.entities.manytomany.sametable.Child2Entity;
import org.hibernate.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.Session;

/**
 * Test which checks that auditing entities which contain multiple mappings to same tables work.
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSametable extends AbstractEntityTest {
    private Integer p1_id;
    private Integer p2_id;
    private Integer c1_1_id;
    private Integer c1_2_id;
    private Integer c2_1_id;
    private Integer c2_2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ParentEntity.class);
        cfg.addAnnotatedClass(Child1Entity.class);
        cfg.addAnnotatedClass(Child2Entity.class);
    }

    @BeforeClass(enabled = true, dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        // We need first to modify the columns in the middle (join table) to allow null values. Hbm2ddl doesn't seem
        // to allow this.
        em.getTransaction().begin();
        Session session = (Session) em.getDelegate();
        session.createSQLQuery("DROP TABLE children").executeUpdate();
        session.createSQLQuery("CREATE TABLE children(parent_id integer, child1_id integer NULL, child2_id integer NULL)").executeUpdate();
        session.createSQLQuery("DROP TABLE children_AUD").executeUpdate();
        session.createSQLQuery("CREATE TABLE children_AUD(REV integer NOT NULL, REVTYPE tinyint, " +
                "parent_id integer, child1_id integer NULL, child2_id integer NULL)").executeUpdate();
        em.getTransaction().commit();
        em.clear();

        ParentEntity p1 = new ParentEntity("parent_1");
        ParentEntity p2 = new ParentEntity("parent_2");

        Child1Entity c1_1 = new Child1Entity("child1_1");
        Child1Entity c1_2 = new Child1Entity("child1_2");

        Child2Entity c2_1 = new Child2Entity("child2_1");
        Child2Entity c2_2 = new Child2Entity("child2_2");

        // Revision 1
        em.getTransaction().begin();

        em.persist(p1);
        em.persist(p2);
        em.persist(c1_1);
        em.persist(c1_2);
        em.persist(c2_1);
        em.persist(c2_2);

        em.getTransaction().commit();
        em.clear();

        // Revision 2 - (p1: c1_1, p2: c2_1)

        em.getTransaction().begin();

        p1 = em.find(ParentEntity.class, p1.getId());
        p2 = em.find(ParentEntity.class, p2.getId());
        c1_1 = em.find(Child1Entity.class, c1_1.getId());
        c2_1 = em.find(Child2Entity.class, c2_1.getId());

        p1.getChildren1().add(c1_1);
        p2.getChildren2().add(c2_1);

        em.getTransaction().commit();
        em.clear();

        // Revision 3 - (p1: c1_1, c1_2, c2_2, p2: c1_1, c2_1)
        em.getTransaction().begin();

        p1 = em.find(ParentEntity.class, p1.getId());
        p2 = em.find(ParentEntity.class, p2.getId());
        c1_1 = em.find(Child1Entity.class, c1_1.getId());
        c1_2 = em.find(Child1Entity.class, c1_2.getId());
        c2_2 = em.find(Child2Entity.class, c2_2.getId());

        p1.getChildren1().add(c1_2);
        p1.getChildren2().add(c2_2);

        p2.getChildren1().add(c1_1);

        em.getTransaction().commit();
        em.clear();

        // Revision 4 - (p1: c1_2, c2_2, p2: c1_1, c2_1, c2_2)
        em.getTransaction().begin();

        p1 = em.find(ParentEntity.class, p1.getId());
        p2 = em.find(ParentEntity.class, p2.getId());
        c1_1 = em.find(Child1Entity.class, c1_1.getId());
        c2_2 = em.find(Child2Entity.class, c2_2.getId());

        p1.getChildren1().remove(c1_1);
        p2.getChildren2().add(c2_2);

        em.getTransaction().commit();
        em.clear();

        // Revision 5 - (p1: c2_2, p2: c1_1, c2_1)
        em.getTransaction().begin();

        p1 = em.find(ParentEntity.class, p1.getId());
        p2 = em.find(ParentEntity.class, p2.getId());
        c1_2 = em.find(Child1Entity.class, c1_2.getId());
        c2_2 = em.find(Child2Entity.class, c2_2.getId());

        c2_2.getParents().remove(p2);
        c1_2.getParents().remove(p1);

        em.getTransaction().commit();
        em.clear();

        //

        p1_id = p1.getId();
        p2_id = p2.getId();
        c1_1_id = c1_1.getId();
        c1_2_id = c1_2.getId();
        c2_1_id = c2_1.getId();
        c2_2_id = c2_2.getId();
    }

    @Test(enabled = true)
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3, 4).equals(getAuditReader().getRevisions(ParentEntity.class, p1_id));
        assert Arrays.asList(1, 2, 3, 4).equals(getAuditReader().getRevisions(ParentEntity.class, p2_id));

        assert Arrays.asList(1).equals(getAuditReader().getRevisions(Child1Entity.class, c1_1_id));
        assert Arrays.asList(1, 5).equals(getAuditReader().getRevisions(Child1Entity.class, c1_2_id));

        assert Arrays.asList(1).equals(getAuditReader().getRevisions(Child1Entity.class, c2_1_id));
        assert Arrays.asList(1, 5).equals(getAuditReader().getRevisions(Child1Entity.class, c2_2_id));
    }

    @Test(enabled = true)
    public void testHistoryOfParent1() {
        Child1Entity c1_1 = getEntityManager().find(Child1Entity.class, c1_1_id);
        Child1Entity c1_2 = getEntityManager().find(Child1Entity.class, c1_2_id);
        Child2Entity c2_2 = getEntityManager().find(Child2Entity.class, c2_2_id);

        ParentEntity rev1 = getAuditReader().find(ParentEntity.class, p1_id, 1);
        ParentEntity rev2 = getAuditReader().find(ParentEntity.class, p1_id, 2);
        ParentEntity rev3 = getAuditReader().find(ParentEntity.class, p1_id, 3);
        ParentEntity rev4 = getAuditReader().find(ParentEntity.class, p1_id, 4);
        ParentEntity rev5 = getAuditReader().find(ParentEntity.class, p1_id, 5);

        assert TestTools.checkList(rev1.getChildren1());
        assert TestTools.checkList(rev2.getChildren1(), c1_1);
        assert TestTools.checkList(rev3.getChildren1(), c1_1, c1_2);
        assert TestTools.checkList(rev4.getChildren1(), c1_2);
        assert TestTools.checkList(rev5.getChildren1());

        assert TestTools.checkList(rev1.getChildren2());
        assert TestTools.checkList(rev2.getChildren2());
        assert TestTools.checkList(rev3.getChildren2(), c2_2);
        assert TestTools.checkList(rev4.getChildren2(), c2_2);
        assert TestTools.checkList(rev5.getChildren2(), c2_2);
    }

    @Test(enabled = true)
    public void testHistoryOfParent2() {
        Child1Entity c1_1 = getEntityManager().find(Child1Entity.class, c1_1_id);
        Child2Entity c2_1 = getEntityManager().find(Child2Entity.class, c2_1_id);
        Child2Entity c2_2 = getEntityManager().find(Child2Entity.class, c2_2_id);

        ParentEntity rev1 = getAuditReader().find(ParentEntity.class, p2_id, 1);
        ParentEntity rev2 = getAuditReader().find(ParentEntity.class, p2_id, 2);
        ParentEntity rev3 = getAuditReader().find(ParentEntity.class, p2_id, 3);
        ParentEntity rev4 = getAuditReader().find(ParentEntity.class, p2_id, 4);
        ParentEntity rev5 = getAuditReader().find(ParentEntity.class, p2_id, 5);

        assert TestTools.checkList(rev1.getChildren1());
        assert TestTools.checkList(rev2.getChildren1());
        assert TestTools.checkList(rev3.getChildren1(), c1_1);
        assert TestTools.checkList(rev4.getChildren1(), c1_1);
        assert TestTools.checkList(rev5.getChildren1(), c1_1);

        assert TestTools.checkList(rev1.getChildren2());
        assert TestTools.checkList(rev2.getChildren2(), c2_1);
        assert TestTools.checkList(rev3.getChildren2(), c2_1);
        assert TestTools.checkList(rev4.getChildren2(), c2_1, c2_2);
        assert TestTools.checkList(rev5.getChildren2(), c2_1);
    }

    @Test(enabled = true)
    public void testHistoryOfChild1_1() {
        ParentEntity p1 = getEntityManager().find(ParentEntity.class, p1_id);
        ParentEntity p2 = getEntityManager().find(ParentEntity.class, p2_id);

        Child1Entity rev1 = getAuditReader().find(Child1Entity.class, c1_1_id, 1);
        Child1Entity rev2 = getAuditReader().find(Child1Entity.class, c1_1_id, 2);
        Child1Entity rev3 = getAuditReader().find(Child1Entity.class, c1_1_id, 3);
        Child1Entity rev4 = getAuditReader().find(Child1Entity.class, c1_1_id, 4);
        Child1Entity rev5 = getAuditReader().find(Child1Entity.class, c1_1_id, 5);

        assert TestTools.checkList(rev1.getParents());
        assert TestTools.checkList(rev2.getParents(), p1);
        assert TestTools.checkList(rev3.getParents(), p1, p2);
        assert TestTools.checkList(rev4.getParents(), p2);
        assert TestTools.checkList(rev5.getParents(), p2);
    }

    @Test(enabled = false)
    public void testHistoryOfChild1_2() {
        ParentEntity p1 = getEntityManager().find(ParentEntity.class, p1_id);

        Child1Entity rev1 = getAuditReader().find(Child1Entity.class, c1_2_id, 1);
        Child1Entity rev2 = getAuditReader().find(Child1Entity.class, c1_2_id, 2);
        Child1Entity rev3 = getAuditReader().find(Child1Entity.class, c1_2_id, 3);
        Child1Entity rev4 = getAuditReader().find(Child1Entity.class, c1_2_id, 4);
        Child1Entity rev5 = getAuditReader().find(Child1Entity.class, c1_2_id, 5);

        assert TestTools.checkList(rev1.getParents());
        assert TestTools.checkList(rev2.getParents());
        assert TestTools.checkList(rev3.getParents(), p1);
        assert TestTools.checkList(rev4.getParents(), p1);
        assert TestTools.checkList(rev5.getParents());
    }

    @Test(enabled = true)
    public void testHistoryOfChild2_1() {
        ParentEntity p2 = getEntityManager().find(ParentEntity.class, p2_id);

        Child2Entity rev1 = getAuditReader().find(Child2Entity.class, c2_1_id, 1);
        Child2Entity rev2 = getAuditReader().find(Child2Entity.class, c2_1_id, 2);
        Child2Entity rev3 = getAuditReader().find(Child2Entity.class, c2_1_id, 3);
        Child2Entity rev4 = getAuditReader().find(Child2Entity.class, c2_1_id, 4);
        Child2Entity rev5 = getAuditReader().find(Child2Entity.class, c2_1_id, 5);

        assert TestTools.checkList(rev1.getParents());
        assert TestTools.checkList(rev2.getParents(), p2);
        assert TestTools.checkList(rev3.getParents(), p2);
        assert TestTools.checkList(rev4.getParents(), p2);
        assert TestTools.checkList(rev5.getParents(), p2);
    }

    @Test(enabled = true)
    public void testHistoryOfChild2_2() {
        ParentEntity p1 = getEntityManager().find(ParentEntity.class, p1_id);
        ParentEntity p2 = getEntityManager().find(ParentEntity.class, p2_id);

        Child2Entity rev1 = getAuditReader().find(Child2Entity.class, c2_2_id, 1);
        Child2Entity rev2 = getAuditReader().find(Child2Entity.class, c2_2_id, 2);
        Child2Entity rev3 = getAuditReader().find(Child2Entity.class, c2_2_id, 3);
        Child2Entity rev4 = getAuditReader().find(Child2Entity.class, c2_2_id, 4);
        Child2Entity rev5 = getAuditReader().find(Child2Entity.class, c2_2_id, 5);

        assert TestTools.checkList(rev1.getParents());
        assert TestTools.checkList(rev2.getParents());
        assert TestTools.checkList(rev3.getParents(), p1);
        assert TestTools.checkList(rev4.getParents(), p1, p2);
        assert TestTools.checkList(rev5.getParents(), p1);
    }
}