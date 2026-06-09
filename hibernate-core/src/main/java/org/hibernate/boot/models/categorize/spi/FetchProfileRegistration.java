/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.List;

/// Global registration of a fetch profile.
///
/// @see org.hibernate.annotations.FetchProfile
/// @see org.hibernate.boot.jaxb.mapping.spi.JaxbFetchProfileImpl
///
/// @since 9.0
/// @author Steve Ebersole
public record FetchProfileRegistration(String name, List<FetchOverride> fetchOverrides) {
	public String getName() {
		return name;
	}

	public List<FetchOverride> getFetchOverrides() {
		return fetchOverrides;
	}

	public record FetchOverride(String entityName, String association, String style) {
	}
}
