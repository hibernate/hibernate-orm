/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.id.IntegralDataTypeHolder;

/**
 * Contract for providing callback access to a {@link DatabaseStructure},
 * typically from the {@link Optimizer}.
 *
 * @author Steve Ebersole
 */
public interface AccessCallback {
	/**
	 * Retrieve the next value from the underlying source.
	 *
	 * @return The next value.
	 */
	IntegralDataTypeHolder getNextValue();

	/**
	 * Obtain the tenant identifier (multi-tenancy), if one, associated with this callback.
	 *
	 * @return The tenant identifier
	 */
	String getTenantIdentifier();
}
