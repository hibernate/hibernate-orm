/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import javax.lang.model.element.Element;

import org.hibernate.processor.model.MetaCollection;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaCollection extends AnnotationMetaAttribute implements MetaCollection {
	private final String collectionType;

	public AnnotationMetaCollection(AnnotationMetaEntity parent, Element element, String collectionType, String elementType) {
		super( parent, element, elementType );
		this.collectionType = collectionType;
	}

	@Override
	public String getMetaType() {
		return collectionType;
	}
}
