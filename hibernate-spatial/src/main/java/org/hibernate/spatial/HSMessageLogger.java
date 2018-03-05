/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * The logger interface for the Hibernate Spatial module.
 *
 * @author Karel Maesen, Geovise BVBA
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 80000001, max = 80001000)
public interface HSMessageLogger extends BasicLogger {
	@LogMessage(level = INFO)
	@Message(value = "hibernate-spatial integration enabled : %s", id = 80000001)
	void spatialEnabled(boolean enabled);

	@LogMessage(level = INFO)
	@Message(value = "hibernate-spatial using Connection Finder for creating Oracle types : %s", id = 80000002)
	void connectionFinder(String className);

}
