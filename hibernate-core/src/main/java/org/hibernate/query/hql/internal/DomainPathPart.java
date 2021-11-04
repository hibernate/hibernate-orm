/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.HqlLogging;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * Specialized "intermediate" SemanticPathPart for processing domain model paths
 *
 * @author Steve Ebersole
 */
public class DomainPathPart implements SemanticPathPart {
	private SqmPath<?> currentPath;

	@SuppressWarnings("WeakerAccess")
	public DomainPathPart(SqmPath<?> basePath) {
		this.currentPath = basePath;
		assert currentPath != null;
	}

	SqmExpression<?> getSqmExpression() {
		return currentPath;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		HqlLogging.QUERY_LOGGER.tracef(
				"Resolving DomainPathPart(%s) sub-part : %s",
				currentPath,
				name
		);
		final SqmPath<?> reusablePath = currentPath.getReusablePath( name );
		if ( reusablePath != null ) {
			currentPath = reusablePath;
		}
		else {
			// Try to resolve an existing attribute join without ON clause
			SqmPath<?> resolvedPath = null;
			if ( currentPath instanceof SqmFrom<?, ?> ) {
				ModelPartContainer modelPartContainer = null;
				for ( SqmJoin<?, ?> sqmJoin : ( (SqmFrom<?, ?>) currentPath ).getSqmJoins() ) {
					if ( sqmJoin instanceof SqmAttributeJoin<?, ?>
							&& name.equals( sqmJoin.getReferencedPathSource().getPathName() ) ) {
						final SqmAttributeJoin<?, ?> attributeJoin = (SqmAttributeJoin<?, ?>) sqmJoin;
						if ( attributeJoin.getOn() == null ) {
							// todo (6.0): to match the expectation of the JPA spec I think we also have to check
							//  that the join type is INNER or the default join type for the attribute,
							//  but as far as I understand, in 5.x we expect to ignore this behavior
//							if ( attributeJoin.getSqmJoinType() != SqmJoinType.INNER ) {
//								if ( attributeJoin.getAttribute().isCollection() ) {
//									continue;
//								}
//								if ( modelPartContainer == null ) {
//									modelPartContainer = findModelPartContainer( attributeJoin, creationState );
//								}
//								final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) modelPartContainer.findSubPart(
//										name,
//										null
//								);
//								if ( attributeJoin.getSqmJoinType().getCorrespondingSqlJoinType() != joinProducer.getDefaultSqlAstJoinType( null ) ) {
//									continue;
//								}
//							}
							resolvedPath = sqmJoin;
							if ( attributeJoin.isFetched() ) {
								break;
							}
						}
					}
				}
			}
			if ( resolvedPath == null ) {
				currentPath = currentPath.get( name );
			}
			else {
				currentPath = resolvedPath;
			}
		}
		if ( isTerminal ) {
			return currentPath;
		}
		else {
			return this;
		}
	}

	private ModelPartContainer findModelPartContainer(SqmAttributeJoin<?, ?> attributeJoin, SqmCreationState creationState) {
		final SqmFrom<?, ?> lhs = attributeJoin.getLhs();
		if ( lhs instanceof SqmAttributeJoin<?, ?> ) {
			final SqmAttributeJoin<?, ?> lhsAttributeJoin = (SqmAttributeJoin<?, ?>) lhs;
			if ( lhsAttributeJoin.getReferencedPathSource() instanceof EntityDomainType<?> ) {
				final String entityName = ( (EntityDomainType<?>) lhsAttributeJoin.getReferencedPathSource() ).getHibernateEntityName();
				return (ModelPartContainer) creationState.getCreationContext().getQueryEngine()
						.getTypeConfiguration()
						.getSessionFactory()
						.getMetamodel()
						.entityPersister( entityName )
						.findSubPart( attributeJoin.getAttribute().getName(), null );
			}
			else {
				return (ModelPartContainer) findModelPartContainer( lhsAttributeJoin, creationState )
						.findSubPart( attributeJoin.getAttribute().getName(), null );
			}
		}
		else {
			final String entityName;
			if ( lhs instanceof SqmRoot<?> ) {
				entityName = ( (SqmRoot<?>) lhs ).getEntityName();
			}
			else if ( lhs instanceof SqmEntityJoin<?> ) {
				entityName = ( (SqmEntityJoin<?>) lhs ).getEntityName();
			}
			else {
				assert lhs instanceof SqmCrossJoin<?>;
				entityName = ( (SqmCrossJoin<?>) lhs ).getEntityName();
			}
			return (ModelPartContainer) creationState.getCreationContext().getQueryEngine()
					.getTypeConfiguration()
					.getSessionFactory()
					.getMetamodel()
					.entityPersister( entityName )
					.findSubPart( attributeJoin.getAttribute().getName(), null );
		}
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
