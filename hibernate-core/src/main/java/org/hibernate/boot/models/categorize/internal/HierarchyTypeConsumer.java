/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;

/**
 * Consumer of types as we walk the managed-type hierarchy
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface HierarchyTypeConsumer {
	void acceptType(IdentifiableTypeMetadata type);
}
