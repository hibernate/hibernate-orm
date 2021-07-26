/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.boot.model;

import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;

/**
 * Contract for contributing functions
 *
 * @author Karel Maesen
 */
public interface FunctionContributor {

	/**
	 *  Contribute functions
	 *
	 * @param functionRegistry The target for the contributions
	 * @param serviceRegistry The service registry
	 */
	void contributeFunctions(SqmFunctionRegistry functionRegistry, ServiceRegistry serviceRegistry);

	/**
	 * Determines order in which the contributions will be applied (lowest ordinal first).
	 *
	 * The range 0-500 is reserved for Hibernate, range 500-1000 for libraries and 1000-Integer.MAX_VALUE for
	 * user-defined FunctionContributors.
	 *
	 * @return the ordinal for this FunctionContributor
	 */
	default int ordinal(){
		return 1000;
	}
}
