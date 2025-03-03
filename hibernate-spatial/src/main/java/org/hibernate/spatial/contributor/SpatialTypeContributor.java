/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.contributor;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;

public class SpatialTypeContributor implements TypeContributor {
	@Override
	public void contribute(
			TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		ContributorImplementor contributorImplementor = ContributorResolver.resolveSpatialtypeContributorImplementor( serviceRegistry );

		if (contributorImplementor != null) {
			contributorImplementor.contributeJavaTypes( typeContributions, serviceRegistry );
			contributorImplementor.contributeJdbcTypes( typeContributions, serviceRegistry );
		}

	}
}
