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
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

/**
 * Specialized consumer for processing domain model paths occurring as part
 * of a join predicate
 *
 * @author Steve Ebersole
 */
public class QualifiedJoinPredicatePathConsumer extends BasicDotIdentifierConsumer {
	private final SqmQualifiedJoin<?, ?> sqmJoin;

	public QualifiedJoinPredicatePathConsumer(
			SqmQualifiedJoin<?, ?> sqmJoin,
			SqmCreationState creationState) {
		super( creationState );
		this.sqmJoin = sqmJoin;
	}

	@Override
	protected SemanticPathPart createBasePart() {
		return new BaseLocalSequencePart() {
			@Override
			protected void validateAsRoot(SqmFrom<?, ?> pathRoot) {
				final SqmRoot<?> root = pathRoot.findRoot();
				if ( root != sqmJoin.findRoot() ) {
					final SqmQuery<?> processingQuery = getCreationState().getCurrentProcessingState().getProcessingQuery();
					if ( processingQuery instanceof SqmSubQuery<?> ) {
						final SqmQuerySpec<?> querySpec = ( (SqmSubQuery<?>) processingQuery ).getQuerySpec();
						// If this "foreign" from element is used in a sub query
						// This is only an error if the from element is actually part of the sub query
						if ( querySpec.getFromClause() == null || !querySpec.getFromClause().getRoots().contains( root ) ) {
							super.validateAsRoot( pathRoot );
							return;
						}
					}
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
