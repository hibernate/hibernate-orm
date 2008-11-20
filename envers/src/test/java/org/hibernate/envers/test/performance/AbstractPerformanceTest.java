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
package org.hibernate.envers.test.performance;

import org.hibernate.envers.test.AbstractEntityTest;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractPerformanceTest extends AbstractEntityTest {
    protected String getSecondsString(long milliseconds) {
        return (milliseconds/1000) + "." + (milliseconds%1000);
    }

    protected abstract void doTest();

    private void printResults(long unaudited, long audited) {
        System.out.println("Unaudited: " + getSecondsString(unaudited));
        System.out.println("  Audited: " + getSecondsString(audited));
        System.out.println("    Delta: " + getSecondsString(audited-unaudited));
        System.out.println("   Factor: " + (double)audited/unaudited);
    }

    private long startTime;
    private long runTotal;

    protected void start() {
        startTime = System.currentTimeMillis();
    }

    protected void stop() {
        long stopTime = System.currentTimeMillis();
        runTotal += stopTime - startTime;
    }

    protected void reset() {
        runTotal = 0;
    }

    public long run(int numberOfRuns, List<Long> results) {
        long total = 0;
        for (int i=0; i<=numberOfRuns; i++) {
            System.out.println();
            System.out.println("RUN " + i);
            reset();
            doTest();
            results.add(runTotal);
            total += runTotal;

            newEntityManager();

            /*System.gc();
            System.gc();
            System.gc();
            System.out.println(Runtime.getRuntime().freeMemory() + ", " + Runtime.getRuntime().totalMemory() + ", "
                    + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));*/
        }

        return total;
    }

    public void test(int numberOfRuns) throws IOException {
        List<Long> unauditedRuns = new ArrayList<Long>();
        List<Long> auditedRuns = new ArrayList<Long>();

        init(true);
        long audited = run(numberOfRuns, auditedRuns);
        close();

        init(false);
        long unaudited = run(numberOfRuns, unauditedRuns);
        close();

        for (int i=0; i<=numberOfRuns; i++) {
            System.out.println("RUN " + i);
            printResults(unauditedRuns.get(i), auditedRuns.get(i));
            System.out.println();
        }

        System.out.println("TOTAL");
        printResults(unaudited, audited);
    }
}
