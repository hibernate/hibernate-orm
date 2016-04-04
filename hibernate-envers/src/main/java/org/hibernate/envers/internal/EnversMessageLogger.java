/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.MessageLogger;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-envers module.  It reserves message ids ranging from
 * 25001 to 30000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
public interface EnversMessageLogger extends BasicLogger {

}
