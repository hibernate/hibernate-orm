/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.fk;

import org.hibernate.action.queue.constraint.ForeignKey;

import java.io.Serializable;
import java.util.List;

/// Access to all the foreign-keys defined in the domain model.
///
/// @param foreignKeys All the foreign-keys.
///
/// @author Steve Ebersole
public record ForeignKeyModel(List<ForeignKey> foreignKeys) implements Serializable {
}
