/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.contributed;

import java.io.InputStream;

import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.ProcessedMappings;
import org.hibernate.boot.spi.AdditionalMappingContributorContext;

/**
 * @author Steve Ebersole
 */
public class ContributorImpl implements AdditionalMappingContributor {
	public ContributorImpl() {
	}

	@Override
	public String getContributorName() {
		return "test";
	}

	@Override
	public void contribute(
			AdditionalMappingContributions contributions,
			ProcessedMappings processedMappings,
			AdditionalMappingContributorContext contributorContext) {
		final InputStream inputStream = contributorContext.getResourceStreamLocator().locateResourceStream(
				"org/hibernate/orm/test/mapping/contributed/BasicContributorTests.xml" );

		contributions.contributeBinding( inputStream );
	}

}
