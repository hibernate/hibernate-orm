/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.profile;

import java.util.Locale;

/**
 * Models an individual fetch override within a profile.
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
	 * The type or style of fetch.
	 */
	public enum Style {
		/**
		 * Fetch via a join
		 */
		JOIN,
		/**
		 * Fetch via a subsequent select
		 */
		SELECT,
		/**
		 * Fetch via a subsequent subselect
		 */
		SUBSELECT;

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ROOT);
		}

		/**
		 * Parses a style given an externalized string representation
		 *
		 * @param name The externalized representation
		 *
		 * @return The style; {@link #JOIN} is returned if not recognized
		 */
		public static Style parse(String name) {
			for ( Style style: values() ) {
				if ( style.name().equalsIgnoreCase( name ) ) {
					return style;
				}
			}
			return JOIN;
		}
	}

	@Override
	public String toString() {
		return "Fetch[" + style + "{" + association.getRole() + "}]";
	}
}
