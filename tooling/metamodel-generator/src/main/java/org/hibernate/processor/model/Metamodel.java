/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.model;

import org.jspecify.annotations.Nullable;
import org.hibernate.processor.Context;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * @author Hardy Ferentschik
 */
public interface Metamodel extends ImportContext {
	String getSimpleName();

	String getQualifiedName();

	@Nullable Element getSuperTypeElement();

	String getPackageName();

	List<MetaAttribute> getMembers();

	String generateImports();

	String importType(String fqcn);

	String staticImport(String fqcn, String member);

	Element getElement();

	boolean isMetaComplete();

	Context getContext();

	/**
	 * Is this an implementation of a repository interface?
	 */
	boolean isImplementation();

	/**
	 * Can this be injected into things?
	 */
	boolean isInjectable();

	/**
	 * What is its CDI scope for injection?
	 */
	String scope();

	/**
	 * Is it a Jakarta Data style metamodel interface?
	 */
	boolean isJakartaDataStyle();

	List<AnnotationMirror> inheritedAnnotations();

	String javadoc();
}
