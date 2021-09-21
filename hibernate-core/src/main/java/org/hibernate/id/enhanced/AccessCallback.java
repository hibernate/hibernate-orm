/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public IntegralDataTypeHolder getNextValue();

	/**
	 * Obtain the tenant identifier (multi-tenancy), if one, associated with this callback.
	 *
	 * @return The tenant identifier
	 */
	public String getTenantIdentifier();
}
