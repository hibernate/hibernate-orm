/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.List;

import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class ModelCategorizationContextImpl implements ModelCategorizationContext {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;
	private final GlobalRegistrations globalRegistrations;
	private final SharedCacheMode sharedCacheMode;

	public ModelCategorizationContextImpl(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			GlobalRegistrations globalRegistrations) {
		this( classDetailsRegistry, annotationDescriptorRegistry, globalRegistrations, SharedCacheMode.UNSPECIFIED );
	}

	public ModelCategorizationContextImpl(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			GlobalRegistrations globalRegistrations,
			SharedCacheMode sharedCacheMode) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
		this.globalRegistrations = globalRegistrations;
		this.sharedCacheMode = sharedCacheMode;
	}

	@Override
	public ClassDetailsRegistry getClassDetailsRegistry() {
		return classDetailsRegistry;
	}

	@Override
	public AnnotationDescriptorRegistry getAnnotationDescriptorRegistry() {
		return annotationDescriptorRegistry;
	}

	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public List<JpaEventListener> getDefaultEventListeners() {
		return getGlobalRegistrations().getEntityListenerRegistrations();
	}
}
