/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.Context;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;

import static org.hibernate.processor.util.Constants.EMBEDDABLE;
import static org.hibernate.processor.util.Constants.ENTITY;
import static org.hibernate.processor.util.Constants.MAPPED_SUPERCLASS;
import static org.hibernate.processor.util.TypeUtils.getTargetEntity;
import static org.hibernate.processor.util.TypeUtils.hasAnnotation;
import static org.hibernate.processor.util.TypeUtils.isPluralAttribute;
import static org.hibernate.processor.util.TypeUtils.isPropertyGetter;
import static org.hibernate.processor.util.TypeUtils.toArrayTypeString;

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
		return dataAttribute(
				element,
				primitiveType,
				typeUtils().boxedClass( primitiveType ).asType().toString(),
				primitiveType.toString()
		);
	}

	@Override
	public @Nullable DataAnnotationMetaAttribute visitArray(ArrayType arrayType, Element element) {
		final String type = toArrayTypeString( arrayType, context );
		return new DataAnnotationMetaAttribute(
				entity,
				element,
				"jakarta.data.metamodel.BasicAttribute",
				type,
				type,
				path
		);
	}

	@Override
	public @Nullable DataAnnotationMetaAttribute visitTypeVariable(TypeVariable typeVariable, Element element) {
		// METAGEN-29 - for a type variable we use the upper bound
		final TypeMirror upperBound = typeUtils().erasure( typeVariable.getUpperBound() );
		return dataAttribute( element, upperBound, upperBound.toString(), upperBound.toString() );
	}

	@Override
	public @Nullable DataAnnotationMetaAttribute visitDeclared(DeclaredType declaredType, Element element) {
		final TypeElement returnedElement = (TypeElement) typeUtils().asElement( declaredType );
		if ( returnedElement == null ) {
			return null;
		}
		// WARNING: .toString() is necessary here since Name equals does not compare to String
		final String targetEntity = getTargetEntity( element.getAnnotationMirrors() );
		if ( isPluralAttribute( element ) ) {
//			final String returnTypeName = returnedElement.getQualifiedName().toString();
//			final String collection = Constants.COLLECTIONS.get( returnTypeName );
			return null;
		}
		else {
			final String type = targetEntity != null ? targetEntity : returnedElement.getQualifiedName().toString();
			return targetEntity != null || isManagedType( returnedElement )
					? new DataAnnotationMetaAttribute(
							entity,
							element,
							"jakarta.data.metamodel.NavigableAttribute",
							type,
							type,
							path
					)
					: dataAttribute( element, declaredType, type, type );
		}
	}

	@Override
	public @Nullable DataAnnotationMetaAttribute visitExecutable(ExecutableType executable, Element element) {
		return isPropertyGetter( executable, element )
				? executable.getReturnType().accept(this, element)
				: null;
	}

	private DataAnnotationMetaAttribute dataAttribute(
			Element element,
			TypeMirror type,
			String typeDeclaration,
			String classLiteralType) {
		final String metaType = getMetaType( type );
		return new DataAnnotationMetaAttribute(
				entity,
				element,
				metaType,
				typeDeclaration,
				requiresClassLiteral( metaType ) ? classLiteralType : null,
				path
		);
	}

	private static boolean requiresClassLiteral(String metaType) {
		return !"jakarta.data.metamodel.TextAttribute".equals( metaType );
	}

	private String getMetaType(TypeMirror type) {
		final TypeMirror boxedType =
				type.getKind().isPrimitive()
						? typeUtils().boxedClass( (PrimitiveType) type ).asType()
						: typeUtils().erasure( type );
		if ( isSameType( boxedType, String.class.getName() ) ) {
			return "jakarta.data.metamodel.TextAttribute";
		}
		else if ( isSameType( boxedType, Boolean.class.getName() ) ) {
			return "jakarta.data.metamodel.BooleanAttribute";
		}
		else if ( isAssignableTo( boxedType, Number.class.getName() )
				&& isAssignableToComparable( boxedType, boxedType ) ) {
			return "jakarta.data.metamodel.NumericAttribute";
		}
		else if ( isAssignableTo( boxedType, "java.time.temporal.Temporal" )
				&& isAssignableToComparable( boxedType, typeUtils().getWildcardType( null, boxedType ) ) ) {
			return "jakarta.data.metamodel.TemporalAttribute";
		}
		else if ( isAssignableToComparable( boxedType, typeUtils().getWildcardType( null, boxedType ) ) ) {
			return "jakarta.data.metamodel.ComparableAttribute";
		}
		else {
			return "jakarta.data.metamodel.BasicAttribute";
		}
	}

	private boolean isManagedType(TypeElement typeElement) {
		return hasAnnotation( typeElement, ENTITY, EMBEDDABLE, MAPPED_SUPERCLASS );
	}

	private boolean isSameType(TypeMirror type, String typeName) {
		final TypeElement typeElement = context.getTypeElementForFullyQualifiedName( typeName );
		return typeElement != null
				&& typeUtils().isSameType( typeUtils().erasure( type ), typeUtils().erasure( typeElement.asType() ) );
	}

	private boolean isAssignableTo(TypeMirror type, String typeName) {
		final TypeElement typeElement = context.getTypeElementForFullyQualifiedName( typeName );
		return typeElement != null
				&& typeUtils().isAssignable( typeUtils().erasure( type ), typeUtils().erasure( typeElement.asType() ) );
	}

	private boolean isAssignableToComparable(TypeMirror type, TypeMirror comparableArgument) {
		final TypeElement typeElement = context.getTypeElementForFullyQualifiedName( Comparable.class.getName() );
		return typeElement != null
				&& typeUtils().isAssignable(
						type,
						typeUtils().getDeclaredType( typeElement, comparableArgument )
				);
	}
}
