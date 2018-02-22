/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.internal.hhh12076;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-12076")
public class HQLJoinClassTest extends BaseCoreFunctionalTestCase {

    // Add your entities here.
    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{
        };
    }

    // If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
    @Override
    protected String[] getMappings() {
        return new String[]{
                "Claim.hbm.xml",
                "EwtAssessmentExtension.hbm.xml",
                "Extension.hbm.xml",
                "GapAssessmentExtension.hbm.xml",
                "Settlement.hbm.xml",
                "SettlementExtension.hbm.xml",
                "SettlementTask.hbm.xml",
                "Task.hbm.xml",
                "TaskStatus.hbm.xml",
        };
    }

    // If those mappings reside somewhere other than resources/org/hibernate/test, change this.
    @Override
    protected String getBaseForMappings() {
        return "org/hibernate/internal/hhh12076/";
    }

    // Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
    @Override
    protected void configure(Configuration configuration) {
        super.configure(configuration);

        configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
        configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());
        //configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
    }

    // Add your tests, using standard JUnit.
    @Test
    public void hhh12076Test() throws Exception {
        // BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        // Do stuff...

        TaskStatus taskStatus = new TaskStatus();
        taskStatus.setName("Enabled");
        taskStatus.setDisplayName("Enabled");
        s.save(taskStatus);

        for (long i = 0; i < 10; i++) {
            SettlementTask settlementTask = new SettlementTask();
            settlementTask.setId(i);
            Settlement settlement = new Settlement();
            settlementTask.setLinked(settlement);
            settlementTask.setStatus(taskStatus);

            Claim claim = new Claim();
            claim.setId(i);
            settlement.setClaim(claim);

            for (int j = 0; j < 2; j++) {
                GapAssessmentExtension gapAssessmentExtension = new GapAssessmentExtension();
                gapAssessmentExtension.setSettlement(settlement);
                EwtAssessmentExtension ewtAssessmentExtension = new EwtAssessmentExtension();
                ewtAssessmentExtension.setSettlement(settlement);

                settlement.getExtensions().add(gapAssessmentExtension);
                settlement.getExtensions().add(ewtAssessmentExtension);
            }
            s.save(claim);
            s.save(settlement);
            s.save(settlementTask);
        }

        final String hql = "select rootAlias.id, linked.id, extensions.id from SettlementTask as rootAlias join rootAlias.linked as linked left join linked.extensions as extensions with extensions.class = org.hibernate.internal.hhh12076.EwtAssessmentExtension where linked.id = :claimId";

        Query<SettlementTask> query = session.createQuery(hql);
        query.setParameter("claimId", 1L);

        List<SettlementTask> results = query.getResultList();
        assertNotNull(results);
        assertTrue(results.size() > 0);

        tx.commit();
        s.close();
    }
}
