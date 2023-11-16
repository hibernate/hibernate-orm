/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;

/**
 * Commonality between entity and any discriminators
 *
 * @author Steve Ebersole
 */
public interface DiscriminatorSqmPath<T> extends SqmPath<T> {
	@Override
	default void appendHqlString(StringBuilder sb) {
		sb.append( "type(" );
		getLhs().appendHqlString( sb );
		sb.append( ')' );
	}

	@Override
	default SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(Class treatJavaType) throws PathException {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(EntityDomainType treatTarget) throws PathException {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(Class treatJavaType, String alias) throws PathException {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(EntityDomainType treatTarget, String alias) throws PathException {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(Class treatJavaType, String alias, boolean fetch) throws PathException {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(EntityDomainType treatTarget, String alias, boolean fetch) throws PathException {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}
}
