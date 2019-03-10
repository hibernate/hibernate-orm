/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.hql.DotIdentifierConsumer;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;

/**
 * @author Steve Ebersole
 */
public class QualifiedJoinPathIdentifierConsumer implements DotIdentifierConsumer {
	private final SqmJoinType joinType;
	private final boolean fetch;
	private final String alias;

	private final SqmCreationProcessingState processingState;
	private final SqmCreationContext sqmCreationContext;

	private String completePath = null;
	private SqmFrom current = null;

	public QualifiedJoinPathIdentifierConsumer(
			SqmJoinType joinType,
			boolean fetch,
			String alias,
			SqmCreationProcessingState processingState) {
		this.joinType = joinType;
		this.fetch = fetch;
		this.alias = alias;
		this.processingState = processingState;
		this.sqmCreationContext = processingState.getCreationState().getCreationContext();
	}

	@Override
	public SemanticPathPart getConsumedPart() {
		return current;
	}

	@Override
	public void consumeIdentifier(String identifier, boolean isBase, boolean isTerminal) {
		if ( completePath == null ) {
			completePath = identifier;
		}
		else {
			completePath += ( '.' + identifier );
		}

		if ( this.current == null ) {
			final SqmFrom pathRootByAlias = processingState.getPathRegistry().findFromByAlias( identifier );
			if ( pathRootByAlias != null ) {
				// identifier is an alias (identification variable)
				this.current = pathRootByAlias;
				return;
			}

			final SqmFrom pathRootByExposedNavigable = processingState.getPathRegistry().findFromExposing( identifier );
			if ( pathRootByExposedNavigable != null ) {
				// identifier is an "unqualified attribute reference".  Set `current` to the exposer,
				// but do not return - we still need to consume the identifier against the from-element
				// exposing
				current = pathRootByExposedNavigable;
				//
			}
			else {
				// the identifier could also signify an "entity join"... this could potentially need
				// to consume the entire sequence.  If we are processing the path terminus try
				// to resolve it as an entity-name
				if ( isTerminal ) {
					final EntityTypeDescriptor entityDescriptor = sqmCreationContext.getDomainModel()
							.findEntityDescriptor( completePath );
					if ( entityDescriptor != null ) {
						current = new SqmEntityJoin(
								processingState.getCreationState().generateUniqueIdentifier(),
								alias,
								entityDescriptor,
								joinType
						);
						return;
					}

//					throw new SemanticException( "Could not resolve domain path root - " + completePath );
				}
				else {
					// wait for the terminal
					return;
				}
			}
		}

		if ( current == null ) {
			throw new SemanticException( "Could not resolve qualified join path - " + identifier );
		}

		final Navigable navigable = this.current.getReferencedNavigable().findNavigable( identifier );
		if ( navigable == null ) {
			throw new SemanticException(
					String.format(
							Locale.ROOT,
							"Could not resolve path - %s -> %s",
							current.getNavigablePath().getFullPath(),
							identifier
					)
			);
		}

		if ( !( navigable instanceof NavigableContainer ) ) {
			throw new SemanticException(
					String.format(
							Locale.ROOT,
							"Path cannot be de-referenced - %s -> %s",
							current.getNavigablePath().getFullPath(),
							identifier
					)
			);
		}

		final SqmFrom lhs = this.current;
		final Navigable<Object> joinedNavigable = lhs.getReferencedNavigable().findNavigable( identifier );
		if ( joinedNavigable instanceof Joinable ) {
			this.current = ( (Joinable) joinedNavigable ).createJoin(
					lhs,
					joinType,
					isTerminal ? alias : null,
					fetch,
					processingState.getCreationState()
			);
			lhs.addJoin( (SqmJoin) current );
		}
		else {
			throw new SemanticException( "Joined path is not joinable: " + completePath );
		}
	}
}
