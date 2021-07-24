/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.contributor;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.service.ServiceRegistry;

public class SpatialFunctionContributor implements FunctionContributor {

	@Override
	public void contributeTypes(
			FunctionContributions functionContributions, ServiceRegistry serviceRegistry) {
		ContributorImplementor contributorImplementor = ContributorResolver.resolveSpatialtypeContributorImplementor(
				serviceRegistry );

		if ( contributorImplementor != null ) {
			contributorImplementor.contributeFunctions( functionContributions );
		}
	}
}
