/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.mapping.Table;

/**
 * Models a from-clause sub-query.
 *
 * @see org.hibernate.annotations.Subselect
 *
 * @author Steve Ebersole
 */
public record InLineView(Identifier logicalName, Table table) implements TableReference {
	@Override
	public Identifier logicalName() {
		return logicalName;
	}

	public String getQuery() {
		return table.getSubselect();
	}

	@Override
	public boolean exportable() {
		return false;
	}
}
