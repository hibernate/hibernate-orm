/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.mapping;
import java.util.LinkedHashSet;

/**
 * A fetch profile allows a user to dynamically modify the fetching strategy used for particular associations at
 * runtime, whereas that information was historically only statically defined in the metadata.
 * <p/>
 * This class represent the data as it is defined in their metadata.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.engine.profile.FetchProfile
 */
public class FetchProfile {
	private final String name;
	private final MetadataSource source;
	private LinkedHashSet<Fetch> fetches = new LinkedHashSet<Fetch>();

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
	 *
	 * @param entity The entity which contains the association to be fetched
	 * @param association The association to fetch
	 * @param style The style of fetch t apply
	 */
	public void addFetch(String entity, String association, String style) {
		fetches.add( new Fetch( entity, association, style ) );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		FetchProfile that = ( FetchProfile ) o;

		return name.equals( that.name );
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return name.hashCode();
	}


	/**
	 * Defines an individual association fetch within the given profile.
	 */
	public static class Fetch {
		private final String entity;
		private final String association;
		private final String style;

		public Fetch(String entity, String association, String style) {
			this.entity = entity;
			this.association = association;
			this.style = style;
		}

		public String getEntity() {
			return entity;
		}

		public String getAssociation() {
			return association;
		}

		public String getStyle() {
			return style;
		}
	}
}
