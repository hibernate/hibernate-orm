/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import jakarta.persistence.FetchType;
import org.hibernate.annotations.FetchMode;

import java.util.LinkedHashSet;

/**
 * A mapping model object representing a {@link org.hibernate.annotations.FetchProfile}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.engine.profile.FetchProfile
 */
public class FetchProfile {

	private final String name;
	private final MetadataSource source;
	private final LinkedHashSet<Fetch> fetches = new LinkedHashSet<>();

	/**
	 * Create a fetch profile representation.
	 *
	 * @param name The name of the fetch profile.
	 * @param source The source of the fetch profile (where was it defined).
	 */
	public FetchProfile(String name, MetadataSource source) {
		this.name = name;
		this.source = source;
	}

	/**
	 * Retrieve the name of the fetch profile.
	 *
	 * @return The profile name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieve the fetch profile source.
	 *
	 * @return The profile source.
	 */
	public MetadataSource getSource() {
		return source;
	}

	/**
	 * Retrieve the fetches associated with this profile
	 *
	 * @return The fetches associated with this profile.
	 */
	public LinkedHashSet<Fetch> getFetches() {
		return fetches;
	}

	/**
	 * Adds a fetch to this profile.
	 */
	public void addFetch(Fetch fetch) {
		fetches.add( fetch );
	}

	@Override
	public boolean equals(Object that) {
		return this == that
			|| that instanceof FetchProfile profile
				&& name.equals( profile.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}


	/**
	 * An individual association fetch within the given profile.
	 */
	public static class Fetch {
		private final String entity;
		private final String association;
		private final FetchMode method;
		private final FetchType type;

		public Fetch(String entity, String association, FetchMode method, FetchType type) {
			this.entity = entity;
			this.association = association;
			this.method = method;
			this.type = type;
		}

		public String getEntity() {
			return entity;
		}

		public String getAssociation() {
			return association;
		}

		public FetchMode getMethod() {
			return method;
		}

		public FetchType getType() {
			return type;
		}
	}
}
