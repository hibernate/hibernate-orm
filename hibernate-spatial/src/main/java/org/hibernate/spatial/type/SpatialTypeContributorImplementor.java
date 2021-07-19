/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.type;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;

/**
 * Internal contract for TypeContributor
 */
abstract class SpatialTypeContributorImplementor {
	private final ServiceRegistry serviceRegistryegistry;

	SpatialTypeContributorImplementor(ServiceRegistry serviceRegistry) {
		this.serviceRegistryegistry = serviceRegistry;
	}

	abstract void contribute(TypeContributions typeContributions);


	ServiceRegistry getServiceRegistryegistry() {
		return serviceRegistryegistry;
	}
}
