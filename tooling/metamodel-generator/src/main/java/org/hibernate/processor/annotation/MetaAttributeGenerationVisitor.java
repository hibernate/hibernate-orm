/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.Context;
import org.hibernate.processor.util.AccessType;
import org.hibernate.processor.util.AccessTypeInformation;
import org.hibernate.processor.util.Constants;
import org.hibernate.processor.util.NullnessUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
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
import java.util.List;

import static org.hibernate.processor.util.Constants.ELEMENT_COLLECTION;
import static org.hibernate.processor.util.Constants.LIST_ATTRIBUTE;
import static org.hibernate.processor.util.Constants.MANY_TO_ANY;
import static org.hibernate.processor.util.Constants.MANY_TO_MANY;
import static org.hibernate.processor.util.Constants.MAP_KEY_CLASS;
import static org.hibernate.processor.util.Constants.ONE_TO_MANY;
import static org.hibernate.processor.util.NullnessUtil.castNonNull;
import static org.hibernate.processor.util.TypeUtils.DEFAULT_ANNOTATION_PARAMETER_NAME;
import static org.hibernate.processor.util.TypeUtils.determineAnnotationSpecifiedAccessType;
import static org.hibernate.processor.util.TypeUtils.extractClosestRealTypeAsString;
import static org.hibernate.processor.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.processor.util.TypeUtils.getAnnotationValue;
import static org.hibernate.processor.util.TypeUtils.getCollectionElementType;
import static org.hibernate.processor.util.TypeUtils.getKeyType;
import static org.hibernate.processor.util.TypeUtils.getTargetEntity;
import static org.hibernate.processor.util.TypeUtils.hasAnnotation;
import static org.hibernate.processor.util.TypeUtils.isBasicAttribute;
import static org.hibernate.processor.util.TypeUtils.isPropertyGetter;
import static org.hibernate.processor.util.TypeUtils.toArrayTypeString;
import static org.hibernate.processor.util.TypeUtils.toTypeString;

/**
 * @author Hardy Ferentschik
 */
public class MetaAttributeGenerationVisitor extends SimpleTypeVisitor8<@Nullable AnnotationMetaAttribute, Element> {

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
		if ( hasAnnotation( element, MANY_TO_MANY, ONE_TO_MANY, ELEMENT_COLLECTION ) ) {
			return new AnnotationMetaCollection( entity, element, LIST_ATTRIBUTE,
					toTypeString(arrayType.getComponentType()) );
		}
		else {
			return new AnnotationMetaSingleAttribute( entity, element, toArrayTypeString( arrayType, context ) );
		}
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
		assert returnedElement != null;
		// WARNING: .toString() is necessary here since Name equals does not compare to String
		final String returnTypeName = NullnessUtil.castNonNull( returnedElement ).getQualifiedName().toString();
		final String collection = Constants.COLLECTIONS.get( returnTypeName );
		final String targetEntity = getTargetEntity( element.getAnnotationMirrors() );
		if ( collection != null ) {
			return createMetaCollectionAttribute( declaredType, element, returnTypeName, collection, targetEntity );
		}
		else if ( isBasicAttribute( element, returnedElement, context ) ) {
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
				setAccessType( collectionElementType, NullnessUtil.castNonNull( collectionElement ) );
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

	private String getMapKeyType(DeclaredType declaredType, Element element) {
		final AnnotationMirror annotationMirror = getAnnotationMirror(element, MAP_KEY_CLASS );
		return annotationMirror == null
				? getKeyType( declaredType, context )
				: castNonNull( getAnnotationValue( annotationMirror, DEFAULT_ANNOTATION_PARAMETER_NAME ) )
						.getValue().toString();
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

}

