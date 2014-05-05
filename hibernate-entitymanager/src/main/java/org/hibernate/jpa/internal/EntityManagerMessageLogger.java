/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.internal;

import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-entitymanager module.  It reserves message ids ranging from
 * 15001 to 20000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger( projectCode = "HHH" )
public interface EntityManagerMessageLogger extends CoreMessageLogger {

    @LogMessage( level = INFO )
    @Message( value = "Bound Ejb3Configuration to JNDI name: %s", id = 15001 )
    void boundEjb3ConfigurationToJndiName( String name );

    @LogMessage( level = INFO )
    @Message( value = "Ejb3Configuration name: %s", id = 15002 )
    void ejb3ConfigurationName( String name );

    @LogMessage( level = INFO )
    @Message( value = "An Ejb3Configuration was renamed from name: %s", id = 15003 )
    void ejb3ConfigurationRenamedFromName( String name );

    @LogMessage( level = INFO )
    @Message( value = "An Ejb3Configuration was unbound from name: %s", id = 15004 )
    void ejb3ConfigurationUnboundFromName( String name );

	/**
	 * @deprecated Moved to the {@link org.hibernate.internal.UrlMessageBundle}
	 * contract
	 */
	@Deprecated
    @LogMessage( level = WARN )
    @Message( value = "Exploded jar file does not exist (ignored): %s", id = 15005 )
    void explodedJarDoesNotExist( URL jarUrl );

	/**
	 * @deprecated Moved to the {@link org.hibernate.internal.UrlMessageBundle}
	 * contract
	 */
	@Deprecated
    @LogMessage( level = WARN )
    @Message( value = "Exploded jar file not a directory (ignored): %s", id = 15006 )
    void explodedJarNotDirectory( URL jarUrl );

	/**
	 * @deprecated Moved to the {@link org.hibernate.internal.UrlMessageBundle}
	 * contract
	 */
	@Deprecated
    @LogMessage( level = ERROR )
    @Message( value = "Malformed URL: %s", id = 15008 )
    void malformedUrl( URL jarUrl,
                       @Cause URISyntaxException e );

	/**
	 * @deprecated Moved to the {@link org.hibernate.internal.UrlMessageBundle}
	 * contract
	 */
	@Deprecated
    @LogMessage( level = WARN )
    @Message( value = "Malformed URL: %s", id = 15009 )
    void malformedUrlWarning( URL jarUrl,
                              @Cause URISyntaxException e );

	/**
	 * @deprecated Moved to the {@link org.hibernate.internal.UrlMessageBundle}
	 * contract
	 */
	@Deprecated
    @LogMessage( level = WARN )
    @Message( value = "Unable to find file (ignored): %s", id = 15010 )
    void unableToFindFile( URL jarUrl,
                           @Cause Exception e );

    @LogMessage( level = INFO )
    @Message( value = "Using provided datasource", id = 15012 )
    void usingProvidedDataSource();


    @LogMessage( level = DEBUG )
    @Message( value = "Returning null (as required by JPA spec) rather than throwing EntityNotFoundException, " +
            "as the entity (type=%s, id=%s) does not exist", id = 15013 )
    void ignoringEntityNotFound( String entityName, String identifier);

	@LogMessage( level = WARN )
	@Message(
			value = "DEPRECATION - attempt to refer to JPA positional parameter [?%1$s] using String name [\"%1$s\"] " +
					"rather than int position [%1$s] (generally in Query#setParameter, Query#getParameter or " +
					"Query#getParameterValue calls).  Hibernate previously allowed such usage, but it is considered " +
					"deprecated.",
			id = 15014
	)
	void deprecatedJpaPositionalParameterAccess(Integer jpaPositionalParameter);

	@LogMessage( level = INFO )
	@Message(
			id = 15015,
			value = "Encountered a MappedSuperclass [%s] not used in any entity hierarchy"
	)
	void unusedMappedSuperclass(String name);

	@LogMessage( level = WARN )
	@Message(
			id = 15016,
			value = "Encountered a deprecated javax.persistence.spi.PersistenceProvider [%s]; use [%s] instead."
	)
	void deprecatedPersistenceProvider(String deprecated, String replacement);
}
