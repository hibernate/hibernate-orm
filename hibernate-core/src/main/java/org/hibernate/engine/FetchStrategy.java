/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine;

/**
 * Describes the strategy for fetching an association, which includes both when and how.
 *
 * @author Steve Ebersole
 */
public class FetchStrategy {
	private final FetchTiming timing;
	private final FetchStyle style;

	/**
	 * Constructs a FetchStrategy.
	 *
	 * @param timing The fetch timing (the when)
	 * @param style The fetch style (the how).
	 */
	public FetchStrategy(FetchTiming timing, FetchStyle style) {
		this.timing = timing;
		this.style = style;
	}

	public FetchTiming getTiming() {
		return timing;
	}

	public FetchStyle getStyle() {
		return style;
	}
}
