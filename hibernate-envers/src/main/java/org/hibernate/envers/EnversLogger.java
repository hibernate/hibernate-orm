/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.envers;

import static org.jboss.logging.Logger.Level.WARN;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
public interface EnversLogger extends BasicLogger {

    @LogMessage( level = WARN )
    @Message( value = "ValidTimeAuditStrategy is deprecated, please use ValidityAuditStrategy instead" )
    void validTimeAuditStrategyDeprecated();
}
