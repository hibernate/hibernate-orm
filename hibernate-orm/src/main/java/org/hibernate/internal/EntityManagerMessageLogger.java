/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.net.URISyntaxException;
import java.net.URL;

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
 * New messages must be added afterQuery the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
public interface EntityManagerMessageLogger extends CoreMessageLogger {

	@LogMessage(level = INFO)
	@Message(value = "Bound Ejb3Configuration to JNDI name: %s", id = 15001)
	void boundEjb3ConfigurationToJndiName(String name);

	@LogMessage(level = INFO)
	@Message(value = "Ejb3Configuration name: %s", id = 15002)
	void ejb3ConfigurationName(String name);

	@LogMessage(level = INFO)
	@Message(value = "An Ejb3Configuration was renamed from name: %s", id = 15003)
	void ejb3ConfigurationRenamedFromName(String name);

	@LogMessage(level = INFO)
	@Message(value = "An Ejb3Configuration was unbound from name: %s", id = 15004)
	void ejb3ConfigurationUnboundFromName(String name);

	@LogMessage(level = WARN)
	@Message(value = "Exploded jar file does not exist (ignored): %s", id = 15005)
	void explodedJarDoesNotExist(URL jarUrl);

	@LogMessage(level = WARN)
	@Message(value = "Exploded jar file not a directory (ignored): %s", id = 15006)
	void explodedJarNotDirectory(URL jarUrl);

	@LogMessage(level = ERROR)
	@Message(value = "Illegal argument on static metamodel field injection : %s#%s; expected type :  %s; encountered type : %s", id = 15007)
	void illegalArgumentOnStaticMetamodelFieldInjection(
			String name,
			String name2,
			String name3,
			String name4);

	@LogMessage(level = ERROR)
	@Message(value = "Malformed URL: %s", id = 15008)
	void malformedUrl(
			URL jarUrl,
			@Cause URISyntaxException e);

	@LogMessage(level = WARN)
	@Message(value = "Malformed URL: %s", id = 15009)
	void malformedUrlWarning(
			URL jarUrl,
			@Cause URISyntaxException e);

	@LogMessage(level = WARN)
	@Message(value = "Unable to find file (ignored): %s", id = 15010)
	void unableToFindFile(
			URL jarUrl,
			@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(value = "Unable to locate static metamodel field : %s#%s; this may or may not indicate a problem with the static metamodel", id = 15011)
	void unableToLocateStaticMetamodelField(
			String name,
			String name2);

	@LogMessage(level = INFO)
	@Message(value = "Using provided datasource", id = 15012)
	void usingProvidedDataSource();


	@LogMessage(level = DEBUG)
	@Message(value = "Returning null (as required by JPA spec) rather than throwing EntityNotFoundException, " +
			"as the entity (type=%s, id=%s) does not exist", id = 15013)
	void ignoringEntityNotFound(String entityName, String identifier);

	@LogMessage(level = WARN)
	@Message(
			value = "DEPRECATION - attempt to refer to JPA positional parameter [?%1$s] using String name [\"%1$s\"] " +
					"rather than int position [%1$s] (generally in Query#setParameter, Query#getParameter or " +
					"Query#getParameterValue calls).  Hibernate previously allowed such usage, but it is considered " +
					"deprecated.",
			id = 15014
	)
	void deprecatedJpaPositionalParameterAccess(Integer jpaPositionalParameter);

	@LogMessage(level = INFO)
	@Message(
			id = 15015,
			value = "Encountered a MappedSuperclass [%s] not used in any entity hierarchy"
	)
	void unusedMappedSuperclass(String name);

	@LogMessage(level = WARN)
	@Message(
			id = 15016,
			value = "Encountered a deprecated javax.persistence.spi.PersistenceProvider [%s]; use [%s] instead."
	)
	void deprecatedPersistenceProvider(String deprecated, String replacement);

	@LogMessage(level = WARN)
	@Message(
			id = 15017,
			value = "'hibernate.ejb.use_class_enhancer' property is deprecated. " +
					"Use 'hibernate.enhance.enable[...]' properties instead to enable each individual feature."
	)
	void deprecatedInstrumentationProperty();

	@LogMessage(level = WARN)
	@Message(
			id = 15018,
			value = "Encountered multiple persistence-unit stanzas defining same name [%s]; persistence-unit names must be unique"
	)
	void duplicatedPersistenceUnitName(String name);

}
