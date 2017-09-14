/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.internal;

import java.util.Locale;

import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.set.SqmSetClause;

/**
 * @author Steve Ebersole
 */
public class SqmUpdateStatementImpl extends AbstractSqmStatement implements SqmUpdateStatement {
	private final SqmRoot entityFromElement;
	private final SqmSetClause setClause = new SqmSetClause();
	private final SqmWhereClause whereClause = new SqmWhereClause();

	public SqmUpdateStatementImpl(SqmRoot entityFromElement) {
		this.entityFromElement = entityFromElement;
	}

	@Override
	public SqmRoot getEntityFromElement() {
		return entityFromElement;
	}

	@Override
	public SqmSetClause getSetClause() {
		return setClause;
	}

	@Override
	public SqmWhereClause getWhereClause() {
		return whereClause;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"update %s %s %s",
				entityFromElement,
				"[no set clause]",
				whereClause
		);
	}
}
