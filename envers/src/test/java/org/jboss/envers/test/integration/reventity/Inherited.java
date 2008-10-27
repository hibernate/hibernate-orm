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
package org.jboss.envers.test.integration.reventity;

import java.util.Arrays;
import java.util.Date;
import javax.persistence.EntityManager;

import org.jboss.envers.VersionsReader;
import org.jboss.envers.exception.RevisionDoesNotExistException;
import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.StrTestEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Inherited extends AbstractEntityTest {
    private Integer id;
    private long timestamp1;
    private long timestamp2;
    private long timestamp3;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(InheritedRevEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() throws InterruptedException {
        timestamp1 = System.currentTimeMillis();

        Thread.sleep(100);

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        StrTestEntity te = new StrTestEntity("x");
        em.persist(te);
        id = te.getId();
        em.getTransaction().commit();

        timestamp2 = System.currentTimeMillis();

        Thread.sleep(100);

        // Revision 2
        em.getTransaction().begin();
        te = em.find(StrTestEntity.class, id);
        te.setStr("y");
        em.getTransaction().commit();

        timestamp3 = System.currentTimeMillis();
    }

    @Test(expectedExceptions = RevisionDoesNotExistException.class)
    public void testTimestamps1() {
        getVersionsReader().getRevisionNumberForDate(new Date(timestamp1));
    }

    @Test
    public void testTimestamps() {
        assert getVersionsReader().getRevisionNumberForDate(new Date(timestamp2)).intValue() == 1;
        assert getVersionsReader().getRevisionNumberForDate(new Date(timestamp3)).intValue() == 2;
    }

    @Test
    public void testDatesForRevisions() {
        VersionsReader vr = getVersionsReader();
        assert vr.getRevisionNumberForDate(vr.getRevisionDate(1)).intValue() == 1;
        assert vr.getRevisionNumberForDate(vr.getRevisionDate(2)).intValue() == 2;
    }

    @Test
    public void testRevisionsForDates() {
        VersionsReader vr = getVersionsReader();

        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp2))).getTime() <= timestamp2;
        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp2)).intValue()+1).getTime() > timestamp2;

        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp3))).getTime() <= timestamp3;
    }

    @Test
    public void testFindRevision() {
        VersionsReader vr = getVersionsReader();

        long rev1Timestamp = vr.findRevision(InheritedRevEntity.class, 1).getTimestamp();
        assert rev1Timestamp > timestamp1;
        assert rev1Timestamp <= timestamp2;

        long rev2Timestamp = vr.findRevision(InheritedRevEntity.class, 2).getTimestamp();
        assert rev2Timestamp > timestamp2;
        assert rev2Timestamp <= timestamp3;
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(StrTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId1() {
        StrTestEntity ver1 = new StrTestEntity("x", id);
        StrTestEntity ver2 = new StrTestEntity("y", id);

        assert getVersionsReader().find(StrTestEntity.class, id, 1).equals(ver1);
        assert getVersionsReader().find(StrTestEntity.class, id, 2).equals(ver2);
    }
}
