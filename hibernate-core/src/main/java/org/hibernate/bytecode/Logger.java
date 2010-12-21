/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.bytecode;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
public interface Logger extends BasicLogger {

    @LogMessage( level = WARN )
    @Message( value = "Per HHH-5451 support for cglib as a bytecode provider has been deprecated." )
    void deprecated();

    @LogMessage( level = DEBUG )
    @Message( value = "Enhancing %s" )
    void enhancingClass( String className );

    @LogMessage( level = DEBUG )
    @Message( value = "reflection optimizer disabled for: %s [%s: %s" )
    void reflectionOptimizerDisabled( String className,
                                      String simpleClassName,
                                      String errorMessage );

    @LogMessage( level = DEBUG )
    @Message( value = "reflection optimizer disabled for: %s [%s: %s (property %s)" )
    void reflectionOptimizerDisabledForBulkException( String className,
                                                      String simpleClassName,
                                                      String errorMessage,
                                                      String setterName );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to build enhancement metamodel for %s" )
    void unableToBuildEnhancementMetamodel( String className );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to read class: %s" )
    void unableToReadClass( String message );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to transform class: %s" )
    void unableToTransformClass( String message );
}
