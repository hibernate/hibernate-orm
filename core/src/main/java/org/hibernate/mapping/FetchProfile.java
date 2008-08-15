/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.mapping;

import java.util.LinkedHashSet;

/**
 * A fetch profile allows a user to dynamically modify the fetching
 * strategy used for particular associations at runtime, whereas that
 * information was historically only statically defined in the metadata.
 *
 * @author Steve Ebersole
 */
public class FetchProfile {
	private final String name;
	private LinkedHashSet fetches = new LinkedHashSet();

	public FetchProfile(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public LinkedHashSet getFetches() {
		return fetches;
	}

	public void addFetch(String entity, String association, String style) {
		fetches.add( new Fetch( entity, association, style ) );
	}

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
