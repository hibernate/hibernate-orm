package org.jboss.envers.test.performance;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.tools.Pair;

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
