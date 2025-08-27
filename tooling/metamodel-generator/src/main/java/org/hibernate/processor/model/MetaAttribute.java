/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.model;

import javax.lang.model.element.AnnotationMirror;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * @author Hardy Ferentschik
 */
public interface MetaAttribute {

	boolean hasTypedAttribute();

	boolean hasStringAttribute();

	String getAttributeDeclarationString();

	String getAttributeNameDeclarationString();

	String getMetaType();

	String getPropertyName();

	String getTypeDeclaration();

	Metamodel getHostingEntity();

	default List<AnnotationMirror> inheritedAnnotations() {
		return emptyList();
	}
}
