/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.util;

import org.infinispan.test.fwk.TestResourceTracker;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Infinispan testing utilities take great care to verify isolation between concurrent tests
 * and resource teardown.
 * These utilities need to be integrated with the test lifecycle; use this as a Rule as one
 * way to achieve that integration.
 *
 * @author Sanne Grinovero
 */
public final class InfinispanTestingSetup implements TestRule {

    private volatile String runningTest;

    public InfinispanTestingSetup() {
    }

    public Statement apply(Statement base, Description d) {
        final String methodName = d.getMethodName();
        final String testName = methodName == null ? d.getClassName() : d.getClassName() + "#" + d.getMethodName();
        runningTest = testName;
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                TestResourceTracker.testStarted( testName );
                try {
                    base.evaluate();
                } finally {
                    TestResourceTracker.testFinished( testName );
                }
            }
        };
    }

    public void joinContext() {
        TestResourceTracker.setThreadTestName( runningTest );
    }

}
