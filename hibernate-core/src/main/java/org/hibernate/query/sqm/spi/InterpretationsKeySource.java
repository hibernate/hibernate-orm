/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.spi;

// Used by Hibernate Reactive
public interface InterpretationsKeySource extends CacheabilityInfluencers {
	Class<?> getResultType();
}
