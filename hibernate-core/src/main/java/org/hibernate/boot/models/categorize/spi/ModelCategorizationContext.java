/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.List;

import org.hibernate.boot.models.categorize.internal.StandardPersistentAttributeMemberResolver;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.SharedCacheMode;

/**
 * Contextual information used while building {@linkplain ManagedTypeMetadata} and friends.
 *
 * @author Steve Ebersole
 */
public interface ModelCategorizationContext {
	ClassDetailsRegistry getClassDetailsRegistry();

	AnnotationDescriptorRegistry getAnnotationDescriptorRegistry();

	SharedCacheMode getSharedCacheMode();

	default PersistentAttributeMemberResolver getPersistentAttributeMemberResolver() {
		return StandardPersistentAttributeMemberResolver.INSTANCE;
	}

	List<JpaEventListener> getDefaultEventListeners();
}
