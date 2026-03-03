/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public
interface StaleStateMapper {
	HibernateException map(StaleStateException staleStateException);
}
