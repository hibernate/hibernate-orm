/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.function.Supplier;

import org.hibernate.query.hql.DotIdentifierConsumer;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.SqmPathRegistry;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import org.jboss.logging.Logger;

/**
 * @asciidoc
 *
 * DotIdentifierHandler used to interpret paths outside of any specific
 * context.  This is the handler used at the root of the handler stack.
 *
 * It can recognize any number of types of paths -
 *
 * 		* fully-qualified class names (entity or otherwise)
 * 		* static field references, e.g. `MyClass.SOME_FIELD`
 * 		* enum value references, e.g. `Sex.MALE`
 * 		* navigable-path
 * 		* others?
 *
 * @author Steve Ebersole
 */
public class BasicDotIdentifierConsumer implements DotIdentifierConsumer {
	private static final Logger log = Logger.getLogger( BasicDotIdentifierConsumer.class );

	private final Supplier<SqmCreationProcessingState> processingStateSupplier;

	private String pathSoFar;
	private SemanticPathPart currentPart;

	public BasicDotIdentifierConsumer(Supplier<SqmCreationProcessingState> processingStateSupplier) {
		this.processingStateSupplier = processingStateSupplier;
	}

	@Override
	public SemanticPathPart getConsumedPart() {
		return currentPart;
	}

	@Override
	public void consumeIdentifier(String identifier, boolean isBase, boolean isTerminal) {
		if ( isBase ) {
			// each time we start a new sequence we need to reset our state
			reset();
		}

		if ( pathSoFar == null ) {
			pathSoFar = identifier;
		}
		else {
			pathSoFar += ( '.' + identifier );
		}

		log.tracef(
				"BasicDotIdentifierHandler#consumeIdentifier( %s, %s, %s ) - %s",
				identifier,
				isBase,
				isTerminal,
				pathSoFar
		);

		currentPart = currentPart.resolvePathPart(
				identifier,
				pathSoFar,
				isTerminal,
				processingStateSupplier.get().getCreationState()
		);
	}

	private void reset() {
		pathSoFar = null;
		currentPart = new BaseLocalSequencePart();
	}

	public class BaseLocalSequencePart implements SemanticPathPart {
		private boolean isBase = true;

		@Override
		public SemanticPathPart resolvePathPart(
				String identifier,
				String currentContextKey,
				boolean isTerminal,
				SqmCreationState creationState) {
			if ( isBase ) {
				isBase = false;

				final SqmPathRegistry sqmPathRegistry = creationState.getProcessingStateStack()
						.getCurrent()
						.getPathRegistry();

				final SqmFrom pathRootByAlias = sqmPathRegistry.findFromByAlias( identifier );
				if ( pathRootByAlias != null ) {
					// identifier is an alias (identification variable)
					return pathRootByAlias;
				}

				final SqmFrom pathRootByExposedNavigable = sqmPathRegistry.findFromExposing( identifier );
				if ( pathRootByExposedNavigable != null ) {
					// identifier is an "unqualified attribute reference"
					return pathRootByExposedNavigable;
				}
			}

			// at the moment, below this point we wait to resolve the sequence until we hit the terminal
			//
			// we could check for "intermediate resolution", but that comes with a performance hit.  E.g., consider
			//
			//		`org.hibernate.test.Sex.MALE`
			//
			// we could check `org` and then `org.hibernate` and then `org.hibernate.test` and then ... until
			// we know it is a package, class or entity name.  That gets expensive though.  For now, plan on
			// resolving these at the terminal
			//
			// todo (6.0) : finish this logic.  and see above note in `! isTerminal` block

			if ( !isTerminal ) {
				return this;
			}

			throw new UnsupportedOperationException( "Not yet implemented" );
		}

		@Override
		public SqmRestrictedCollectionElementReference resolveIndexedAccess(
				SqmExpression selector,
				String currentContextKey,
				boolean isTerminal,
				SqmCreationState processingState) {
			throw new UnsupportedOperationException();
		}
	}
}
