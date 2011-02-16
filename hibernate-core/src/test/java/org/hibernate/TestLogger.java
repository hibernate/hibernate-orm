/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.MessageLogger;

/**
 * Defines internationalized messages for all modules part of hibernate-core.
 */
@MessageLogger( projectCode = "HHH" )
public interface TestLogger extends BasicLogger {

    public static final TestLogger LOG = Logger.getMessageLogger(TestLogger.class, TestLogger.class.getName());
}
