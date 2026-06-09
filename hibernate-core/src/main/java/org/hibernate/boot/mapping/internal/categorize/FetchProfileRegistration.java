/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.FetchType;

import org.hibernate.annotations.FetchMode;
import org.hibernate.mapping.MetadataSource;

/// Global registration of a fetch profile.
///
/// @see org.hibernate.annotations.FetchProfile
/// @see org.hibernate.boot.jaxb.mapping.spi.JaxbFetchProfileImpl
///
/// @since 9.0
/// @author Steve Ebersole
public record FetchProfileRegistration(String name, MetadataSource source, List<FetchOverride> fetchOverrides) {
	public String getName() {
		return name;
	}

	public MetadataSource getSource() {
		return source;
	}

	public List<FetchOverride> getFetchOverrides() {
		return fetchOverrides;
	}

	public record FetchOverride(String entityName, String association, FetchMode mode, FetchType fetch) {
		public String style() {
			return mode == null ? null : mode.name().toLowerCase( Locale.ROOT );
		}
	}
}
