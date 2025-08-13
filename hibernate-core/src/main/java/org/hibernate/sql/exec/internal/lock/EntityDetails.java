/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;

/**
 * Models details about an entity instance used while performing follow-on locking
 *
 * @author Steve Ebersole
 */
public record EntityDetails(EntityKey key, EntityEntry entry, Object instance) {
}
