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
package org.hibernate.envers.test.integration.ids;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.ids.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DateId extends AbstractEntityTest {
    private Date id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(DateIdTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        DateIdTestEntity dite = new DateIdTestEntity(new Date(), "x");
        em.persist(dite);

        id1 = dite.getId();

        em.getTransaction().commit();

        // Revision 2
        em = getEntityManager();
        em.getTransaction().begin();

        dite = em.find(DateIdTestEntity.class, id1);
        dite.setStr1("y");

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(DateIdTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        DateIdTestEntity ver1 = new DateIdTestEntity(id1, "x");
        DateIdTestEntity ver2 = new DateIdTestEntity(id1, "y");

        assert getAuditReader().find(DateIdTestEntity.class, id1, 1).getStr1().equals("x");
        assert getAuditReader().find(DateIdTestEntity.class, id1, 2).getStr1().equals("y");
    }
}