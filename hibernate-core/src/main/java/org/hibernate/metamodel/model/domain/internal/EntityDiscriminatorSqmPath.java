/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.sqm.spi.DiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.domain.AbstractSqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.spi.domain.SqmEntityDomainType;
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
	public @Nonnull SqmPath<?> getLhs() {
		return castNonNull( super.getLhs() );
	}

	@Override
	public @Nonnull EntityDiscriminatorSqmPathSource getExpressible() {
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
