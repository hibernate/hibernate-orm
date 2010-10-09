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
package org.hibernate.envers.test.integration.query;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.entities.reventity.CustomRevEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class CustomRevEntityQuery extends AbstractEntityTest {
    private Integer id1;
    private Integer id2;
    private Long timestamp;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(CustomRevEntity.class);
        cfg.addAnnotatedClass(StrIntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() throws InterruptedException {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        StrIntTestEntity site1 = new StrIntTestEntity("a", 10);
        StrIntTestEntity site2 = new StrIntTestEntity("b", 15);

        em.persist(site1);
        em.persist(site2);

        id1 = site1.getId();
        id2 = site2.getId();

        em.getTransaction().commit();

        Thread.sleep(100);

        timestamp = System.currentTimeMillis();

        Thread.sleep(100);

        // Revision 2
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);

        site1.setStr1("c");

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsOfId1Query() {
        List<Object[]> result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .add(AuditEntity.id().eq(id1))
                .getResultList();

        assert result.get(0)[0].equals(new StrIntTestEntity("a", 10, id1));
        assert result.get(0)[1] instanceof CustomRevEntity;
        assert ((CustomRevEntity) result.get(0)[1]).getCustomId() == 1;

        assert result.get(1)[0].equals(new StrIntTestEntity("c", 10, id1));
        assert result.get(1)[1] instanceof CustomRevEntity;
        assert ((CustomRevEntity) result.get(1)[1]).getCustomId() == 2;
    }

    @Test
    public void testRevisionsOfId2Query() {
        List<Object[]> result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .add(AuditEntity.id().eq(id2))
                .getResultList();

        assert result.get(0)[0].equals(new StrIntTestEntity("b", 15, id2));
        assert result.get(0)[1] instanceof CustomRevEntity;
        assert ((CustomRevEntity) result.get(0)[1]).getCustomId() == 1;
    }

    @Test
    public void testRevisionPropertyRestriction() {
        List<Object[]> result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .add(AuditEntity.id().eq(id1))
                .add(AuditEntity.revisionProperty("customTimestamp").ge(timestamp))
                .getResultList();

        assert result.get(0)[0].equals(new StrIntTestEntity("c", 10, id1));
        assert result.get(0)[1] instanceof CustomRevEntity;
        assert ((CustomRevEntity) result.get(0)[1]).getCustomId() == 2;  
        assert ((CustomRevEntity) result.get(0)[1]).getCustomTimestamp() >= timestamp;
    }
}