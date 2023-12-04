/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.Map;
import java.util.Set;

import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;

/**
 * @author Steve Ebersole
 */
public class CategorizedDomainModelImpl implements CategorizedDomainModel {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;
	private final Set<EntityHierarchy> entityHierarchies;
	private final Map<String, ClassDetails> mappedSuperclasses;
	private final Map<String, ClassDetails> embeddables;
	private final GlobalRegistrations globalRegistrations;

	public CategorizedDomainModelImpl(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			Set<EntityHierarchy> entityHierarchies,
			Map<String, ClassDetails> mappedSuperclasses,
			Map<String, ClassDetails> embeddables,
			GlobalRegistrations globalRegistrations) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
		this.entityHierarchies = entityHierarchies;
		this.mappedSuperclasses = mappedSuperclasses;
		this.embeddables = embeddables;
		this.globalRegistrations = globalRegistrations;
	}

	@Override
	public ClassDetailsRegistry getClassDetailsRegistry() {
		return classDetailsRegistry;
	}

	@Override
	public AnnotationDescriptorRegistry getAnnotationDescriptorRegistry() {
		return annotationDescriptorRegistry;
	}

	@Override
	public Set<EntityHierarchy> getEntityHierarchies() {
		return entityHierarchies;
	}

	public Map<String, ClassDetails> getMappedSuperclasses() {
		return mappedSuperclasses;
	}

	@Override
	public Map<String, ClassDetails> getEmbeddables() {
		return embeddables;
	}

	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}
}
