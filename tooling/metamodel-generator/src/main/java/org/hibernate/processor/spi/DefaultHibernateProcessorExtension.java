package org.hibernate.processor.spi;

import jakarta.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * The {@link HibernateProcessorExtension} used when no other implementation is
 * registered with {@link java.util.ServiceLoader}. It does nothing: the processor
 * behaves exactly as it would without any framework-specific support.
 */
public class DefaultHibernateProcessorExtension implements HibernateProcessorExtension {

	@Override
	public void init(ProcessingEnvironment processingEnvironment) {
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
