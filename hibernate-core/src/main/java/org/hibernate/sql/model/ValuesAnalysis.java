/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model;

import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;

/**
 * Marker interface for analysis of new/old values.
 *
 * @see org.hibernate.engine.jdbc.mutation.MutationExecutor#execute
 * @see UpdateValuesAnalysis
 *
 * @author Steve Ebersole
 */
public interface ValuesAnalysis {
}
