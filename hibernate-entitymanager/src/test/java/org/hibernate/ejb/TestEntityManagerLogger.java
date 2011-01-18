/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.ejb;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
public interface TestEntityManagerLogger extends BasicLogger {

    public static final TestEntityManagerLogger LOG = org.jboss.logging.Logger.getMessageLogger(TestEntityManagerLogger.class,
                                                                                                TestEntityManagerLogger.class.getPackage().getName());
}
