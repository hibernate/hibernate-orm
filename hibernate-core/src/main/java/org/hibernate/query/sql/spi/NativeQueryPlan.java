/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.spi.QueryPlan;

/**
 * Specialization of {@link QueryPlan} for {@link NativeQuery}
 * plans
 *
 * @author Steve Ebersole
 */
public interface NativeQueryPlan extends QueryPlan {
}
