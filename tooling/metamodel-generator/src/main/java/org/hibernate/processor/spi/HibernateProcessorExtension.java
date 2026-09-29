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
 * SPI for extending the Hibernate annotation processor with framework-specific
 * behavior (e.g., Quarkus Data, Panache).
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}. When no
 * implementation is found, {@link DefaultHibernateProcessorExtension} is used.
 */
public interface HibernateProcessorExtension {

	void init(ProcessingEnvironment processingEnvironment);

	boolean isInjectionAvailable();

	@Nullable String qualifierAnnotation();

	boolean isExtensionEntity(TypeElement type);

	boolean isExtensionRepository(TypeElement type);

	void addRepositoryMembers(TypeElement element, AnnotationMetaEntityContext context);

	@Nullable SessionSetup setupRepositorySession(
			TypeElement element,
			@Nullable ExecutableElement getter,
			AnnotationMetaEntityContext context);
}
