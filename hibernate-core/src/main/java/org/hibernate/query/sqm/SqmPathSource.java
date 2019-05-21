/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * Represents parts of the application's domain model that can be used
 * to create {@link SqmPath} nodes.
 *
 * @apiNote Parallel to JPA's {@link javax.persistence.metamodel.Bindable} but
 * broader mainly to support Hibernate ANY-mappings
 *
 * @author Steve Ebersole
 */
public interface SqmPathSource<J,B> extends SqmExpressable<J>, SemanticPathPart {
	/**
	 * The name of this thing.  Mainly used in logging and when creating a
	 * {@link org.hibernate.query.NavigablePath}
	 */
	String getPathName();

	/**
	 * The type of SqmPaths this source creates
	 */
	DomainType<B> getSqmNodeType();

	SqmPathSource<?,?> findSubPathSource(String name);

	/**
	 * Create an SQM path for this source relative to the given left-hand side
	 */
	SqmPath<B> createSqmPath(SqmPath<?> lhs, SqmCreationState creationState);
}
