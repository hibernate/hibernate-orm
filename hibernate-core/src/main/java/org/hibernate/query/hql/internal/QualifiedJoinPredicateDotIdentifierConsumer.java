/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;

/**
 * @author Steve Ebersole
 */
public class QualifiedJoinPredicateDotIdentifierConsumer extends BasicDotIdentifierConsumer {
	private final SqmQualifiedJoin joinRhs;
	private final String predicateText;

	public QualifiedJoinPredicateDotIdentifierConsumer(
			Supplier<SqmCreationProcessingState> processingStateSupplier,
			SqmQualifiedJoin joinRhs,
			String predicateText) {
		super( processingStateSupplier );
		this.joinRhs = joinRhs;
		this.predicateText = predicateText;
	}

	@Override
	protected SemanticPathPart createBasePart() {
		return new BaseLocalSequencePart() {
			@Override
			protected void validateAsRoot(SqmFrom pathRoot) {
				if ( pathRoot.findRoot() != joinRhs.findRoot() ) {
					throw new SemanticException(
							String.format(
									Locale.ROOT,
									"SqmQualifiedJoin predicate referred to SqmRoot [`%s`] other than the join's root [`%s`] - `%s`",
									pathRoot.getNavigablePath().getFullPath(),
									joinRhs.getNavigablePath().getFullPath(),
									predicateText
							)
					);
				}

				super.validateAsRoot( pathRoot );
			}
		};
	}
}
