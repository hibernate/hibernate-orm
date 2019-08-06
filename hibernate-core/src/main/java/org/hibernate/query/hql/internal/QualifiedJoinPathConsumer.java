/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import org.jboss.logging.Logger;

/**
 * Specialized "intermediate" SemanticPathPart for processing domain model paths
 *
 * @author Steve Ebersole
 */
public class QualifiedJoinPathConsumer implements DotIdentifierConsumer {
	private static final Logger log = Logger.getLogger( QualifiedJoinPathConsumer.class );

	private final SqmCreationState creationState;
	private final SqmRoot sqmRoot;

	private final SqmJoinType joinType;
	private final boolean fetch;
	private final String alias;

	private ConsumerDelegate delegate;

	public QualifiedJoinPathConsumer(
			SqmRoot<?> sqmRoot,
			SqmJoinType joinType,
			boolean fetch,
			String alias,
			SqmCreationState creationState) {
		this.sqmRoot = sqmRoot;
		this.joinType = joinType;
		this.fetch = fetch;
		this.alias = alias;
		this.creationState = creationState;
	}

	@Override
	public SemanticPathPart getConsumedPart() {
		return delegate.getConsumedPart();
	}

	@Override
	public void consumeIdentifier(String identifier, boolean isBase, boolean isTerminal) {
		if ( isBase ) {
			assert delegate == null;
			delegate = resolveBase( identifier, isTerminal );
		}
		else {
			assert delegate != null;
			delegate.consumeIdentifier( identifier, isTerminal );
		}
	}

	private ConsumerDelegate resolveBase(String identifier, boolean isTerminal) {
		final SqmCreationProcessingState processingState = creationState.getCurrentProcessingState();
		final SqmPathRegistry pathRegistry = processingState.getPathRegistry();

		final SqmFrom pathRootByAlias = pathRegistry.findFromByAlias( identifier );
		if ( pathRootByAlias != null ) {
			// identifier is an alias (identification variable)

			if ( isTerminal ) {
				throw new SemanticException( "Cannot join to root : " + identifier );
			}

			return new AttributeJoinDelegate(
					pathRootByAlias,
					joinType,
					fetch,
					alias,
					creationState
			);
		}

		final SqmFrom pathRootByExposedNavigable = pathRegistry.findFromExposing( identifier );
		if ( pathRootByExposedNavigable != null ) {
			return new AttributeJoinDelegate(
					createJoin( pathRootByExposedNavigable, identifier, isTerminal ),
					joinType,
					fetch,
					alias,
					creationState
			);
		}

		// otherwise, assume we have a qualified entity name - delay resolution until we
		// process the final token

		return new ExpectingEntityJoinDelegate(
				identifier,
				isTerminal,
				sqmRoot,
				joinType,
				alias,
				fetch,
				creationState
		);
	}

	private SqmFrom createJoin(SqmFrom lhs, String identifier, boolean isTerminal) {
		return createJoin(
				lhs,
				identifier,
				joinType,
				alias,
				fetch,
				isTerminal,
				creationState
		);
	}

	private static SqmFrom createJoin(
			SqmFrom lhs,
			String name,
			SqmJoinType joinType,
			String alias,
			boolean fetch,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPathSource subPathSource = lhs.getReferencedPathSource().findSubPathSource( name );
		final SqmAttributeJoin join = ( (SqmJoinable) subPathSource ).createSqmJoin(
				lhs,
				joinType,
				isTerminal ? alias : null,
				fetch,
				creationState
		);
		//noinspection unchecked
		lhs.addSqmJoin( join );
		creationState.getCurrentProcessingState().getPathRegistry().register( join );
		return join;
	}

	private interface ConsumerDelegate {
		void consumeIdentifier(String identifier, boolean isTerminal);
		SemanticPathPart getConsumedPart();
	}

	private static class AttributeJoinDelegate implements ConsumerDelegate {
		private final SqmCreationState creationState;

		private final SqmJoinType joinType;
		private final boolean fetch;
		private final String alias;

		private SqmFrom currentPath;

		public AttributeJoinDelegate(
				SqmFrom base,
				SqmJoinType joinType,
				boolean fetch,
				String alias,
				SqmCreationState creationState) {
			this.joinType = joinType;
			this.fetch = fetch;
			this.alias = alias;
			this.creationState = creationState;

			this.currentPath = base;
		}

		@Override
		public void consumeIdentifier(String identifier, boolean isTerminal) {
			currentPath = createJoin(
					currentPath,
					identifier,
					joinType,
					alias,
					fetch,
					isTerminal,
					creationState
			);
		}

		@Override
		public SemanticPathPart getConsumedPart() {
			return currentPath;
		}
	}

	private static class ExpectingEntityJoinDelegate implements ConsumerDelegate {
		private final SqmCreationState creationState;
		private final SqmRoot sqmRoot;

		private final SqmJoinType joinType;
		private final boolean fetch;
		private final String alias;

		private NavigablePath path = new NavigablePath();

		private SqmEntityJoin<?> join;

		public ExpectingEntityJoinDelegate(
				String identifier,
				boolean isTerminal,
				SqmRoot sqmRoot,
				SqmJoinType joinType,
				String alias,
				boolean fetch,
				SqmCreationState creationState) {
			this.creationState = creationState;
			this.sqmRoot = sqmRoot;
			this.joinType = joinType;
			this.fetch = fetch;
			this.alias = alias;

			consumeIdentifier( identifier, isTerminal );
		}

		@Override
		public void consumeIdentifier(String identifier, boolean isTerminal) {
			path = path.append( identifier );

			if ( isTerminal ) {
				final EntityDomainType<?> joinedEntityType = creationState.getCreationContext()
						.getJpaMetamodel()
						.resolveHqlEntityReference( path.getFullPath() );
				if ( joinedEntityType == null ) {
					throw new SemanticException( "Could not resolve join path - " + path.getFullPath() );
				}

				assert ! ( joinedEntityType instanceof SqmPolymorphicRootDescriptor );

				if ( fetch ) {
					log.debugf( "Ignoring fetch on entity join : %s(%s)", joinedEntityType.getHibernateEntityName(), alias );
				}

				join = new SqmEntityJoin<>( joinedEntityType, alias, joinType, sqmRoot );
				creationState.getCurrentProcessingState().getPathRegistry().register( join );
			}
		}

		@Override
		public SemanticPathPart getConsumedPart() {
			return join;
		}
	}
}
