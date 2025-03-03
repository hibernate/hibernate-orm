/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import org.hibernate.query.spi.SelectQueryPlan;

/**
 * Union of {@link SelectQueryPlan} and {@link NativeQueryPlan} as the
 * {@link SelectQueryPlan} for native-queries.
 *
 * @author Steve Ebersole
 */
public interface NativeSelectQueryPlan<T> extends SelectQueryPlan<T>, NativeQueryPlan {
}
