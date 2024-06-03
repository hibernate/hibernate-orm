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
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * {@link MessageLogger} originally for the no-longer existing hibernate-entitymanager module.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange( min = 15001, max = 20000 )
public interface EntityManagerMessageLogger extends CoreMessageLogger {

	@LogMessage(level = ERROR)
	@Message(value = "Illegal argument on static metamodel field injection : %s#%s; expected type :  %s; encountered type : %s", id = 15007)
	void illegalArgumentOnStaticMetamodelFieldInjection(
			String name,
			String name2,
			String name3,
			String name4);

	@LogMessage(level = WARN)
	@Message(value = "Unable to locate static metamodel field : %s#%s; this may or may not indicate a problem with the static metamodel", id = 15011)
	void unableToLocateStaticMetamodelField(
			String name,
			String name2);

	@LogMessage(level = DEBUG)
	@Message(value = "Returning null (as required by JPA spec) rather than throwing EntityNotFoundException, " +
			"as the entity (type=%s, id=%s) does not exist", id = 15013)
	void ignoringEntityNotFound(String entityName, String identifier);

	@LogMessage(level = DEBUG)
	@Message(
			id = 15015,
			value = "Encountered a MappedSuperclass [%s] not used in any entity hierarchy"
	)
	void unusedMappedSuperclass(String name);

	@LogMessage(level = WARN)
	@Message(
			id = 15016,
			value = "Encountered a deprecated jakarta.persistence.spi.PersistenceProvider [%s]; [%s] will be used instead."
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
