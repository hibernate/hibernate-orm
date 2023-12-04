/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
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
