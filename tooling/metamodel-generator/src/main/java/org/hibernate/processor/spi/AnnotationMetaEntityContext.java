/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.spi;

import jakarta.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import java.util.List;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

/**
 * Gives a {@link HibernateProcessorExtension} access to the state of the metamodel class
 * the processor is currently generating, and lets it add members to that class.
 * <p>
 * An instance is only valid during the call of the extension method it is passed to.
 */
public interface AnnotationMetaEntityContext {

	/**
	 * The metamodel class being generated, for example to {@linkplain Metamodel#importType import
	 * types} so that they can be referenced by short name in generated code.
	 */
	Metamodel metamodel();

	/**
	 * Add a member to the generated metamodel class. If a member with the same name was
	 * already added, it is replaced.
	 *
	 * @param name the name under which the member is registered, which is also used to
	 * detect clashes with user-defined members, see {@link #hasMember(String)}
	 * @param attribute the code to generate
	 */
	void addMember(String name, MetaAttribute attribute);

	/**
	 * Whether a member with the given name was already registered.
	 */
	boolean hasMember(String name);

	/**
	 * The type of the identifier of the {@linkplain #primaryEntity() primary entity},
	 * or {@code null} if it cannot be determined.
	 */
	@Nullable TypeMirror findIdType();

	/**
	 * The entity the generated repository is about: the entity type itself when generating an
	 * entity metamodel, or the entity a repository declares, or {@code null} if unknown.
	 */
	@Nullable TypeElement primaryEntity();

	/**
	 * Whether the type being processed is a Jakarta Data repository, that is, is annotated
	 * {@code @Repository}.
	 */
	boolean isJakartaDataRepository();

	/**
	 * Report a message through the annotation processing environment.
	 *
	 * @param element the element the message is about
	 * @param message the message
	 * @param kind the severity; {@link Diagnostic.Kind#ERROR} fails the compilation
	 */
	void message(Element element, String message, Diagnostic.Kind kind);

	/**
	 * The simple name of the generated class: {@code _Name} for a repository implementation,
	 * and {@code Name_} for a metamodel class.
	 */
	String getConstructorName();

	/**
	 * The name of the data store configured for the repository, or {@code null} if there is none.
	 */
	@Nullable String dataStore();

	/**
	 * Whether {@code @Inject} should be added to generated constructors,
	 * because Jakarta Inject is on the classpath.
	 */
	boolean addInjectAnnotation();

	/**
	 * Whether {@code @Nonnull} should be added to generated members,
	 * because Jakarta Annotations is on the classpath.
	 */
	boolean addNonnullAnnotation();

	/**
	 * The name of the variable, or field, holding a session of the given type
	 * in the generated class.
	 *
	 * @param sessionType the fully qualified name of the session type
	 */
	String getSessionVariableName(String sessionType);

	/**
	 * Declare the expression that the generated query methods call to obtain the session, for
	 * example {@code "SessionOperations.getSession()"}. Use this instead of
	 * {@link #addRepositoryConstructor(String, String)} when the session is not injected.
	 *
	 * @param getter a Java expression of the session type
	 */
	void setSessionGetter(String getter);

	/**
	 * The return type of the given method as source code, with its type arguments, resolved
	 * in the context of the type being processed.
	 */
	String fullReturnType(ExecutableElement method);

	/**
	 * All members of the given type, including inherited ones.
	 */
	List<? extends Element> getAllMembers(TypeElement type);

	/**
	 * Whether the processor generates a no-argument constructor in addition to the injecting
	 * constructor, to make the repository proxyable by CDI.
	 */
	boolean needsDefaultConstructor();

	/**
	 * Generate a constructor which takes the session as parameter, annotated
	 * with {@code @Inject} if available, together with an accessor method returning it, and use it
	 * as the session of the repository.
	 *
	 * @param name the name of the session accessor method and of the registered member
	 * @param sessionType the fully qualified name of the type of the session
	 */
	void addRepositoryConstructor(String name, String sessionType);
}
