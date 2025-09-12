/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import java.util.Locale;

import jakarta.persistence.metamodel.Bindable;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.tree.SqmExpressibleAccessor;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * Represents parts of the application's domain model that can be used
 * to create {@link SqmPath} nodes.
 *
 * @apiNote Parallel to JPA's {@link jakarta.persistence.metamodel.Bindable} but
 * broader mainly to support Hibernate ANY-mappings
 *
 * @author Steve Ebersole
 */
public interface SqmPathSource<J> extends SqmExpressible<J>, Bindable<J>, SqmExpressibleAccessor<J> {
	/**
	 * The name of this thing.  Mainly used in logging and when creating a
	 * {@link NavigablePath}
	 */
	String getPathName();

	/**
	 * The type of SqmPaths this source creates.  Corollary to JPA's
	 * {@link Bindable#getBindableJavaType()}
	 */
	DomainType<?> getSqmPathType();

	/**
	 * Find a SqmPathSource by name relative to this source.
	 *
	 * returns null if the subPathSource is not found
	 *
	 * @throws IllegalStateException to indicate that this source cannot be de-referenced
	 */
	SqmPathSource<?> findSubPathSource(String name);

	/**
	 * Find a SqmPathSource by name relative to this source.
	 *
	 * @throws IllegalStateException to indicate that this source cannot be de-referenced
	 * @throws IllegalArgumentException if the subPathSource is not found
	 */
	default SqmPathSource<?> getSubPathSource(String name) {
		final SqmPathSource<?> subPathSource = findSubPathSource( name );
		if ( subPathSource == null ) {
			throw new IllegalArgumentException(
					new SemanticException(
							String.format(
									Locale.ROOT,
									"Could not resolve attribute '%s' of '%s'",
									name,
									getExpressible().getExpressibleJavaType().getJavaType().getTypeName()
							)
					)
			);
		}
		return subPathSource;
	}

	/**
	 * Returns the intermediate SqmPathSource for a path source previously acquired via {@link #findSubPathSource(String)}.
	 */
	default SqmPathSource<?> getIntermediatePathSource(SqmPathSource<?> pathSource) {
		return null;
	}

	/**
	 * Create an SQM path for this source relative to the given left-hand side
	 */
	SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource);

	@Override
	default SqmExpressible<J> getExpressible() {
		return (SqmExpressible<J>) getSqmPathType();
	}

	@Override
	default DomainType<J> getSqmType() {
		return (DomainType<J>) getSqmPathType();
	}

	/**
	 * Indicates if this path source is generically typed
	 */
	default boolean isGeneric() {
		return false;
	}
}
