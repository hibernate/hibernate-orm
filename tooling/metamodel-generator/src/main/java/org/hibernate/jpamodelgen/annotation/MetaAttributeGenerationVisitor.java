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
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.util.StringUtil;
import org.hibernate.jpamodelgen.util.TypeUtils;

/**
 * @author Hardy Ferentschik
 */
public class MetaAttributeGenerationVisitor extends SimpleTypeVisitor6<AnnotationMetaAttribute, Element> {

	/**
	 * FQCN of the Hibernate specific @Target annotation. We do not use the class directly to avoid depending on Hibernate
	 * Core.
	 */
	private static final String ORG_HIBERNATE_ANNOTATIONS_TARGET = "org.hibernate.annotations.Target";

	/**
	 * FQCN of the Hibernate specific @Type annotation. We do not use the class directly to avoid depending on Hibernate
	 * Core.
	 */
	private static final String ORG_HIBERNATE_ANNOTATIONS_TYPE = "org.hibernate.annotations.Type";

	private final AnnotationMetaEntity entity;
	private final Context context;

	MetaAttributeGenerationVisitor(AnnotationMetaEntity entity, Context context) {
		this.entity = entity;
		this.context = context;
	}

	@Override
	public AnnotationMetaAttribute visitPrimitive(PrimitiveType t, Element element) {
		return new AnnotationMetaSingleAttribute( entity, element, TypeUtils.toTypeString( t ) );
	}

	@Override
	public AnnotationMetaAttribute visitArray(ArrayType t, Element element) {
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
		return new AnnotationMetaSingleAttribute( entity, element, TypeUtils.toArrayTypeString( t, context ) );
	}

	@Override
	public AnnotationMetaAttribute visitTypeVariable(TypeVariable t, Element element) {
		// METAGEN-29 - for a type variable we use the upper bound
		TypeMirror mirror = t.getUpperBound();
		TypeMirror erasedType = context.getTypeUtils().erasure( mirror );
		return new AnnotationMetaSingleAttribute(
				entity, element, erasedType.toString()
		);
	}

	@Override
	public AnnotationMetaAttribute visitDeclared(DeclaredType declaredType, Element element) {
		AnnotationMetaAttribute metaAttribute = null;
		TypeElement returnedElement = (TypeElement) context.getTypeUtils().asElement( declaredType );
		// WARNING: .toString() is necessary here since Name equals does not compare to String
		String fqNameOfReturnType = returnedElement.getQualifiedName().toString();
		String collection = Constants.COLLECTIONS.get( fqNameOfReturnType );
		String targetEntity = getTargetEntity( element.getAnnotationMirrors() );
		if ( collection != null ) {
			return createMetaCollectionAttribute(
					declaredType, element, fqNameOfReturnType, collection, targetEntity
			);
		}
		else if ( isBasicAttribute( element, returnedElement ) ) {
			String type = targetEntity != null ? targetEntity : returnedElement.getQualifiedName().toString();
			return new AnnotationMetaSingleAttribute( entity, element, type );
		}
		return metaAttribute;
	}

	private AnnotationMetaAttribute createMetaCollectionAttribute(DeclaredType declaredType, Element element, String fqNameOfReturnType, String collection, String targetEntity) {
		if ( TypeUtils.containsAnnotation( element, Constants.ELEMENT_COLLECTION ) ) {
			String explicitTargetEntity = getTargetEntity( element.getAnnotationMirrors() );
			TypeMirror collectionElementType = TypeUtils.getCollectionElementType(
					declaredType, fqNameOfReturnType, explicitTargetEntity, context
			);
			final TypeElement collectionElement = (TypeElement) context.getTypeUtils()
					.asElement( collectionElementType );
			AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo(
					collectionElementType.toString() );
			if ( accessTypeInfo == null ) {
				AccessType explicitAccessType = null;
				if ( collectionElement != null ) {
					explicitAccessType = TypeUtils.determineAnnotationSpecifiedAccessType(
						collectionElement
					);
				}
				accessTypeInfo = new AccessTypeInformation(
						collectionElementType.toString(),
						explicitAccessType,
						entity.getEntityAccessTypeInfo().getAccessType()
				);
				context.addAccessTypeInformation( collectionElementType.toString(), accessTypeInfo );
			}
			else {
				accessTypeInfo.setDefaultAccessType( entity.getEntityAccessTypeInfo().getAccessType() );
			}
		}
		if ( collection.equals( Constants.MAP_ATTRIBUTE ) ) {
			return createAnnotationMetaAttributeForMap( declaredType, element, collection, targetEntity );
		}
		return new AnnotationMetaCollection(
				entity, element, collection, getElementType( declaredType, targetEntity )
		);
	}

	@Override
	public AnnotationMetaAttribute visitExecutable(ExecutableType t, Element p) {
		if ( !p.getKind().equals( ElementKind.METHOD ) ) {
			return null;
		}

		String string = p.getSimpleName().toString();
		if ( !StringUtil.isProperty( string, TypeUtils.toTypeString( t.getReturnType() ) ) ) {
			return null;
		}

		TypeMirror returnType = t.getReturnType();
		return returnType.accept( this, p );
	}

	private boolean isBasicAttribute(Element element, Element returnedElement) {
		if ( TypeUtils.containsAnnotation( element, Constants.BASIC )
				|| TypeUtils.containsAnnotation( element, Constants.ONE_TO_ONE )
				|| TypeUtils.containsAnnotation( element, Constants.MANY_TO_ONE )
				|| TypeUtils.containsAnnotation( element, Constants.EMBEDDED_ID )
				|| TypeUtils.containsAnnotation( element, Constants.ID ) ) {
			return true;
		}

		// METAGEN-28
		if ( TypeUtils.getAnnotationMirror( element, ORG_HIBERNATE_ANNOTATIONS_TYPE ) != null ) {
			return true;
		}

		BasicAttributeVisitor basicVisitor = new BasicAttributeVisitor( context );
		return returnedElement.asType().accept( basicVisitor, returnedElement );
	}

	private AnnotationMetaAttribute createAnnotationMetaAttributeForMap(DeclaredType declaredType, Element element, String collection, String targetEntity) {
		String keyType;
		if ( TypeUtils.containsAnnotation( element, Constants.MAP_KEY_CLASS ) ) {
			TypeMirror typeMirror = (TypeMirror) TypeUtils.getAnnotationValue(
					TypeUtils.getAnnotationMirror(
							element, Constants.MAP_KEY_CLASS
					), TypeUtils.DEFAULT_ANNOTATION_PARAMETER_NAME
			);
			keyType = typeMirror.toString();
		}
		else {
			keyType = TypeUtils.getKeyType( declaredType, context );
		}
		return new AnnotationMetaMap(
				entity,
				element,
				collection,
				keyType,
				getElementType( declaredType, targetEntity )
		);
	}

	private String getElementType(DeclaredType declaredType, String targetEntity) {
		if ( targetEntity != null ) {
			return targetEntity;
		}
		final List<? extends TypeMirror> mirrors = declaredType.getTypeArguments();
		if ( mirrors.size() == 1 ) {
			final TypeMirror type = mirrors.get( 0 );
			return TypeUtils.extractClosestRealTypeAsString( type, context );
		}
		else if ( mirrors.size() == 2 ) {
			return TypeUtils.extractClosestRealTypeAsString( mirrors.get( 1 ), context );
		}
		else {
			//for 0 or many
			//0 is expected, many is not
			if ( mirrors.size() > 2 ) {
				context.logMessage(
						Diagnostic.Kind.WARNING, "Unable to find the closest solid type" + declaredType
				);
			}
			return "?";
		}
	}

	/**
	 * @param annotations list of annotation mirrors.
	 *
	 * @return target entity class name as string or {@code null} if no targetEntity is here or if equals to void
	 */
	private String getTargetEntity(List<? extends AnnotationMirror> annotations) {
		String fullyQualifiedTargetEntityName = null;
		for ( AnnotationMirror mirror : annotations ) {
			if ( TypeUtils.isAnnotationMirrorOfType( mirror, Constants.ELEMENT_COLLECTION ) ) {
				fullyQualifiedTargetEntityName = getFullyQualifiedClassNameOfTargetEntity( mirror, "targetClass" );
			}
			else if ( TypeUtils.isAnnotationMirrorOfType( mirror, Constants.ONE_TO_MANY )
					|| TypeUtils.isAnnotationMirrorOfType( mirror, Constants.MANY_TO_MANY )
					|| TypeUtils.isAnnotationMirrorOfType( mirror, Constants.MANY_TO_ONE )
					|| TypeUtils.isAnnotationMirrorOfType( mirror, Constants.ONE_TO_ONE ) ) {
				fullyQualifiedTargetEntityName = getFullyQualifiedClassNameOfTargetEntity( mirror, "targetEntity" );
			}
			else if ( TypeUtils.isAnnotationMirrorOfType( mirror, ORG_HIBERNATE_ANNOTATIONS_TARGET ) ) {
				fullyQualifiedTargetEntityName = getFullyQualifiedClassNameOfTargetEntity( mirror, "value" );
			}
		}
		return fullyQualifiedTargetEntityName;
	}

	private String getFullyQualifiedClassNameOfTargetEntity(AnnotationMirror mirror, String parameterName) {
		assert mirror != null;
		assert parameterName != null;

		String targetEntityName = null;
		Object parameterValue = TypeUtils.getAnnotationValue( mirror, parameterName );
		if ( parameterValue != null ) {
			TypeMirror parameterType = (TypeMirror) parameterValue;
			if ( !parameterType.getKind().equals( TypeKind.VOID ) ) {
				targetEntityName = parameterType.toString();
			}
		}
		return targetEntityName;
	}
}

/**
 * Checks whether the visited type is a basic attribute according to the JPA 2 spec
 * ( section 2.8 Mapping Defaults for Non-Relationship Fields or Properties)
 */
class BasicAttributeVisitor extends SimpleTypeVisitor6<Boolean, Element> {

	private final Context context;

	public BasicAttributeVisitor(Context context) {
		this.context = context;
	}

	@Override
	public Boolean visitPrimitive(PrimitiveType t, Element element) {
		return Boolean.TRUE;
	}

	@Override
	public Boolean visitArray(ArrayType t, Element element) {
		TypeMirror componentMirror = t.getComponentType();
		TypeElement componentElement = (TypeElement) context.getTypeUtils().asElement( componentMirror );

		return Constants.BASIC_ARRAY_TYPES.contains( componentElement.getQualifiedName().toString() );
	}

	@Override
	public Boolean visitDeclared(DeclaredType declaredType, Element element) {
		if ( ElementKind.ENUM.equals( element.getKind() ) ) {
			return Boolean.TRUE;
		}

		if ( ElementKind.CLASS.equals( element.getKind() ) || ElementKind.INTERFACE.equals( element.getKind() ) ) {
			TypeElement typeElement = ( (TypeElement) element );
			String typeName = typeElement.getQualifiedName().toString();
			if ( Constants.BASIC_TYPES.contains( typeName ) ) {
				return Boolean.TRUE;
			}
			if ( TypeUtils.containsAnnotation( element, Constants.EMBEDDABLE ) ) {
				return Boolean.TRUE;
			}
			for ( TypeMirror mirror : typeElement.getInterfaces() ) {
				TypeElement interfaceElement = (TypeElement) context.getTypeUtils().asElement( mirror );
				if ( "java.io.Serializable".equals( interfaceElement.getQualifiedName().toString() ) ) {
					return Boolean.TRUE;
				}
			}
		}
		return Boolean.FALSE;
	}
}
