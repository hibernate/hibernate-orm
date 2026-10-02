package org.hibernate.spatial.contributor;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;

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
