/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import jakarta.persistence.metamodel.Bindable;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.tree.SqmExpressableAccessor;
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
public interface SqmPathSource<J> extends SqmExpressable<J>, Bindable<J>, SqmExpressableAccessor<J> {
	/**
	 * The name of this thing.  Mainly used in logging and when creating a
	 * {@link org.hibernate.query.NavigablePath}
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
	 * @throws IllegalStateException to indicate that this source cannot be de-referenced
	 */
	SqmPathSource<?> findSubPathSource(String name);

	/**
	 * Create an SQM path for this source relative to the given left-hand side
	 */
	SqmPath<J> createSqmPath(SqmPath<?> lhs);

	@Override
	default SqmExpressable<J> getExpressable() {
		return (SqmExpressable<J>) getSqmPathType();
	}
}
