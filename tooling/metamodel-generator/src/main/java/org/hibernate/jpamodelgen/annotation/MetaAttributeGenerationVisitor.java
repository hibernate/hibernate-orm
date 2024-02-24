/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.Constants;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.jpamodelgen.util.Constants.BASIC;
import static org.hibernate.jpamodelgen.util.Constants.ELEMENT_COLLECTION;
import static org.hibernate.jpamodelgen.util.Constants.EMBEDDED_ID;
import static org.hibernate.jpamodelgen.util.Constants.ID;
import static org.hibernate.jpamodelgen.util.Constants.MANY_TO_ANY;
import static org.hibernate.jpamodelgen.util.Constants.MANY_TO_MANY;
import static org.hibernate.jpamodelgen.util.Constants.MANY_TO_ONE;
import static org.hibernate.jpamodelgen.util.Constants.ONE_TO_MANY;
import static org.hibernate.jpamodelgen.util.Constants.ONE_TO_ONE;
import static org.hibernate.jpamodelgen.util.TypeUtils.hasAnnotation;
import static org.hibernate.jpamodelgen.util.NullnessUtil.castNonNull;
import static org.hibernate.jpamodelgen.util.StringUtil.isProperty;
import static org.hibernate.jpamodelgen.util.TypeUtils.DEFAULT_ANNOTATION_PARAMETER_NAME;
import static org.hibernate.jpamodelgen.util.TypeUtils.determineAnnotationSpecifiedAccessType;
import static org.hibernate.jpamodelgen.util.TypeUtils.extractClosestRealTypeAsString;
import static org.hibernate.jpamodelgen.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.jpamodelgen.util.TypeUtils.getAnnotationValue;
import static org.hibernate.jpamodelgen.util.TypeUtils.getCollectionElementType;
import static org.hibernate.jpamodelgen.util.TypeUtils.getKeyType;
import static org.hibernate.jpamodelgen.util.TypeUtils.isAnnotationMirrorOfType;
import static org.hibernate.jpamodelgen.util.TypeUtils.toArrayTypeString;
import static org.hibernate.jpamodelgen.util.TypeUtils.toTypeString;

/**
 * @author Hardy Ferentschik
 */
public class MetaAttributeGenerationVisitor extends SimpleTypeVisitor8<@Nullable AnnotationMetaAttribute, Element> {

	/**
	 * FQCN of the Hibernate-specific {@code @Target} annotation.
	 * We do not use the class directly to avoid depending on Hibernate Core.
	 */
	private static final String ORG_HIBERNATE_ANNOTATIONS_TARGET = "org.hibernate.annotations.Target";

	/**
	 * FQCN of the Hibernate-specific {@code @Type} annotation.
	 * We do not use the class directly to avoid depending on Hibernate Core.
	 */
	private static final String ORG_HIBERNATE_ANNOTATIONS_TYPE = "org.hibernate.annotations.Type";

	private final AnnotationMetaEntity entity;
	private final Context context;

	MetaAttributeGenerationVisitor(AnnotationMetaEntity entity, Context context) {
		this.entity = entity;
		this.context = context;
	}

	private Types typeUtils() {
		return context.getTypeUtils();
	}

	@Override
	public @Nullable AnnotationMetaAttribute visitPrimitive(PrimitiveType primitiveType, Element element) {
		return new AnnotationMetaSingleAttribute( entity, element, toTypeString( primitiveType ) );
	}

	@Override
	public @Nullable AnnotationMetaAttribute visitArray(ArrayType arrayType, Element element) {
		return new AnnotationMetaSingleAttribute( entity, element, toArrayTypeString( arrayType, context ) );
	}

	@Override
	public @Nullable AnnotationMetaAttribute visitTypeVariable(TypeVariable typeVariable, Element element) {
		// METAGEN-29 - for a type variable we use the upper bound
		return new AnnotationMetaSingleAttribute( entity, element,
				typeUtils().erasure( typeVariable.getUpperBound() ).toString() );
	}

	@Override
	public @Nullable AnnotationMetaAttribute visitDeclared(DeclaredType declaredType, Element element) {
		final TypeElement returnedElement = (TypeElement) typeUtils().asElement( declaredType );
		// WARNING: .toString() is necessary here since Name equals does not compare to String
		final String returnTypeName = returnedElement.getQualifiedName().toString();
		final String collection = Constants.COLLECTIONS.get( returnTypeName );
		final String targetEntity = getTargetEntity( element.getAnnotationMirrors() );
		if ( collection != null ) {
			return createMetaCollectionAttribute( declaredType, element, returnTypeName, collection, targetEntity );
		}
		else if ( isBasicAttribute( element, returnedElement ) ) {
			final String type = targetEntity != null ? targetEntity : returnedElement.getQualifiedName().toString();
			return new AnnotationMetaSingleAttribute( entity, element, type );
		}
		else {
			return null;
		}
	}

	private AnnotationMetaAttribute createMetaCollectionAttribute(
			DeclaredType declaredType, Element element, String returnTypeName, String collection,
			@Nullable String targetEntity) {
		if ( hasAnnotation( element, ELEMENT_COLLECTION ) ) {
			final String explicitTargetEntity = getTargetEntity( element.getAnnotationMirrors() );
			final TypeMirror collectionElementType =
					getCollectionElementType( declaredType, returnTypeName, explicitTargetEntity, context );
			if ( collectionElementType.getKind() == TypeKind.DECLARED ) {
				final TypeElement collectionElement = (TypeElement) typeUtils().asElement( collectionElementType );
				setAccessType( collectionElementType, collectionElement );
			}
		}
		return createMetaAttribute( declaredType, element, collection, targetEntity );
	}

	private AnnotationMetaAttribute createMetaAttribute(
			DeclaredType declaredType, Element element, String collection, @Nullable String targetEntity) {
		if ( hasAnnotation( element, ONE_TO_MANY, MANY_TO_MANY, MANY_TO_ANY, ELEMENT_COLLECTION ) ) {
			final String elementType = getElementType( declaredType, targetEntity );
			if ( collection.equals( Constants.MAP_ATTRIBUTE ) ) { //TODO: pretty fragile!
				final String keyType = getMapKeyType( declaredType, element );
				return new AnnotationMetaMap( entity, element, collection, keyType, elementType );
			}
			else {
				return new AnnotationMetaCollection( entity, element, collection, elementType );
			}
		}
		else {
			return new AnnotationMetaSingleAttribute( entity, element,
					extractClosestRealTypeAsString( declaredType, context ) );
		}
	}

	private void setAccessType(TypeMirror collectionElementType, TypeElement collectionElement) {
		final String elementTypeName = collectionElementType.toString();
		final AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( elementTypeName );
		final AccessType entityAccessType = entity.getEntityAccessTypeInfo().getAccessType();
		if ( accessTypeInfo == null ) {
			context.addAccessTypeInformation(
					elementTypeName,
					new AccessTypeInformation(
							elementTypeName,
							collectionElement == null ? null
									: determineAnnotationSpecifiedAccessType( collectionElement ),
							entityAccessType
					)
			);
		}
		else {
			accessTypeInfo.setDefaultAccessType( entityAccessType );
		}
	}

	@Override
	public @Nullable AnnotationMetaAttribute visitExecutable(ExecutableType executable, Element element) {
		return isPropertyGetter( executable, element )
				? executable.getReturnType().accept(this, element)
				: null;
	}

	private static boolean isPropertyGetter(ExecutableType executable, Element element) {
		return element.getKind() == ElementKind.METHOD
			&& isProperty( element.getSimpleName().toString(),
				toTypeString( executable.getReturnType() ) );
	}

	private boolean isBasicAttribute(Element element, Element returnedElement) {
		return hasAnnotation( element, BASIC, ONE_TO_ONE, MANY_TO_ONE, EMBEDDED_ID, ID )
			|| hasAnnotation( element, ORG_HIBERNATE_ANNOTATIONS_TYPE ) // METAGEN-28
			|| returnedElement.asType().accept( new BasicAttributeVisitor( context ), returnedElement );
	}

	private String getMapKeyType(DeclaredType declaredType, Element element) {
		final AnnotationMirror annotationMirror = getAnnotationMirror(element, Constants.MAP_KEY_CLASS );
		return annotationMirror == null
				? getKeyType( declaredType, context )
				: castNonNull( getAnnotationValue( annotationMirror, DEFAULT_ANNOTATION_PARAMETER_NAME ) ).toString();
	}

	private String getElementType(DeclaredType declaredType, @Nullable String targetEntity) {
		if ( targetEntity != null ) {
			return targetEntity;
		}
		else {
			final List<? extends TypeMirror> mirrors = declaredType.getTypeArguments();
			switch ( mirrors.size() ) {
				case 0:
					return "?";
				case 1:
					return extractClosestRealTypeAsString( mirrors.get( 0 ), context );
				case 2:
					return extractClosestRealTypeAsString( mirrors.get( 1 ), context );
				default:
					context.logMessage(
							Diagnostic.Kind.WARNING,
							"Unable to find the closest solid type" + declaredType
					);
					return "?";
			}
		}
	}

	/**
	 * @param annotations list of annotation mirrors.
	 *
	 * @return target entity class name as string or {@code null} if no targetEntity is here or if equals to void
	 */
	private @Nullable String getTargetEntity(List<? extends AnnotationMirror> annotations) {
		for ( AnnotationMirror mirror : annotations ) {
			if ( isAnnotationMirrorOfType( mirror, ELEMENT_COLLECTION ) ) {
				return getFullyQualifiedClassNameOfTargetEntity( mirror, "targetClass" );
			}
			else if ( isAnnotationMirrorOfType( mirror, ONE_TO_MANY )
					|| isAnnotationMirrorOfType( mirror, MANY_TO_MANY )
					|| isAnnotationMirrorOfType( mirror, MANY_TO_ONE )
					|| isAnnotationMirrorOfType( mirror, ONE_TO_ONE ) ) {
				return getFullyQualifiedClassNameOfTargetEntity( mirror, "targetEntity" );
			}
			else if ( isAnnotationMirrorOfType( mirror, ORG_HIBERNATE_ANNOTATIONS_TARGET ) ) {
				return getFullyQualifiedClassNameOfTargetEntity( mirror, "value" );
			}
		}
		return null;
	}

	private @Nullable String getFullyQualifiedClassNameOfTargetEntity(AnnotationMirror mirror, String parameterName) {
		assert mirror != null;
		assert parameterName != null;

		final Object parameterValue = getAnnotationValue( mirror, parameterName );
		if ( parameterValue != null ) {
			final TypeMirror parameterType = (TypeMirror) parameterValue;
			if ( parameterType.getKind() != TypeKind.VOID ) {
				return parameterType.toString();
			}
		}
		return null;
	}
}

