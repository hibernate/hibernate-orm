/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate;

import org.jboss.logging.BasicLogger;

/**
 *
 */
public interface aTestLogger extends BasicLogger {

    public static final aTestLogger LOG = org.jboss.logging.Logger.getMessageLogger(aTestLogger.class,
                                                                                    aTestLogger.class.getPackage().getName());
}
