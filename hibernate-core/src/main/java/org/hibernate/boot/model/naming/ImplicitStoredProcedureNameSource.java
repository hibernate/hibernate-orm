/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.Incubating;

/**
 * Context for determining an implicit stored procedure name.
 *
 * @author Gavin King
 */
@Incubating
public interface ImplicitStoredProcedureNameSource {
	/**
	 * The operation prefix, for example, {@code insert}, {@code update},
	 * {@code delete}, or {@code get}.
	 */
	String getOperation();

	/**
	 * The associated entity name or collection role path.
	 */
	String getRolePath();
}
