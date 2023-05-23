/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.profile;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;

import java.util.Locale;

import static org.hibernate.engine.FetchTiming.IMMEDIATE;

/**
 * Models an individual fetch override within a {@link FetchProfile}.
 *
 * @author Steve Ebersole
 */
public class Fetch {
	private final Association association;
	private final FetchStyle method;
	private final FetchTiming timing;

	/**
	 * Constructs a {@link Fetch}.
	 *
	 * @param association The association to be fetched
	 * @param style How to fetch it
	 *
	 * @deprecated use {@link #Fetch(Association,FetchStyle,FetchTiming)}
	 */
	@Deprecated(forRemoval = true)
	public Fetch(Association association, Style style) {
		this.association = association;
		this.method = style.toFetchStyle();
		this.timing = IMMEDIATE;
	}

	/**
	 * Constructs a {@link Fetch}.
	 *
	 * @param association The association to be fetched
	 * @param method How to fetch it
	 */
	public Fetch(Association association, FetchStyle method, FetchTiming timing) {
		this.association = association;
		this.method = method;
		this.timing = timing;
	}

	/**
	 * The association to which the fetch style applies.
	 */
	public Association getAssociation() {
		return association;
	}

	/**
	 * The fetch style applied to the association.
	 *
	 * @deprecated use {@link #getMethod()}
	 */
	@Deprecated(forRemoval = true)
	public Style getStyle() {
		return Style.fromFetchStyle( method );
	}

	/**
	 * The fetch method to be applied to the association.
	 */
	public FetchStyle getMethod() {
		return method;
	}

	/**
	 * The fetch timing to be applied to the association.
	 */
	public FetchTiming getTiming() {
		return timing;
	}

	/**
	 * The type or style of fetch.
	 *
	 * @deprecated Use {@link FetchStyle}
	 */
	@Deprecated(forRemoval = true)
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

		public FetchStyle toFetchStyle() {
			switch (this) {
				case SELECT:
					return FetchStyle.SELECT;
				case SUBSELECT:
					return FetchStyle.SUBSELECT;
				case JOIN:
					return FetchStyle.JOIN;
				default:
					throw new AssertionFailure("Unknown Fetch.Style");
			}
		}

		static Style fromFetchStyle(FetchStyle fetchStyle) {
			switch (fetchStyle) {
				case SELECT:
					return SELECT;
				case SUBSELECT:
					return SUBSELECT;
				case JOIN:
					return JOIN;
				default:
					throw new IllegalArgumentException("Unhandled FetchStyle");
			}
		}

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

		public static Style forMethod(FetchMode fetchMode) {
			switch ( fetchMode ) {
				case JOIN:
					return JOIN;
				case SELECT:
					return SELECT;
				case SUBSELECT:
					return SUBSELECT;
				default:
					throw new IllegalArgumentException( "Unknown FetchMode" );
			}
		}
	}

	@Override
	public String toString() {
		return "Fetch[" + method + "{" + association.getRole() + "}]";
	}
}
