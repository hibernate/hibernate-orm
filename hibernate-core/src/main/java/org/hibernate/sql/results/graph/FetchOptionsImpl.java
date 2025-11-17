/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
