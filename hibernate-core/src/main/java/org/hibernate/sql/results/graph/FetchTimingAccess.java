/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.engine.FetchTiming;

/**
 * Access to a FetchTiming
 *
 * @author Steve Ebersole
 */
public interface FetchTimingAccess {
	FetchTiming getTiming();
}
