/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.profile.FetchProfile;

import static java.util.Collections.unmodifiableSet;

/// Runtime fetch-profile registry used by the SQL translation engine.
///
/// The registry is created before runtime model initialization finishes because
/// the SQL translation engine is created before persisters are complete.  Its
/// contents are populated later, after the mapping metamodel has finished
/// building the persisters needed to resolve fetch-profile declarations.
///
/// @since 9.0
/// @author Steve Ebersole
public final class FetchProfileRegistry {
	private final Map<String, FetchProfile> fetchProfiles = new HashMap<>();

	public void put(String name, FetchProfile fetchProfile) {
		fetchProfiles.put( name, fetchProfile );
	}

	public FetchProfile get(String name) {
		return fetchProfiles.get( name );
	}

	public boolean contains(String name) {
		return fetchProfiles.containsKey( name );
	}

	public Set<String> names() {
		return unmodifiableSet( fetchProfiles.keySet() );
	}
}
