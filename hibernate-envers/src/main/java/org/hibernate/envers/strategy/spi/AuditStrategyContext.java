/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.strategy.spi;

import org.hibernate.Incubating;
import org.hibernate.property.access.spi.PropertyValueAccessor;

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
	PropertyValueAccessor getRevisionInfoTimestampAccessor();
}
