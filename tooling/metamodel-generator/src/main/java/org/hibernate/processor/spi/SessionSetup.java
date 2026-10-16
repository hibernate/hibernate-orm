package org.hibernate.processor.spi;

import org.hibernate.Incubating;

/**
 * The result of
 * {@link HibernateProcessorExtension#setupRepositorySession(javax.lang.model.element.TypeElement,
 * javax.lang.model.element.ExecutableElement, AnnotationMetaEntityContext)}:
 * how a generated repository obtains its session.
 *
 * @param sessionType the fully qualified name of the type of the session, for example
 * {@code org.hibernate.StatelessSession}, which determines the kind of code generated
 * for the repository methods
 * @param isRepository whether the type is a repository for which an implementation must be
 * generated. When {@code false}, the session type is only used for the static methods
 * generated in the metamodel class, and no repository implementation is generated.
 */
@Incubating(since = "8.0", group = "processor-extension")
public record SessionSetup(String sessionType, boolean isRepository) {
}
