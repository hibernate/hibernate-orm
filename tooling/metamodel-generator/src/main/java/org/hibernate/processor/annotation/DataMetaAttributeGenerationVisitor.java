/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.Context;
import org.hibernate.processor.util.Constants;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;

import static org.hibernate.processor.util.NullnessUtil.castNonNull;
import static org.hibernate.processor.util.TypeUtils.getTargetEntity;
import static org.hibernate.processor.util.TypeUtils.isBasicAttribute;
import static org.hibernate.processor.util.TypeUtils.isPropertyGetter;
import static org.hibernate.processor.util.TypeUtils.toArrayTypeString;
import static org.hibernate.processor.util.TypeUtils.toTypeString;

/**
 * @author Gavin King
 */
public class DataMetaAttributeGenerationVisitor extends SimpleTypeVisitor8<@Nullable DataAnnotationMetaAttribute, Element> {

	private final AnnotationMetaEntity entity;
	private final Context context;
	private final @Nullable String path;

	DataMetaAttributeGenerationVisitor(AnnotationMetaEntity entity, Context context) {
		this.entity = entity;
		this.context = context;
		this.path = null;
	}

	DataMetaAttributeGenerationVisitor(AnnotationMetaEntity entity, String path, Context context) {
		this.entity = entity;
		this.context = context;
		this.path = path;
	}

	private Types typeUtils() {
		return context.getTypeUtils();
	}

	@Override
	public @Nullable DataAnnotationMetaAttribute visitPrimitive(PrimitiveType primitiveType, Element element) {
		return new DataAnnotationMetaAttribute( entity, element, toTypeString( primitiveType ), path );
	}

	@Override
	public @Nullable DataAnnotationMetaAttribute visitArray(ArrayType arrayType, Element element) {
		return new DataAnnotationMetaAttribute( entity, element, toArrayTypeString( arrayType, context ), path );
	}

	@Override
	public @Nullable DataAnnotationMetaAttribute visitTypeVariable(TypeVariable typeVariable, Element element) {
		// METAGEN-29 - for a type variable we use the upper bound
		return new DataAnnotationMetaAttribute( entity, element,
				typeUtils().erasure( typeVariable.getUpperBound() ).toString(), path );
	}

	@Override
	public @Nullable DataAnnotationMetaAttribute visitDeclared(DeclaredType declaredType, Element element) {
		final TypeElement returnedElement = (TypeElement) typeUtils().asElement( declaredType );
		// WARNING: .toString() is necessary here since Name equals does not compare to String
		final String returnTypeName = castNonNull( returnedElement ).getQualifiedName().toString();
		final String collection = Constants.COLLECTIONS.get( returnTypeName );
		final String targetEntity = getTargetEntity( element.getAnnotationMirrors() );
		if ( collection != null ) {
			return null;
		}
		else if ( isBasicAttribute( element, returnedElement, context ) ) {
			final String type = targetEntity != null ? targetEntity : returnedElement.getQualifiedName().toString();
			return new DataAnnotationMetaAttribute( entity, element, type, path );
		}
		else {
			return null;
		}
	}

	@Override
	public @Nullable DataAnnotationMetaAttribute visitExecutable(ExecutableType executable, Element element) {
		return isPropertyGetter( executable, element )
				? executable.getReturnType().accept(this, element)
				: null;
	}
}
