/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal.lock;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;

/**
 * @author Steve Ebersole
 */
record EntityDetails(EntityKey key, EntityEntry entry, Object instance) {
}
