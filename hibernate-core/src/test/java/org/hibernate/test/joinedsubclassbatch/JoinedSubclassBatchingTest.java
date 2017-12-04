/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinedsubclassbatch;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test batching of insert,update,delete on joined subclasses
 * @author dcebotarenco
 */
@TestForIssue(jiraKey = "HHH-2558")
public class JoinedSubclassBatchingTest extends BaseCoreFunctionalTestCase {
    @Override
    public String[] getMappings() {
        return new String[]{"joinedsubclassbatch/Person.hbm.xml"};
    }

    @Override
    public void configure(Configuration cfg) {
        cfg.setProperty(Environment.STATEMENT_BATCH_SIZE, "20");
    }

    @Test
    public void doBatchInsertUpdateJoinedSubclassNrEqualWithBatch() {
        doBatchInsertUpdateJoined(20,20);
    }

    @Test
    public void doBatchInsertUpdateJoinedSubclassNrLessThenBatch() {
        doBatchInsertUpdateJoined(19,20);
    }

    @Test
    public void doBatchInsertUpdateJoinedSubclassNrBiggerThenBatch() {
        doBatchInsertUpdateJoined(21,20);
    }

    @Test
    public void testBatchInsertUpdateSizeEqJdbcBatchSize() {
        int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
        doBatchInsertUpdateJoined(50, batchSize);
    }

    @Test
    public void testBatchInsertUpdateSizeLtJdbcBatchSize() {
        int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
        doBatchInsertUpdateJoined(50, batchSize - 1);
    }

    @Test
    public void testBatchInsertUpdateSizeGtJdbcBatchSize() {
        int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
        doBatchInsertUpdateJoined(50, batchSize + 1);
    }

    public void doBatchInsertUpdateJoined(int nEntities, int nBeforeFlush) {
        final Logger afelLogger = LogManager.getLogger(
                "org.hibernate");
        final Level afelLevel = afelLogger.getLevel();

        try {
            afelLogger.setLevel(Level.DEBUG);


            Session s = openSession();
            s.setCacheMode(CacheMode.IGNORE);
            Transaction t = s.beginTransaction();
            for (int i = 0; i < nEntities; i++) {
                Employee e = new Employee();
                e.getId();
                e.setName("Mark");
                e.setTitle("internal sales");
                e.setSex('M');
                e.setAddress("buckhead");
                e.setZip("30305");
                e.setCountry("USA");
                s.save(e);
                if (i % nBeforeFlush == 0 && i > 0) {
                    s.flush();
                    s.clear();
                }

            }
            t.commit();
            s.close();

            s = openSession();
            s.setCacheMode(CacheMode.IGNORE);
            t = s.beginTransaction();
            int i = 0;
            ScrollableResults sr = s.createQuery("from Employee e")
                    .scroll(ScrollMode.FORWARD_ONLY);
            while (sr.next()) {
                Employee e = (Employee) sr.get(0);
                e.setTitle("Unknown");
                if (i % nBeforeFlush == 0 && i > 0) {
                    s.flush();
                    s.clear();
                }
            }
            t.commit();
            s.close();

            s = openSession();
            s.setCacheMode(CacheMode.IGNORE);
            t = s.beginTransaction();
            i = 0;
            sr = s.createQuery("from Employee e")
                    .scroll(ScrollMode.FORWARD_ONLY);
            while (sr.next()) {
                Employee e = (Employee) sr.get(0);
                s.delete(e);
                if (i % nBeforeFlush == 0 && i > 0) {
                    s.flush();
                    s.clear();
                }
            }
            t.commit();
            s.close();
        } finally {
            // set back previous logging level
            afelLogger.setLevel(afelLevel);
        }
    }

}

