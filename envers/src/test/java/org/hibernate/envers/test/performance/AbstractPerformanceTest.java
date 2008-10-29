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
import org.hibernate.envers.tools.Pair;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractPerformanceTest extends AbstractEntityTest {
    protected String getSecondsString(long milliseconds) {
        return (milliseconds/1000) + "." + (milliseconds%1000);
    }

    protected long measureTime(Runnable r) {
        long start = System.currentTimeMillis();
        r.run();
        return System.currentTimeMillis() - start;
    }

    protected abstract Pair<Long, Long> doTest();

    protected abstract String getName();

    private long totalUnversioned;
    private long totalVersioned;

    private void printResults(long unversioned, long versioned) {
        System.out.println("Unversioned: " + getSecondsString(unversioned));
        System.out.println("  Versioned: " + getSecondsString(versioned));
        System.out.println("      Delta: " + getSecondsString(versioned-unversioned));
        System.out.println("     Factor: " + (double)versioned/unversioned);
    }

    private void test(boolean count) {
        Pair<Long, Long> result = doTest();
        long unversioned = result.getFirst();
        long versioned = result.getSecond();

        totalUnversioned += unversioned;
        totalVersioned += versioned;

        printResults(unversioned, versioned);
    }

    public void run(int numberOfRuns) {
        for (int i=0; i<=numberOfRuns; i++) {
            System.out.println("");
            System.out.println(getName() + " TEST, RUN " + i);
            test(i != 0);
        }

        totalUnversioned /= numberOfRuns;
        totalVersioned /= numberOfRuns;

        System.out.println("");
        System.out.println(getName() + " TEST, AVERAGE");
        printResults(totalUnversioned, totalVersioned);
    }
}
