/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import java.util.Locale;

import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;

/**
 * @author Steve Ebersole
 */
public class SemanticPathPartJoinPredicate extends SemanticPathPartRoot {
	private final SqmFromElementSpace fromElementSpace;

	public SemanticPathPartJoinPredicate(SqmFromElementSpace fromElementSpace) {
		this.fromElementSpace = fromElementSpace;
	}

	@Override
	protected void validateNavigablePathRoot(
			SqmNavigableReference navigableReference,
			String currentContextKey,
			SqmCreationContext context) {
		final SqmFrom sqmFrom = navigableReference.getExportedFromElement();
		if ( sqmFrom == null ) {
			throw new ParsingException(
					String.format(
							Locale.ROOT,
							"SqmNavigableReference [%s] used as root for join-predicate path [%s] did not link to SqmFrom",
							navigableReference.getNavigablePath().getFullPath(),
							currentContextKey
					)
			);
		}

		if ( ! sqmFrom.getContainingSpace().equals( fromElementSpace ) ) {
			throw new SemanticException(
					"Qualified join predicate referred to FromElement [" +
							navigableReference.getNavigablePath().getFullPath() +
							"] outside the FromElementSpace containing the join"
			);
		}
	}
}
