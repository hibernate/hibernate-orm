/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

@Incubating(since = "6.2")
public interface SqmPersistentAttribute<D,J> extends PersistentAttribute<D,J> {
}
