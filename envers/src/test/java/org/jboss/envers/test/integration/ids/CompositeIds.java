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
package org.jboss.envers.test.integration.ids;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.ids.EmbId;
import org.jboss.envers.test.entities.ids.EmbIdTestEntity;
import org.jboss.envers.test.entities.ids.MulId;
import org.jboss.envers.test.entities.ids.MulIdTestEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CompositeIds extends AbstractEntityTest {
    private EmbId id1;
    private EmbId id2;
    private MulId id3;
    private MulId id4;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(EmbIdTestEntity.class);
        cfg.addAnnotatedClass(MulIdTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        id1 = new EmbId(1, 2);
        id2 = new EmbId(10, 20);
        id3 = new MulId(100, 101);
        id4 = new MulId(102, 103);

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(new EmbIdTestEntity(id1, "x"));
        em.persist(new MulIdTestEntity(id3.getId1(), id3.getId2(), "a"));

        em.getTransaction().commit();

        // Revision 2
        em = getEntityManager();
        em.getTransaction().begin();

        em.persist(new EmbIdTestEntity(id2, "y"));
        em.persist(new MulIdTestEntity(id4.getId1(), id4.getId2(), "b"));

        em.getTransaction().commit();

        // Revision 3
        em = getEntityManager();
        em.getTransaction().begin();

        EmbIdTestEntity ete1 = em.find(EmbIdTestEntity.class, id1);
        EmbIdTestEntity ete2 = em.find(EmbIdTestEntity.class, id2);
        MulIdTestEntity mte3 = em.find(MulIdTestEntity.class, id3);
        MulIdTestEntity mte4 = em.find(MulIdTestEntity.class, id4);

        ete1.setStr1("x2");
        ete2.setStr1("y2");
        mte3.setStr1("a2");
        mte4.setStr1("b2");

        em.getTransaction().commit();

        // Revision 4
        em = getEntityManager();
        em.getTransaction().begin();

        ete1 = em.find(EmbIdTestEntity.class, id1);
        ete2 = em.find(EmbIdTestEntity.class, id2);
        mte3 = em.find(MulIdTestEntity.class, id3);

        em.remove(ete1);
        em.remove(mte3);

        ete2.setStr1("y3");

        em.getTransaction().commit();

        // Revision 5
        em = getEntityManager();
        em.getTransaction().begin();

        ete2 = em.find(EmbIdTestEntity.class, id2);

        em.remove(ete2);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 3, 4).equals(getVersionsReader().getRevisions(EmbIdTestEntity.class, id1));

        assert Arrays.asList(2, 3, 4, 5).equals(getVersionsReader().getRevisions(EmbIdTestEntity.class, id2));

        assert Arrays.asList(1, 3, 4).equals(getVersionsReader().getRevisions(MulIdTestEntity.class, id3));

        assert Arrays.asList(2, 3).equals(getVersionsReader().getRevisions(MulIdTestEntity.class, id4));
    }

    @Test
    public void testHistoryOfId1() {
        EmbIdTestEntity ver1 = new EmbIdTestEntity(id1, "x");
        EmbIdTestEntity ver2 = new EmbIdTestEntity(id1, "x2");

        assert getVersionsReader().find(EmbIdTestEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(EmbIdTestEntity.class, id1, 2).equals(ver1);
        assert getVersionsReader().find(EmbIdTestEntity.class, id1, 3).equals(ver2);
        assert getVersionsReader().find(EmbIdTestEntity.class, id1, 4) == null;
        assert getVersionsReader().find(EmbIdTestEntity.class, id1, 5) == null;
    }

    @Test
    public void testHistoryOfId2() {
        EmbIdTestEntity ver1 = new EmbIdTestEntity(id2, "y");
        EmbIdTestEntity ver2 = new EmbIdTestEntity(id2, "y2");
        EmbIdTestEntity ver3 = new EmbIdTestEntity(id2, "y3");

        assert getVersionsReader().find(EmbIdTestEntity.class, id2, 1) == null;
        assert getVersionsReader().find(EmbIdTestEntity.class, id2, 2).equals(ver1);
        assert getVersionsReader().find(EmbIdTestEntity.class, id2, 3).equals(ver2);
        assert getVersionsReader().find(EmbIdTestEntity.class, id2, 4).equals(ver3);
        assert getVersionsReader().find(EmbIdTestEntity.class, id2, 5) == null;
    }

    @Test
    public void testHistoryOfId3() {
        MulIdTestEntity ver1 = new MulIdTestEntity(id3.getId1(), id3.getId2(), "a");
        MulIdTestEntity ver2 = new MulIdTestEntity(id3.getId1(), id3.getId2(), "a2");

        assert getVersionsReader().find(MulIdTestEntity.class, id3, 1).equals(ver1);
        assert getVersionsReader().find(MulIdTestEntity.class, id3, 2).equals(ver1);
        assert getVersionsReader().find(MulIdTestEntity.class, id3, 3).equals(ver2);
        assert getVersionsReader().find(MulIdTestEntity.class, id3, 4) == null;
        assert getVersionsReader().find(MulIdTestEntity.class, id3, 5) == null;
    }

    @Test
    public void testHistoryOfId4() {
        MulIdTestEntity ver1 = new MulIdTestEntity(id4.getId1(), id4.getId2(), "b");
        MulIdTestEntity ver2 = new MulIdTestEntity(id4.getId1(), id4.getId2(), "b2");

        assert getVersionsReader().find(MulIdTestEntity.class, id4, 1) == null;
        assert getVersionsReader().find(MulIdTestEntity.class, id4, 2).equals(ver1);
        assert getVersionsReader().find(MulIdTestEntity.class, id4, 3).equals(ver2);
        assert getVersionsReader().find(MulIdTestEntity.class, id4, 4).equals(ver2);
        assert getVersionsReader().find(MulIdTestEntity.class, id4, 5).equals(ver2);
    }
}
