/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce;

import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

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
			Navigable subNavigable,
			boolean isSubRefTerminal,
			SqmCreationState creationState) {
		if ( lhs == null ) {
			// this should mean that `processingPath` is an `SqmRoot` and really does not need resolution.
			//		- just skip it
			return;
		}

		final SqmCreationProcessingState processingState = creationState.getProcessingStateStack().getCurrent();

		final SqmFrom lhsFrom;
		if ( lhs instanceof SqmFrom ) {
			lhsFrom = (SqmFrom) lhs;
		}
		else {
			lhs.prepareForSubNavigableReference( processingPath.getReferencedNavigable(), false, creationState );

			// now we should be able to access the SqmFrom node for the `lhs`...
			lhsFrom = processingState.getPathRegistry().findFromByPath( lhs.getNavigablePath() );
		}

		if ( subNavigable instanceof EntityIdentifier && isSubRefTerminal ) {
			// do not create the join if the subNavigable reference is an entity identifier and is the path terminal
			// 		e.g., `select p.address.id from Person p ...`
			return;
		}

		// create the join if not already

		final SqmFrom existingJoin = processingState.getPathRegistry().findFromByPath( processingPath.getNavigablePath() );
		if ( existingJoin == null ) {
			final SqmNavigableJoin sqmJoin = new SqmNavigableJoin(
					processingPath.getNavigablePath().getFullPath(),
					lhsFrom,
					(Joinable) processingPath.getReferencedNavigable(),
					// a non-terminal should never have an alias
					null,
					SqmJoinType.INNER,
					false,
					creationState
			);
			lhsFrom.addJoin( sqmJoin );
			processingState.getPathRegistry().register( sqmJoin );
		}
	}
}
