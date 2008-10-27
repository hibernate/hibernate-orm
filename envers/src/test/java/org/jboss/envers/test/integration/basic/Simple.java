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
package org.jboss.envers.test.integration.basic;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.IntTestEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Simple extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        IntTestEntity ite = new IntTestEntity(10);
        em.persist(ite);
        id1 = ite.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        ite = em.find(IntTestEntity.class, id1);
        ite.setNumber(20);
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(IntTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        IntTestEntity ver1 = new IntTestEntity(10, id1);
        IntTestEntity ver2 = new IntTestEntity(20, id1);

        assert getVersionsReader().find(IntTestEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(IntTestEntity.class, id1, 2).equals(ver2);
    }
}