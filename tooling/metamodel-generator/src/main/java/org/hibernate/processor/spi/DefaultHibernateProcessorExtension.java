/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.spi;

import jakarta.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Default no-op implementation. Framework-specific behavior is provided
 * by implementations discovered via {@link java.util.ServiceLoader}.
 */
public class DefaultHibernateProcessorExtension implements HibernateProcessorExtension {

	@Override
	public void init(ProcessingEnvironment processingEnvironment) {
	}

	@Override
	public boolean isInjectionAvailable() {
		return false;
	}

	@Override
	public @Nullable String qualifierAnnotation() {
		return null;
	}

	@Override
	public boolean isExtensionEntity(TypeElement type) {
		return false;
	}

	@Override
	public boolean isExtensionRepository(TypeElement type) {
		return false;
	}

	@Override
	public void addRepositoryMembers(TypeElement element, AnnotationMetaEntityContext context) {
	}

	@Override
	public @Nullable SessionSetup setupRepositorySession(
			TypeElement element,
			@Nullable ExecutableElement getter,
			AnnotationMetaEntityContext context) {
		return null;
	}
}
