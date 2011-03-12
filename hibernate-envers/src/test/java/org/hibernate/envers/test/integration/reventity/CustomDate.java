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
import java.util.Date;
import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.reventity.CustomDateRevEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CustomDate extends AbstractEntityTest {
    private Integer id;
    private long timestamp1;
    private long timestamp2;
    private long timestamp3;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(CustomDateRevEntity.class);
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
        getAuditReader().getRevisionNumberForDate(new Date(timestamp1));
    }

    @Test
    public void testTimestamps() {
        assert getAuditReader().getRevisionNumberForDate(new Date(timestamp2)).intValue() == 1;
        assert getAuditReader().getRevisionNumberForDate(new Date(timestamp3)).intValue() == 2;
    }

    @Test
    public void testDatesForRevisions() {
        AuditReader vr = getAuditReader();
        assert vr.getRevisionNumberForDate(vr.getRevisionDate(1)).intValue() == 1;
        assert vr.getRevisionNumberForDate(vr.getRevisionDate(2)).intValue() == 2;
    }

    @Test
    public void testRevisionsForDates() {
        AuditReader vr = getAuditReader();

        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp2))).getTime() <= timestamp2;
        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp2)).intValue()+1).getTime() > timestamp2;

        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp3))).getTime() <= timestamp3;
    }

    @Test
    public void testFindRevision() {
        AuditReader vr = getAuditReader();

        long rev1Timestamp = vr.findRevision(CustomDateRevEntity.class, 1).getDateTimestamp().getTime();
        assert rev1Timestamp > timestamp1;
        assert rev1Timestamp <= timestamp2;

        long rev2Timestamp = vr.findRevision(CustomDateRevEntity.class, 2).getDateTimestamp().getTime();
        assert rev2Timestamp > timestamp2;
        assert rev2Timestamp <= timestamp3;
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(StrTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId1() {
        StrTestEntity ver1 = new StrTestEntity("x", id);
        StrTestEntity ver2 = new StrTestEntity("y", id);

        assert getAuditReader().find(StrTestEntity.class, id, 1).equals(ver1);
        assert getAuditReader().find(StrTestEntity.class, id, 2).equals(ver2);
    }
}