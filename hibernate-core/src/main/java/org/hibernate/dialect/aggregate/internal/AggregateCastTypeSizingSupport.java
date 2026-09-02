/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.internal;

/**
 * Internal marker for aggregate implementations which need resolved column lengths in casts.
 *
 * @author Steve Ebersole
 */
public interface AggregateCastTypeSizingSupport {
	boolean useLengthsInCasts();
}
