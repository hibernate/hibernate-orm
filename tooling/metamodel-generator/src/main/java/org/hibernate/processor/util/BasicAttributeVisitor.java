/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.processor.Context;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;
import java.io.Serializable;

import static org.hibernate.processor.util.Constants.BASIC_ARRAY_TYPES;
import static org.hibernate.processor.util.TypeUtils.hasAnnotation;

/**
 * Checks whether the visited type is a basic attribute according to the JPA 2 spec
 * ( section 2.8 Mapping Defaults for Non-Relationship Fields or Properties)
 */
class BasicAttributeVisitor extends SimpleTypeVisitor8<Boolean, Element> {

	private final Context context;

	public BasicAttributeVisitor(Context context) {
		super(false);
		this.context = context;
	}

	@Override
	public Boolean visitPrimitive(PrimitiveType primitiveType, Element element) {
		return true;
	}

	@Override
	public Boolean visitArray(ArrayType arrayType, Element element) {
		final TypeElement componentElement = (TypeElement)
				context.getTypeUtils().asElement( arrayType.getComponentType() );
		return BASIC_ARRAY_TYPES.contains( NullnessUtil.castNonNull( componentElement ).getQualifiedName().toString() );
	}

	@Override
	public Boolean visitDeclared(DeclaredType declaredType, Element element) {
		final ElementKind kind = element.getKind();
		switch ( kind ) {
			case ENUM -> {
				return true;
			}
			case INTERFACE -> {
				// TODO: this isn't really correct
				final TypeElement typeElement = (TypeElement) element;
				final Name qualifiedName = typeElement.getQualifiedName();
				return qualifiedName.contentEquals( "java.sql.Blob" )
					|| qualifiedName.contentEquals( "java.sql.Clob" )
					|| qualifiedName.contentEquals( "java.sql.NClob" );
			}
			case CLASS, RECORD -> {
				// TODO: this is simply wrong
				final TypeElement typeElement = (TypeElement) element;
				return hasAnnotation( element, Constants.EMBEDDABLE )
					|| isSerializable( typeElement );
			}
			default -> {
				return false;
			}
		}
	}

	private boolean isSerializable(TypeElement typeElement) {
		final TypeMirror serializableType =
				context.getElementUtils()
						.getTypeElement(Serializable.class.getName())
						.asType();
		return context.getTypeUtils()
				.isSubtype( typeElement.asType(), serializableType );
	}
}
