/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractPerformanceTest extends AbstractEntityManagerTest {
	protected String getSecondsString(long milliseconds) {
		return (milliseconds / 1000) + "." + (milliseconds % 1000);
	}

	protected abstract void doTest();

	private void printResults(long unaudited, long audited) {
		System.out.println( "Unaudited: " + getSecondsString( unaudited ) );
		System.out.println( "  Audited: " + getSecondsString( audited ) );
		System.out.println( "    Delta: " + getSecondsString( audited - unaudited ) );
		System.out.println( "   Factor: " + (double) audited / unaudited );
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
		for ( int i = 0; i <= numberOfRuns; i++ ) {
			System.out.println();
			System.out.println( "RUN " + i );
			reset();
			doTest();
			results.add( runTotal );
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

		init( true, null );
		long audited = run( numberOfRuns, auditedRuns );
		close();

		init( false, null );
		long unaudited = run( numberOfRuns, unauditedRuns );
		close();

		for ( int i = 0; i <= numberOfRuns; i++ ) {
			System.out.println( "RUN " + i );
			printResults( unauditedRuns.get( i ), auditedRuns.get( i ) );
			System.out.println();
		}

		System.out.println( "TOTAL" );
		printResults( unaudited, audited );
	}
}
