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
package org.hibernate.envers.test.integration.onetomany.detached;

import java.util.Arrays;
import java.util.HashSet;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.onetomany.detached.SetRefCollEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DataChangesDetachedSet extends AbstractEntityTest {
    private Integer str1_id;

    private Integer coll1_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(SetRefCollEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        StrTestEntity str1 = new StrTestEntity("str1");

        SetRefCollEntity coll1 = new SetRefCollEntity(3, "coll1");

        // Revision 1
        em.getTransaction().begin();

        em.persist(str1);

        coll1.setCollection(new HashSet<StrTestEntity>());
        em.persist(coll1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        str1 = em.find(StrTestEntity.class, str1.getId());
        coll1 = em.find(SetRefCollEntity.class, coll1.getId());

        coll1.getCollection().add(str1);
        coll1.setData("coll2");

        em.getTransaction().commit();

        //

        str1_id = str1.getId();

        coll1_id = coll1.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(SetRefCollEntity.class, coll1_id));

        assert Arrays.asList(1).equals(getAuditReader().getRevisions(StrTestEntity.class, str1_id));
    }

    @Test
    public void testHistoryOfColl1() {
        StrTestEntity str1 = getEntityManager().find(StrTestEntity.class, str1_id);

        SetRefCollEntity rev1 = getAuditReader().find(SetRefCollEntity.class, coll1_id, 1);
        SetRefCollEntity rev2 = getAuditReader().find(SetRefCollEntity.class, coll1_id, 2);

        assert rev1.getCollection().equals(TestTools.makeSet());
        assert rev2.getCollection().equals(TestTools.makeSet(str1));

        assert "coll1".equals(rev1.getData());
        assert "coll2".equals(rev2.getData());
    }
}