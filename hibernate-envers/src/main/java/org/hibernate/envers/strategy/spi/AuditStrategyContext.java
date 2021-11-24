/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy.spi;

import org.hibernate.Incubating;
import org.hibernate.property.access.spi.Getter;

/**
 * A contract to initialize an {@link AuditStrategy}
 *
 * @author Chris Cranford
 */
@Incubating
public interface AuditStrategyContext {
	/**
	 * Get the revision entity class name
	 * @return the class name of the revision entity
	 */
	Class<?> getRevisionInfoClass();

	/**
	 * Get the revision info timestamp accessor
	 * @return the getter for the timestamp attribute on the revision entity
	 */
	Getter getRevisionInfoTimestampAccessor();
}
