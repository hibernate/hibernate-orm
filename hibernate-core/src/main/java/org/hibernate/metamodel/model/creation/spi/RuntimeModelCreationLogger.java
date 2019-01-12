/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Message logger used for runtime model creation logging
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005501, max = 90005600 )
public interface RuntimeModelCreationLogger extends BasicLogger {
	RuntimeModelCreationLogger INSTANCE = Logger.getMessageLogger(
			RuntimeModelCreationLogger.class,
			"org.hibernate.orm.model.creation"
	);

	@LogMessage(level = WARN)
	@Message(value = "Unable to locate static metamodel field [%s#%s]; this may or may not indicate a problem with the static metamodel", id = 90005501)
	void unableToLocateStaticMetamodelField(
			String metamodelClassName,
			String metamodelClassFieldName);

	@LogMessage(level = ERROR)
	@Message(value = "Illegal argument on static metamodel field injection [%s#%s]; expecting `%s`, but encountered `%s`", id = 90005502)
	void illegalArgumentOnStaticMetamodelFieldInjection(
			String metamodelClassName,
			String metamodelClassFieldName,
			String expectedDescriptorType,
			String actualDescriptorType);
}
