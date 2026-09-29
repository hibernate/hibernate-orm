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
 * Context passed to {@link HibernateProcessorExtension} methods
 * to give the SPI access to the annotation meta entity's internal state.
 */
public interface AnnotationMetaEntityContext {

	Metamodel metamodel();

	void addMember(String name, MetaAttribute attribute);

	boolean hasMember(String name);

	@Nullable TypeMirror findIdType();

	@Nullable TypeElement primaryEntity();

	boolean isJakartaDataRepository();

	void message(Element element, String message, Diagnostic.Kind kind);

	String getConstructorName();

	@Nullable String dataStore();

	boolean addInjectAnnotation();

	boolean addNonnullAnnotation();

	String getSessionVariableName(String sessionType);

	void setSessionGetter(String getter);

	String fullReturnType(ExecutableElement method);

	List<? extends Element> getAllMembers(TypeElement type);

	boolean needsDefaultConstructor();

	void addRepositoryConstructor(String name, String sessionType);
}
