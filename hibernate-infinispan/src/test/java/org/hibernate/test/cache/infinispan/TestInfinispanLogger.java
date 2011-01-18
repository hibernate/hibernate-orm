/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.test.cache.infinispan;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
public interface TestInfinispanLogger extends BasicLogger {

    public static final TestInfinispanLogger LOG = org.jboss.logging.Logger.getMessageLogger(TestInfinispanLogger.class,
                                                                                             TestInfinispanLogger.class.getPackage().getName());
}
