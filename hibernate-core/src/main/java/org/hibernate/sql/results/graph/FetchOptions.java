/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;

/**
 * Represents an aggregated {@link FetchTiming} and {@link FetchStyle} value
 *
 * @author Steve Ebersole
 */
public interface FetchOptions extends FetchTimingAccess, FetchStyleAccess {

	static FetchOptions valueOf(FetchTiming fetchTiming, FetchStyle fetchStyle) {
		return new FetchOptionsImpl( fetchTiming, fetchStyle );
	}
}
