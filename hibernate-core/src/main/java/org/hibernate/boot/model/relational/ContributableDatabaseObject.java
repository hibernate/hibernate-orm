/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.mapping.Contributable;

/**
 * Database objects (table, sequence, etc) which are associated with
 * a {@linkplain #getContributor() contributor} (ORM, Envers, etc) and
 * can be selectively exported per contributor
 */
public interface ContributableDatabaseObject extends Contributable, Exportable {
}
