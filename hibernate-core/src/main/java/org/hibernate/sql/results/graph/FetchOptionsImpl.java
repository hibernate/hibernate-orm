/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.graph;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;

class FetchOptionsImpl implements FetchOptions {
	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;

	FetchOptionsImpl(FetchTiming fetchTiming, FetchStyle fetchStyle) {
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
	}

	@Override
	public FetchStyle getStyle() {
		return fetchStyle;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}
}
