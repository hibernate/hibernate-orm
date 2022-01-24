/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;
import org.hibernate.type.Type;

/**
 * Provides the information needed to properly handle type discrimination
 * in HQL queries, either by 'something.class' or 'type(something)' references.
 *
 * @author Steve Ebersole
 */
public interface DiscriminatorMetadata {

	/**
	 * Get the type used to resolve the actual discriminator value.
	 *
	 * @return The resolution type.
	 */
	Type getResolutionType();
}
