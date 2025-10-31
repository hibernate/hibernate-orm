/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;

/**
 * Commonality between entity and any discriminators
 *
 * @author Steve Ebersole
 */
public interface DiscriminatorSqmPath<T> extends SqmPath<T> {
	@Override
	default void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "type(" );
		getLhs().appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	default SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(Class treatJavaType) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(EntityDomainType treatTarget) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(Class treatJavaType, @Nullable String alias) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(EntityDomainType treatTarget, @Nullable String alias) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(Class treatJavaType, @Nullable String alias, boolean fetch) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(EntityDomainType treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}
}
