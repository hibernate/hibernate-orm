/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.Serializable;


/**
 * Identifies a named association belonging to a particular
 * entity instance. Used to record the fact that an association
 * is null during loading.
 *
 * @param ownerKey The EntityKey of the association owner
 * @param propertyName The name of the property on the owner which defines the association
 *
 * @author Gavin King
 */
public record AssociationKey(EntityKey ownerKey, String propertyName)
		implements Serializable {
}
