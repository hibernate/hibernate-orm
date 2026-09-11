/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.strategy.spi;

import org.hibernate.Incubating;
import org.hibernate.property.access.spi.Getter;

/**
 * A contract to initialize an {@link AuditStrategy}
 *
 * @author Chris Cranford
 */
@Incubating(since = "8.0")
public interface AuditStrategyContext {
	/**
	 * Get the revision entity class name
	 * @return the class name of the revision entity
	 */
	Class<?> getRevisionInfoClass();

	/**
	 * Get the revision info timestamp accessor
	 * @return the getter for the timestamp attribute on the revision entity
	 *
	 * @deprecated no longer used, it will be replaced by a method that will not return the deprecated {@link Getter} interface
	 */
	@Deprecated(since = "7.4", forRemoval = true)
	Getter getRevisionInfoTimestampAccessor();
}
