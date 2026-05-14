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
	private final String metaType;

	public AnnotationMetaSingleAttribute(AnnotationMetaEntity parent, Element element, String type) {
		this( parent, element, type, Constants.SINGULAR_ATTRIBUTE );
	}

	public AnnotationMetaSingleAttribute(AnnotationMetaEntity parent, Element element, String type, String metaType) {
		super( parent, element, type );
		this.metaType = metaType;
	}

	@Override
	public final String getMetaType() {
		return metaType;
	}

	@Override
	public String getAttributeDeclarationString() {
		if ( !isSingleGenericAttribute() ) {
			return super.getAttributeDeclarationString();
		}
		return new StringBuilder()
				.append("\n/**\n * Static metamodel for attribute {@link ")
				.append(parent.getQualifiedName())
				.append('#')
				.append(element.getSimpleName())
				.append("}\n **/\n")
				.append("public static volatile ")
				.append(parent.importType(getMetaType()))
				.append('<')
				.append(parent.importType(parent.getQualifiedName()))
				.append('>')
				.append(' ')
				.append(getPropertyName())
				.append(';')
				.toString();
	}

	private boolean isSingleGenericAttribute() {
		return Constants.TEXT_ATTRIBUTE.equals( metaType )
			|| Constants.BOOLEAN_ATTRIBUTE.equals( metaType );
	}

	@Override
	public List<AnnotationMirror> inheritedAnnotations() {
		return new ArrayList<>(element.getAnnotationMirrors());
	}
}
