/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.profile;

/**
 * Models an individual fetch within a profile.
 *
 * @author Steve Ebersole
 */
public class Fetch {
	private final Association association;
	private final Style style;

	/**
	 * Constructs a Fetch
	 *
	 * @param association The association to be fetched
	 * @param style How to fetch it
	 */
	public Fetch(Association association, Style style) {
		this.association = association;
		this.style = style;
	}

	public Association getAssociation() {
		return association;
	}

	public Style getStyle() {
		return style;
	}

	/**
	 * The type or style of fetch.  For the moment we limit this to
	 * join and select, though technically subselect would be valid
	 * here as as well; however, to support subselect here would
	 * require major changes to the subselect loading code (which is
	 * needed for other things as well anyway).
	 */
	public enum Style {
		/**
		 * Fetch via a join
		 */
		JOIN( "join" ),
		/**
		 * Fetch via a subsequent select
		 */
		SELECT( "select" );

		private final String name;

		private Style(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		/**
		 * Parses a style given an externalized string representation
		 *
		 * @param name The externalized representation
		 *
		 * @return The style; {@link #JOIN} is returned if not recognized
		 */
		public static Style parse(String name) {
			if ( SELECT.name.equals( name ) ) {
				return SELECT;
			}
			else {
				// the default...
				return JOIN;
			}
		}
	}

	@Override
	public String toString() {
		return "Fetch[" + style + "{" + association.getRole() + "}]";
	}
}
