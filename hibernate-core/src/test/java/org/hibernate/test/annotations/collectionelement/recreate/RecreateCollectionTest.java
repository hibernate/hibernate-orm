/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.collectionelement.recreate;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Astakhov
 */
public class RecreateCollectionTest extends BaseCoreFunctionalTestCase {

    private static class StatementsCounterListener extends BaseSessionEventListener {
        int statements;

        @Override
        public void jdbcExecuteStatementEnd() {
            statements++;
        }
    }

    @Test
    @TestForIssue(jiraKey = "HHH-9474")
    public void testUpdateCollectionOfElements() throws Exception {
        Session s = openSession();

        s.getTransaction().begin();

        Poi poi1 = new Poi("Poi 1");
        Poi poi2 = new Poi("Poi 2");

        s.save(poi1);
        s.save(poi2);

        RaceExecution race = new RaceExecution();

        s.save(race);

        Date currentTime = new Date();

        race.arriveToPoi(poi1, currentTime);
        race.expectedArrive(poi2, new Date(currentTime.getTime() + 60 * 1000));

        s.flush();

        assertEquals(2, race.getPoiArrival().size());

        StatementsCounterListener statementsCounterListener = new StatementsCounterListener();

        s.addEventListeners(statementsCounterListener);

        race.arriveToPoi(poi2, new Date(currentTime.getTime() + 2 * 60 * 1000));

        s.flush();

        assertEquals(2, race.getPoiArrival().size());

        // There is should be one UPDATE statement. Without fix there is one DELETE and two INSERT-s.

        assertEquals(1, statementsCounterListener.statements);

        s.getTransaction().rollback();
        s.close();
    }

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{
                Poi.class,
                RaceExecution.class
        };
    }
}
