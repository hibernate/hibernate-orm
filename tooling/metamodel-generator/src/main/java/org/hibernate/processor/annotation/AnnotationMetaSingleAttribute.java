/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.hibernate.processor.model.MetaSingleAttribute;
import org.hibernate.processor.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaSingleAttribute extends AnnotationMetaAttribute implements MetaSingleAttribute {

	public AnnotationMetaSingleAttribute(AnnotationMetaEntity parent, Element element, String type) {
		super( parent, element, type );
	}

	@Override
	public final String getMetaType() {
		return Constants.SINGULAR_ATTRIBUTE;
	}

	@Override
	public List<AnnotationMirror> inheritedAnnotations() {
		return new ArrayList<>(element.getAnnotationMirrors());
	}
}
