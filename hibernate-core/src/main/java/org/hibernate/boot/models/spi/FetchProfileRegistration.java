/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import java.util.List;

/**
 * Global registration of a fetch profile
 *
 * @see org.hibernate.annotations.FetchProfile
 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbFetchProfileImpl
 */
public class FetchProfileRegistration {
	private final String name;
	private final List<FetchOverride> fetchOverrides;

	public FetchProfileRegistration(String name, List<FetchOverride> fetchOverrides) {
		this.name = name;
		this.fetchOverrides = fetchOverrides;
	}

	public String getName() {
		return name;
	}

	public List<FetchOverride> getFetchOverrides() {
		return fetchOverrides;
	}

	public record FetchOverride(String entityName, String association, String style) {
	}
}
