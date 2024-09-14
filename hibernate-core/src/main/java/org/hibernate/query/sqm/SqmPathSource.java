/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm;

import java.util.Locale;

import jakarta.persistence.metamodel.Bindable;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.tree.SqmExpressibleAccessor;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * Represents any part of the domain model which can be used to create a
 * {@link SqmPath} node.
 *
 * @apiNote Parallel to the JPA-defined interface
 *          {@link jakarta.persistence.metamodel.Bindable}
 *          but broader mainly to support {@code @Any} mappings
 *
 * @author Steve Ebersole
 */
public interface SqmPathSource<J> extends SqmExpressible<J>, Bindable<J>, SqmExpressibleAccessor<J> {
	/**
	 * The name of this thing.
	 *
	 * @apiNote Mainly used in logging and when creating a {@link NavigablePath}.
	 */
	String getPathName();

	/**
	 * The type of {@linkplain SqmPath path} this source creates.
	 *
	 * @apiNote Analogous to {@link Bindable#getBindableJavaType()}.
	 */
	DomainType<J> getSqmPathType();

	/**
	 * Find a {@link SqmPathSource} by name relative to this source.
	 *
	 * @return null if the subPathSource is not found
	 *
	 * @throws IllegalStateException to indicate that this source cannot be de-referenced
	 */
	SqmPathSource<?> findSubPathSource(String name);

	/**
	 * Find a {@link SqmPathSource} by name relative to this source.
	 *
	 * @return null if the subPathSource is not found
	 *
	 * @throws IllegalStateException to indicate that this source cannot be de-referenced
	 */
	default SqmPathSource<?> findSubPathSource(String name, JpaMetamodel metamodel) {
		return findSubPathSource( name );
	}

	/**
	 * Find a {@link SqmPathSource} by name relative to this source.
	 *
	 * @throws IllegalStateException to indicate that this source cannot be de-referenced
	 * @throws IllegalArgumentException if the subPathSource is not found
	 */
	default SqmPathSource<?> getSubPathSource(String name) {
		final SqmPathSource<?> subPathSource = findSubPathSource( name );
		if ( subPathSource == null ) {
			throw new PathElementException(
					String.format(
							Locale.ROOT,
							"Could not resolve attribute '%s' of '%s'",
							name,
							getExpressible().getTypeName()
					)
			);
		}
		return subPathSource;
	}

	/**
	 * Find a {@link SqmPathSource} by name relative to this source.
	 *
	 * @throws IllegalStateException to indicate that this source cannot be de-referenced
	 * @throws IllegalArgumentException if the subPathSource is not found
	 */
	default SqmPathSource<?> getSubPathSource(String name, JpaMetamodel metamodel) {
		final SqmPathSource<?> subPathSource = findSubPathSource( name, metamodel );
		if ( subPathSource == null ) {
			throw new PathElementException(
					String.format(
							Locale.ROOT,
							"Could not resolve attribute '%s' of '%s'",
							name,
							getExpressible().getTypeName()
					)
			);
		}
		return subPathSource;
	}

	/**
	 * Returns the intermediate {@link SqmPathSource} for a path source
	 * previously acquired via {@link #findSubPathSource(String)}.
	 */
	default SqmPathSource<?> getIntermediatePathSource(SqmPathSource<?> pathSource) {
		return null;
	}

	/**
	 * Create an SQM path for this source relative to the given left hand side
	 */
	SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource);

	@Override
	default SqmExpressible<J> getExpressible() {
		return getSqmPathType();
	}

	@Override
	default DomainType<J> getSqmType() {
		return getSqmPathType();
	}

	/**
	 * Indicates if this path source is generically typed
	 */
	default boolean isGeneric() {
		return false;
	}
}
