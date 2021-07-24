/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.contributor;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;

/**
 * Internal contract for TypeContributor
 */
abstract class ContributorImplementor {
	private final ServiceRegistry serviceRegistryegistry;

	ContributorImplementor(ServiceRegistry serviceRegistry) {
		this.serviceRegistryegistry = serviceRegistry;
	}

	abstract void contributeTypes(TypeContributions typeContributions);

	abstract void contributeFunctions(FunctionContributions functionContributions);

	ServiceRegistry getServiceRegistryegistry() {
		return serviceRegistryegistry;
	}
}
