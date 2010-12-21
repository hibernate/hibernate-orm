/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.dialect.lock;

import static org.jboss.logging.Logger.Level.WARN;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
interface Logger extends BasicLogger {

    @LogMessage( level = WARN )
    @Message( value = "Write locks via update not supported for non-versioned entities [%s]" )
    void writeLocksNotSupported( String entityName );
}
