/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce;

import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;

/**
 * @author Steve Ebersole
 */
public class SqmCreationHelper {
	public static NavigablePath buildRootNavigablePath(String base, String alias) {
		return alias == null
				? new NavigablePath( base )
				: new NavigablePath( base + '(' + alias + ')' );
	}

	public static NavigablePath buildSubNavigablePath(NavigablePath lhs, String base, String alias) {
		final String localPath = alias == null
				? base
				: base + '(' + alias + ')';
		return lhs.append( localPath );
	}

	private SqmCreationHelper() {
	}

	public static void resolveAsLhs(
			SqmPath lhs,
			SqmPath processingPath,
			SqmPath subReference,
			boolean isSubRefTerminal,
			SqmCreationState creationState) {
		assert lhs == null || processingPath.getLhs().getNavigablePath().equals( lhs.getNavigablePath() );

		final SqmCreationProcessingState processingState = creationState.getProcessingStateStack().getCurrent();

		SqmFrom lhsFrom = null;
		if ( lhs != null ) {
			lhs.prepareForSubNavigableReference( processingPath, false, creationState );
			lhsFrom = processingState.getPathRegistry().findFromByPath( lhs.getNavigablePath() );
		}

		if ( subReference.getReferencedNavigable() instanceof EntityIdentifier && isSubRefTerminal ) {
			return;
		}

		// create the join if not already

		final SqmFrom fromByPath = processingState.getPathRegistry().findFromByPath( processingPath.getNavigablePath() );
		if ( fromByPath == null ) {
			final SqmNavigableJoin entityFrom = new SqmNavigableJoin(
					"",
					lhsFrom,
					(Joinable) processingPath.getReferencedNavigable(),
					// a non-terminal should never have an alias
					null,
					SqmJoinType.INNER,
					false,
					creationState
			);
			if ( lhsFrom != null ) {
				lhsFrom.addJoin( entityFrom );
			}

			processingState.getPathRegistry().register( entityFrom );
		}
	}
}
