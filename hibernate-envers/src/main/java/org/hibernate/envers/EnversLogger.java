/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.envers;
import static org.jboss.logging.Logger.Level.WARN;
import org.hibernate.HibernateLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;

/**
 * Defines internationalized messages for this hibernate-envers, with IDs ranging from 25001 to 30000 inclusively. New messages must
 * be added after the last message defined to ensure message codes are unique.
 */
public interface EnversLogger extends HibernateLogger {

    @LogMessage( level = WARN )
    @Message( value = "ValidTimeAuditStrategy is deprecated, please use ValidityAuditStrategy instead", id = 25001 )
    void validTimeAuditStrategyDeprecated();
}
