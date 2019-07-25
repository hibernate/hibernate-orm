/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.Locale;

import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * Specialized consumer for processing domain model paths occurring as part
 * of a join predicate
 *
 * @author Steve Ebersole
 */
public class QualifiedJoinPredicatePathConsumer extends BasicDotIdentifierConsumer {
	private final SqmQualifiedJoin sqmJoin;

	public QualifiedJoinPredicatePathConsumer(
			SqmQualifiedJoin sqmJoin,
			SqmCreationState creationState) {
		super( creationState );
		this.sqmJoin = sqmJoin;
	}

	@Override
	protected SemanticPathPart createBasePart() {
		return new BaseLocalSequencePart() {
			@Override
			protected void validateAsRoot(SqmFrom pathRoot) {
				if ( pathRoot.findRoot() != sqmJoin.findRoot() ) {
					throw new SemanticException(
							String.format(
									Locale.ROOT,
									"SqmQualifiedJoin predicate referred to SqmRoot [`%s`] other than the join's root [`%s`]",
									pathRoot.getNavigablePath().getFullPath(),
									sqmJoin.getNavigablePath().getFullPath()
							)
					);
				}

				super.validateAsRoot( pathRoot );
			}
		};
	}
}
