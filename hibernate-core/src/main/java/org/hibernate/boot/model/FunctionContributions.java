/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.boot.model;

import org.hibernate.query.sqm.function.SqmFunctionDescriptor;

/**
 * Defines the target for contributing functions via {@link FunctionContributor}
 *
 * @author Karel Maesen
 */
public interface FunctionContributions {
	/**
	 * Add the function under the specified name
	 * @param registrationKey the name under which to register the function
	 * @param function the function description
	 */
	void contributeFunction(String registrationKey, SqmFunctionDescriptor function);
}
