/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.type.MetaType;
import org.hibernate.type.Type;

/**
 * Provides the information needed to properly handle type discrimination
 * in HQL queries, either by 'something.class' or 'type(something)' references.
 *
 * @deprecated The functionality of DiscriminatorMetadata, {@link DiscriminatorType} and {@link MetaType}  have been
 * consolidated into {@link EntityDiscriminatorMapping} and {@link DiscriminatorConverter}
 *
 * @author Steve Ebersole
 */
@Deprecated( since = "6.2", forRemoval = true )
public interface DiscriminatorMetadata {

	/**
	 * Get the type used to resolve the actual discriminator value.
	 *
	 * @return The resolution type.
	 */
	Type getResolutionType();
}
