/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.contributor;

import aQute.bnd.annotation.spi.ServiceProvider;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;

@ServiceProvider(value = FunctionContributor.class)
public class SpatialFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final ContributorImplementor contributorImplementor = ContributorResolver.resolveSpatialtypeContributorImplementor(
				functionContributions.getServiceRegistry()
		);

		if ( contributorImplementor != null ) {
			contributorImplementor.contributeFunctions( functionContributions );
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
