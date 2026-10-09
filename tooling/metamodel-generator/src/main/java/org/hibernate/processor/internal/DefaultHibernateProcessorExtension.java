package org.hibernate.processor.internal;

import jakarta.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import org.hibernate.processor.spi.AnnotationMetaEntityContext;
import org.hibernate.processor.spi.HibernateProcessorExtension;
import org.hibernate.processor.spi.SessionSetup;

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
	public @Nullable String sessionQualifier(@Nullable String dataStore, AnnotationMetaEntityContext context) {
		return null;
	}

	@Override
	public boolean usesConstructorInjection(AnnotationMetaEntityContext context) {
		return false;
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
