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
package org.hibernate.envers.test.integration.manytomany.ternary;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.IntTestEntity;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TernaryMapFlush extends AbstractEntityTest {
    private Integer str1_id;
    private Integer str2_id;
    private Integer int1_id;
    private Integer int2_id;
    private Integer map1_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(TernaryMapEntity.class);
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(IntTestEntity.class);
    }

    @Test
    public void createData() {
        EntityManager em = getEntityManager();

        StrTestEntity str1 = new StrTestEntity("a");
        StrTestEntity str2 = new StrTestEntity("b");
        IntTestEntity int1 = new IntTestEntity(1);
        IntTestEntity int2 = new IntTestEntity(2);
        TernaryMapEntity map1 = new TernaryMapEntity();

        // Revision 1 (int1 -> str1)
        em.getTransaction().begin();

        em.persist(str1);
        em.persist(str2);
        em.persist(int1);
        em.persist(int2);

        map1.getMap().put(int1, str1);

        em.persist(map1);

        em.getTransaction().commit();

        // Revision 2 (removing int1->str1, flushing, adding int1->str1 again and a new int2->str2 mapping to force a change)

        em.getTransaction().begin();

        map1 = em.find(TernaryMapEntity.class, map1.getId());
        str1 = em.find(StrTestEntity.class, str1.getId());
        int1 = em.find(IntTestEntity.class, int1.getId());

        map1.setMap(new HashMap<IntTestEntity, StrTestEntity>());
        
        em.flush();

        map1.getMap().put(int1, str1);
        map1.getMap().put(int2, str2);

        em.getTransaction().commit();

        // Revision 3 (removing int1->str1, flushing, overwriting int2->str1)

        em.getTransaction().begin();

        map1 = em.find(TernaryMapEntity.class, map1.getId());
        str1 = em.find(StrTestEntity.class, str1.getId());
        int1 = em.find(IntTestEntity.class, int1.getId());

        map1.getMap().remove(int1);

        em.flush();

        map1.getMap().put(int2, str1);

        em.getTransaction().commit();

        //

        map1_id = map1.getId();
        str1_id = str1.getId();
        str2_id = str2.getId();
        int1_id = int1.getId();
        int2_id = int2.getId();
    }

    @Test(dependsOnMethods = "createData")
    public void testRevisionsCounts() {
        assertEquals(Arrays.asList(1, 2, 3), getAuditReader().getRevisions(TernaryMapEntity.class, map1_id));
        assertEquals(Arrays.asList(1), getAuditReader().getRevisions(StrTestEntity.class, str1_id));
        assertEquals(Arrays.asList(1), getAuditReader().getRevisions(StrTestEntity.class, str2_id));
        assertEquals(Arrays.asList(1) ,getAuditReader().getRevisions(IntTestEntity.class, int1_id));
        assertEquals(Arrays.asList(1) ,getAuditReader().getRevisions(IntTestEntity.class, int2_id));
    }

    @Test(dependsOnMethods = "createData")
    public void testHistoryOfMap1() {
        StrTestEntity str1 = getEntityManager().find(StrTestEntity.class, str1_id);
        StrTestEntity str2 = getEntityManager().find(StrTestEntity.class, str2_id);
        IntTestEntity int1 = getEntityManager().find(IntTestEntity.class, int1_id);
        IntTestEntity int2 = getEntityManager().find(IntTestEntity.class, int2_id);

        TernaryMapEntity rev1 = getAuditReader().find(TernaryMapEntity.class, map1_id, 1);
        TernaryMapEntity rev2 = getAuditReader().find(TernaryMapEntity.class, map1_id, 2);
        TernaryMapEntity rev3 = getAuditReader().find(TernaryMapEntity.class, map1_id, 3);

        assertEquals(rev1.getMap(), TestTools.makeMap(int1, str1));
        assertEquals(rev2.getMap(), TestTools.makeMap(int1, str1, int2, str2));
        assertEquals(rev3.getMap(), TestTools.makeMap(int2, str1));
    }
}