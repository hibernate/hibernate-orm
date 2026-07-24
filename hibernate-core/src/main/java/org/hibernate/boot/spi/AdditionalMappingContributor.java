/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.Incubating;
import org.hibernate.service.JavaServiceLoadable;

/**
 * Contract allowing pluggable contributions of additional mapping objects.
 *
 * Discoverable as a {@linkplain java.util.ServiceLoader Java service}.
 *
 * @author Steve Ebersole
 */
@Incubating
@JavaServiceLoadable
public interface AdditionalMappingContributor {
	/**
	 * The name of this contributor.  May be {@code null}.
	 *
	 * @see org.hibernate.mapping.Contributable
	 */
	default String getContributorName() {
		return null;
	}

	/**
	 * Contribute the additional mappings
	 *
	 * @param contributions Collector of the contributions.
	 * @param processedMappings Immutable view of mappings already processed.
	 * @param contributorContext Access to contextual services.
	 */
	void contribute(
			AdditionalMappingContributions contributions,
			ProcessedMappings processedMappings,
			AdditionalMappingContributorContext contributorContext);
}
