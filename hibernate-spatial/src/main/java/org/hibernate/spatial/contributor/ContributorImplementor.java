/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.contributor;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;

/**
 * Internal contract for Type and Function Contributors
 */
public interface ContributorImplementor {

	void contributeTypes(TypeContributions typeContributions);

	void contributeFunctions(FunctionContributions functionContributions);

	ServiceRegistry getServiceRegistry();
}
