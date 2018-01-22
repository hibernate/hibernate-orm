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
package org.hibernate.internal.hhh12225;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-12225")
public class HQLTypeTest extends BaseCoreFunctionalTestCase {

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
                "Contract.hbm.xml",
                "Vehicle.hbm.xml"
        };
    }

    // If those mappings reside somewhere other than resources/org/hibernate/test, change this.
    @Override
    protected String getBaseForMappings() {
        return "org/hibernate/internal/hhh12225/";
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
    public void hhh12225Test() throws Exception {
        // BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        // Do stuff...

        for (long i = 0; i < 10; i++) {
            VehicleContract vehicleContract = new VehicleContract();
            Vehicle vehicle1 = new Vehicle();
            vehicle1.setContract(vehicleContract);
            VehicleTrackContract vehicleTrackContract = new VehicleTrackContract();
            Vehicle vehicle2 = new Vehicle();
            vehicle2.setContract(vehicleTrackContract);

            s.save(vehicle1);
            s.save(vehicle2);
            s.save(vehicleContract);
            s.save(vehicleTrackContract);
        }

        final String workingHql = "select rootAlias.id from Contract as rootAlias where rootAlias.id = :id";

        Query<Contract> workingQuery = session.createQuery(workingHql);
        workingQuery.setParameter("id", 1L);

        ScrollableResults workingResults = workingQuery.scroll();
        assertNotNull(workingResults);
        assertTrue(workingResults.next());
        Long workingId = (Long)workingResults.get(0);
        assertEquals(Long.valueOf(1), workingId);

        final String failingHql = "select rootAlias.id, type(rootAlias) from Contract as rootAlias where rootAlias.id = :id";

        Query<Contract> failingQuery = session.createQuery(failingHql);
        failingQuery.setParameter("id", 1L);

        ScrollableResults failingResults = failingQuery.scroll();
        assertNotNull(failingResults);
        assertTrue(failingResults.next());
        Long failingId = (Long)failingResults.get(0);
        assertEquals(Long.valueOf(1), failingId);

        tx.commit();
        s.close();
    }
}
