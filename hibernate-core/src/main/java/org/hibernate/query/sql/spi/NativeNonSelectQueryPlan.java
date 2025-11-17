/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import org.hibernate.query.spi.NonSelectQueryPlan;

/**
 * Union of {@link NonSelectQueryPlan} and {@link NativeQueryPlan} as the
 * {@link NonSelectQueryPlan} for native-queries.
 *
 * @author Steve Ebersole
 */
public interface NativeNonSelectQueryPlan extends NativeQueryPlan, NonSelectQueryPlan {
}
