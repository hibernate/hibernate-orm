/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
