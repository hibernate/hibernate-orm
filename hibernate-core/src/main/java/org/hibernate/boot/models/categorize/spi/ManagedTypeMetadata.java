/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.Collection;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.models.spi.ClassDetails;


/**
 * Metadata about a {@linkplain jakarta.persistence.metamodel.ManagedType managed type}
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeMetadata {

	enum Kind { ENTITY, MAPPED_SUPER, EMBEDDABLE }

	Kind getManagedTypeKind();

	/**
	 * The underlying managed-class
	 */
	ClassDetails getClassDetails();

	/**
	 * The class-level access type
	 */
	ClassAttributeAccessType getClassLevelAccessType();

	/**
	 * Get the number of declared attributes
	 */
	int getNumberOfAttributes();

	/**
	 * Get the declared attributes
	 */
	Collection<AttributeMetadata> getAttributes();

	AttributeMetadata findAttribute(String name);

	/**
	 * Visit each declared attributes
	 */
	void forEachAttribute(IndexedConsumer<AttributeMetadata> consumer);
}
