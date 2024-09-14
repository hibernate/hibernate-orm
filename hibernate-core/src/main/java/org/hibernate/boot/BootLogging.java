/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;


/**
 * Logging related to Hibernate bootstrapping
 */
@SubSystemLogging(
		name = BootLogging.NAME,
		description = "Logging related to bootstrapping of a SessionFactory / EntityManagerFactory"
)
public interface BootLogging {
	String NAME = SubSystemLogging.BASE + ".boot";
	Logger BOOT_LOGGER = Logger.getLogger( NAME );
}
