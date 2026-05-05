/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public
interface StaleStateMapper {
	@Nullable
	HibernateException map(StaleStateException staleStateException);
}
