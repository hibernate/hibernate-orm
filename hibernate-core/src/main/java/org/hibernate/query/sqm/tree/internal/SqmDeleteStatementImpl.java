/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.internal;

import java.util.Locale;

import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;

/**
 * @author Steve Ebersole
 */
public class SqmDeleteStatementImpl extends AbstractSqmStatement implements SqmDeleteStatement {
	private final SqmRoot entityFromElement;
	private final SqmWhereClause whereClause = new SqmWhereClause();

	public SqmDeleteStatementImpl(SqmRoot entityFromElement) {
		this.entityFromElement = entityFromElement;
	}

	@Override
	public SqmRoot getEntityFromElement() {
		return entityFromElement;
	}

	public SqmWhereClause getWhereClause() {
		return whereClause;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"delete %s %s",
				entityFromElement,
				whereClause
		);
	}
}
