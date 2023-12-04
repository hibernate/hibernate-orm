/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.mapping.DenormalizedTable;

/**
 * @author Steve Ebersole
 */
public record UnionTable(
		Identifier logicalName,
		TableReference base,
		DenormalizedTable table,
		boolean exportable) implements TableReference {
}
