/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

/**
 * Metadata about a {@linkplain jakarta.persistence.metamodel.MappedSuperclassType mapped-superclass}
 *
 * @author Steve Ebersole
 */
public interface MappedSuperclassTypeMetadata extends IdentifiableTypeMetadata {
	@Override
	default Kind getManagedTypeKind() {
		return Kind.MAPPED_SUPER;
	}
}
