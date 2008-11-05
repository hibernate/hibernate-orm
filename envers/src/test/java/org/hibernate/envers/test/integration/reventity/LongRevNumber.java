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
package org.hibernate.envers.test.integration.reventity;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class LongRevNumber extends AbstractEntityTest {
    private Integer id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(LongRevNumberRevEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() throws InterruptedException {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        StrTestEntity te = new StrTestEntity("x");
        em.persist(te);
        id = te.getId();
        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();
        te = em.find(StrTestEntity.class, id);
        te.setStr("y");
        em.getTransaction().commit();
    }

    @Test
    public void testFindRevision() {
        AuditReader vr = getAuditReader();

        assert vr.findRevision(LongRevNumberRevEntity.class, 1l).getCustomId() == 1l;
        assert vr.findRevision(LongRevNumberRevEntity.class, 2l).getCustomId() == 2l;
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1l, 2l).equals(getAuditReader().getRevisions(StrTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId1() {
        StrTestEntity ver1 = new StrTestEntity("x", id);
        StrTestEntity ver2 = new StrTestEntity("y", id);

        assert getAuditReader().find(StrTestEntity.class, id, 1l).equals(ver1);
        assert getAuditReader().find(StrTestEntity.class, id, 2l).equals(ver2);
    }
}