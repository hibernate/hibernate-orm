/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.model;

import org.checkerframework.checker.nullness.qual.Nullable;
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

	@Nullable String getSupertypeName();

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
}
