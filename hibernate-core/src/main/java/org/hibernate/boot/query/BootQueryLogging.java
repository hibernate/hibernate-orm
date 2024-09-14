/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.query;

import org.hibernate.boot.BootLogging;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = BootQueryLogging.NAME,
		description = "Logging related to processing of named-queries"
)
public interface BootQueryLogging {
	String NAME = BootLogging.NAME + ".query";
	Logger BOOT_QUERY_LOGGER = Logger.getLogger( NAME );
}
