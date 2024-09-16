/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.List;

import org.hibernate.boot.models.categorize.internal.StandardPersistentAttributeMemberResolver;
import org.hibernate.boot.models.spi.JpaEventListener;
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
