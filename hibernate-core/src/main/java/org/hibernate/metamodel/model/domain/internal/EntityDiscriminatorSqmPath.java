/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.sqm.DiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.AbstractSqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


/**
 * {@link SqmPath} specialization for an entity discriminator
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EntityDiscriminatorSqmPath<T> extends AbstractSqmPath<T> implements DiscriminatorSqmPath<T> {
	private final SqmEntityDomainType entityDomainType;
	private final EntityMappingType entityDescriptor;

	protected EntityDiscriminatorSqmPath(
			NavigablePath navigablePath,
			SqmPathSource referencedPathSource,
			SqmPath<?> lhs,
			SqmEntityDomainType entityDomainType,
			EntityMappingType entityDescriptor,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
		this.entityDomainType = entityDomainType;
		this.entityDescriptor = entityDescriptor;
	}

	public EntityDomainType getEntityDomainType() {
		return entityDomainType;
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public @NonNull SqmPath<?> getLhs() {
		return castNonNull( super.getLhs() );
	}

	@Override
	public @NonNull EntityDiscriminatorSqmPathSource getExpressible() {
//		return (EntityDiscriminatorSqmPathSource) getNodeType();
		return (EntityDiscriminatorSqmPathSource) getReferencedPathSource();
	}

	@Override
	public EntityDiscriminatorSqmPath copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		else {
			return context.registerCopy(
					this,
					(EntityDiscriminatorSqmPath) getLhs().copy( context ).type()
			);
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return entityDescriptor.hasSubclasses()
				? walker.visitDiscriminatorPath( this )
				: walker.visitEntityTypeLiteralExpression(
						new SqmLiteralEntityType( entityDomainType, nodeBuilder() ) );
	}
}
