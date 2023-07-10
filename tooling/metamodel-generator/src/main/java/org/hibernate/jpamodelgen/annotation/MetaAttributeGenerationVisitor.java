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
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.Constants;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.jpamodelgen.util.TypeUtils.isClassOrRecordType;
import static org.hibernate.jpamodelgen.util.NullnessUtil.castNonNull;
import static org.hibernate.jpamodelgen.util.StringUtil.isProperty;
import static org.hibernate.jpamodelgen.util.TypeUtils.DEFAULT_ANNOTATION_PARAMETER_NAME;
import static org.hibernate.jpamodelgen.util.TypeUtils.containsAnnotation;
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

	@Override
	public @Nullable AnnotationMetaAttribute visitPrimitive(PrimitiveType t, Element element) {
		return new AnnotationMetaSingleAttribute( entity, element, toTypeString( t ) );
	}

	@Override
	public @Nullable AnnotationMetaAttribute visitArray(ArrayType t, Element element) {
		// METAGEN-2 - For now we handle arrays as SingularAttribute
		// The code below is an attempt to be closer to the spec and only allow byte[], Byte[], char[] and Character[]
//			AnnotationMetaSingleAttribute attribute = null;
//			TypeMirror componentMirror = t.getComponentType();
//			if ( TypeKind.CHAR.equals( componentMirror.getKind() )
//					|| TypeKind.BYTE.equals( componentMirror.getKind() ) ) {
//				attribute = new AnnotationMetaSingleAttribute( entity, element, TypeUtils.toTypeString( t ) );
//			}
//			else if ( TypeKind.DECLARED.equals( componentMirror.getKind() ) ) {
//				TypeElement componentElement = ( TypeElement ) context.getProcessingEnvironment()
//						.getTypeUtils()
//						.asElement( componentMirror );
//				if ( BASIC_ARRAY_TYPES.contains( componentElement.getQualifiedName().toString() ) ) {
//					attribute = new AnnotationMetaSingleAttribute( entity, element, TypeUtils.toTypeString( t ) );
//				}
//			}
//			return attribute;
		return new AnnotationMetaSingleAttribute( entity, element, toArrayTypeString( t, context ) );
	}

	@Override
	public @Nullable AnnotationMetaAttribute visitTypeVariable(TypeVariable t, Element element) {
		// METAGEN-29 - for a type variable we use the upper bound
		final TypeMirror erasedType = context.getTypeUtils().erasure( t.getUpperBound() );
		return new AnnotationMetaSingleAttribute( entity, element, erasedType.toString() );
	}

	@Override
	public @Nullable AnnotationMetaAttribute visitDeclared(DeclaredType declaredType, Element element) {
		final TypeElement returnedElement = (TypeElement) context.getTypeUtils().asElement( declaredType );
		// WARNING: .toString() is necessary here since Name equals does not compare to String
		final String fqNameOfReturnType = returnedElement.getQualifiedName().toString();
		final String collection = Constants.COLLECTIONS.get( fqNameOfReturnType );
		final String targetEntity = getTargetEntity( element.getAnnotationMirrors() );
		if ( collection != null ) {
			return createMetaCollectionAttribute( declaredType, element, fqNameOfReturnType, collection, targetEntity );
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
			DeclaredType declaredType, Element element, String fqNameOfReturnType, String collection,
			@Nullable String targetEntity) {
		if ( containsAnnotation( element, Constants.ELEMENT_COLLECTION ) ) {
			final String explicitTargetEntity = getTargetEntity( element.getAnnotationMirrors() );
			final TypeMirror collectionElementType =
					getCollectionElementType( declaredType, fqNameOfReturnType, explicitTargetEntity, context );
			if ( collectionElementType.getKind() == TypeKind.DECLARED ) {
				final TypeElement collectionElement = (TypeElement)
						context.getTypeUtils().asElement( collectionElementType );
				setAccessType( collectionElementType, collectionElement );
			}
		}
		return createMetaAttribute( declaredType, element, collection, targetEntity );
	}

	private AnnotationMetaAttribute createMetaAttribute(
			DeclaredType declaredType, Element element, String collection, @Nullable String targetEntity) {
		if ( containsAnnotation( element,
				Constants.ONE_TO_MANY, Constants.MANY_TO_MANY,
				Constants.MANY_TO_ANY, Constants.ELEMENT_COLLECTION ) ) {
			if ( collection.equals( Constants.MAP_ATTRIBUTE ) ) { //TODO: pretty fragile!
				return new AnnotationMetaMap(
						entity,
						element,
						collection,
						getMapKeyType( declaredType, element ),
						getElementType( declaredType, targetEntity )
				);
			}
			else {
				return new AnnotationMetaCollection(
						entity,
						element,
						collection,
						getElementType( declaredType, targetEntity )
				);
			}
		}
		else {
			final String typeWithVariablesErased = extractClosestRealTypeAsString( declaredType, context );
			return new AnnotationMetaSingleAttribute( entity, element, typeWithVariablesErased );
		}
	}

	private void setAccessType(TypeMirror collectionElementType, TypeElement collectionElement) {
		final AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( collectionElementType.toString() );
		if ( accessTypeInfo == null ) {
			final AccessTypeInformation newAccessTypeInfo = new AccessTypeInformation(
					collectionElementType.toString(),
					collectionElement == null ? null : determineAnnotationSpecifiedAccessType( collectionElement ),
					entity.getEntityAccessTypeInfo().getAccessType()
			);
			context.addAccessTypeInformation( collectionElementType.toString(), newAccessTypeInfo );
		}
		else {
			accessTypeInfo.setDefaultAccessType( entity.getEntityAccessTypeInfo().getAccessType() );
		}
	}

	@Override
	public @Nullable AnnotationMetaAttribute visitExecutable(ExecutableType t, Element p) {
		if ( p.getKind() == ElementKind.METHOD
				&& isProperty( p.getSimpleName().toString(), toTypeString( t.getReturnType() ) ) ) {
			return t.getReturnType().accept( this, p );
		}
		else {
			return null;
		}
	}

	private boolean isBasicAttribute(Element element, Element returnedElement) {
		if ( containsAnnotation( element, Constants.BASIC )
				|| containsAnnotation( element, Constants.ONE_TO_ONE )
				|| containsAnnotation( element, Constants.MANY_TO_ONE )
				|| containsAnnotation( element, Constants.EMBEDDED_ID )
				|| containsAnnotation( element, Constants.ID ) ) {
			return true;
		}

		// METAGEN-28
		if ( getAnnotationMirror( element, ORG_HIBERNATE_ANNOTATIONS_TYPE ) != null ) {
			return true;
		}

		return returnedElement.asType().accept( new BasicAttributeVisitor( context ), returnedElement );
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
			if ( isAnnotationMirrorOfType( mirror, Constants.ELEMENT_COLLECTION ) ) {
				return getFullyQualifiedClassNameOfTargetEntity( mirror, "targetClass" );
			}
			else if ( isAnnotationMirrorOfType( mirror, Constants.ONE_TO_MANY )
					|| isAnnotationMirrorOfType( mirror, Constants.MANY_TO_MANY )
					|| isAnnotationMirrorOfType( mirror, Constants.MANY_TO_ONE )
					|| isAnnotationMirrorOfType( mirror, Constants.ONE_TO_ONE ) ) {
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

/**
 * Checks whether the visited type is a basic attribute according to the JPA 2 spec
 * ( section 2.8 Mapping Defaults for Non-Relationship Fields or Properties)
 */
class BasicAttributeVisitor extends SimpleTypeVisitor8<Boolean, Element> {

	private final Context context;

	public BasicAttributeVisitor(Context context) {
		super( false );
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
		return Constants.BASIC_ARRAY_TYPES.contains( componentElement.getQualifiedName().toString() );
	}

	@Override
	public Boolean visitDeclared(DeclaredType declaredType, Element element) {
		if ( ElementKind.ENUM.equals( element.getKind() ) ) {
			return true;
		}

		if ( isClassOrRecordType( element )
				|| element.getKind() == ElementKind.INTERFACE ) {
			final TypeElement typeElement = (TypeElement) element;
			final String typeName = typeElement.getQualifiedName().toString();
			if ( Constants.BASIC_TYPES.contains( typeName ) ) {
				return true;
			}
			if ( containsAnnotation( element, Constants.EMBEDDABLE ) ) {
				return true;
			}
			final TypeMirror serializableType =
					context.getElementUtils()
							.getTypeElement(java.io.Serializable.class.getName())
							.asType();
			if ( context.getTypeUtils().isSubtype( typeElement.asType(), serializableType) ) {
				return true;
			}
		}

		return false;
	}
}
