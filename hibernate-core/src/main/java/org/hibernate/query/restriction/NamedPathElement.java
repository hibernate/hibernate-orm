package org.hibernate.query.restriction;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.JoinType;

/**
 * A non-root element of a {@link Path}, using a stringly-typed
 * attribute reference.
 *
 * @author Gavin King
 */
record NamedPathElement<X, U, V>(@Nonnull Path<? super X, U> parent, @Nonnull String attributeName, @Nonnull Class<V> attributeType)
		implements Path<X, V> {
	@Nonnull
	@Override
	public Class<V> getType() {
		return attributeType;
	}

	@Nonnull
	@Override
	public jakarta.persistence.criteria.Path<V> path(@Nonnull Root<? extends X> root) {
		final jakarta.persistence.criteria.Path<V> path = parent.path( root ).get( attributeName );
		if ( !attributeType.isAssignableFrom( path.getJavaType() ) ) {
			throw new IllegalArgumentException( "Attribute '" + attributeName
												+ "' is not of type '" + attributeType.getName() + "'" );
		}
		return path;
	}

	@Nonnull
	@Override
	public FetchParent<?, V> fetch(@Nonnull Root<? extends X> root) {
		return parent.fetch( root ).fetch( attributeName, JoinType.LEFT );
	}
}
