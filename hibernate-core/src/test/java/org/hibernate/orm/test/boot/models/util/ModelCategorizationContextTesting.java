/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.util;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class ModelCategorizationContextTesting implements ModelCategorizationContext {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;
	private final SharedCacheMode sharedCacheMode;

	public ModelCategorizationContextTesting(SourceModelBuildingContext sourceModelBuildingContext) {
		this( sourceModelBuildingContext, SharedCacheMode.UNSPECIFIED );
	}

	public ModelCategorizationContextTesting(
			SourceModelBuildingContext sourceModelBuildingContext,
			SharedCacheMode sharedCacheMode) {
		this(
				sourceModelBuildingContext.getClassDetailsRegistry(),
				sourceModelBuildingContext.getAnnotationDescriptorRegistry(),
				sharedCacheMode
		);
	}

	public ModelCategorizationContextTesting(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry) {
		this( classDetailsRegistry, annotationDescriptorRegistry, SharedCacheMode.UNSPECIFIED );
	}

	public ModelCategorizationContextTesting(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			SharedCacheMode sharedCacheMode) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
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

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public List<JpaEventListener> getDefaultEventListeners() {
		return Collections.emptyList();
	}
}
