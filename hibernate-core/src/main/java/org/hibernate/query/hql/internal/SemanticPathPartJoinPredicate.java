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
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SemanticPathPartJoinPredicate implements SemanticPathPart {
	private final SqmFrom joinLhs;

	@SuppressWarnings("WeakerAccess")
	public SemanticPathPartJoinPredicate(SqmFrom joinLhs) {
		super();
		this.joinLhs = joinLhs;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmCreationProcessingState processingState = creationState.getCurrentProcessingState();
		final SqmPathRegistry pathRegistry = processingState.getPathRegistry();

		// #1 - name is joinLhs alias
		if ( name.equals( joinLhs.getExplicitAlias() ) ) {
			return joinLhs;
		}

		// #2 - name is alias for another SqmFrom
		final SqmFrom fromByAlias = pathRegistry.findFromByAlias( name );
		if ( fromByAlias != null ) {
			validatePathRoot( fromByAlias );
			return fromByAlias;
		}

		// #3 - name is a unqualified attribute reference relative to the current processing state
		final SqmFrom fromExposing = pathRegistry.findFromExposing( name );
		if ( fromExposing != null ) {
			validatePathRoot( fromExposing );
			return fromExposing;
		}

		if ( ! isTerminal ) {
			return new SemanticPathPartDelayedResolution( name );
		}

		throw new SemanticException( "Could not resolve path root used in join predicate: " + name );
	}

	private void validatePathRoot(SqmPath path) {
		if ( ! path.findRoot().equals( joinLhs.findRoot() ) ) {
			throw new SemanticException(
					String.format(
							Locale.ROOT,
							"Qualified join predicate path [%s] referred to from-clause root other that the join rhs",
							path.getNavigablePath().getFullPath()
					)
			);
		}
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException( "Illegal index-access as join predicate root" );
	}
}
