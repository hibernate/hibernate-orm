/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import jakarta.persistence.AccessType;
import org.jspecify.annotations.Nullable;
import org.hibernate.processor.Context;
import org.hibernate.processor.MetaModelGenerationException;
import org.hibernate.processor.model.Metamodel;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.beans.Introspector.decapitalize;
import static java.util.stream.Stream.concat;
import static org.hibernate.internal.util.StringHelper.split;
import static org.hibernate.processor.util.AccessTypeInformation.DEFAULT_ACCESS_TYPE;
import static org.hibernate.processor.util.Constants.ACCESS;
import static org.hibernate.processor.util.Constants.BASIC;
import static org.hibernate.processor.util.Constants.ELEMENT_COLLECTION;
import static org.hibernate.processor.util.Constants.EMBEDDABLE;
import static org.hibernate.processor.util.Constants.EMBEDDED;
import static org.hibernate.processor.util.Constants.EMBEDDED_ID;
import static org.hibernate.processor.util.Constants.ENTITY;
import static org.hibernate.processor.util.Constants.ID;
import static org.hibernate.processor.util.Constants.JAVA_OBJECT;
import static org.hibernate.processor.util.Constants.MANY_TO_MANY;
import static org.hibernate.processor.util.Constants.MANY_TO_ONE;
import static org.hibernate.processor.util.Constants.MAP;
import static org.hibernate.processor.util.Constants.MAPPED_SUPERCLASS;
import static org.hibernate.processor.util.Constants.ONE_TO_MANY;
import static org.hibernate.processor.util.Constants.ONE_TO_ONE;
import static org.hibernate.processor.util.NullnessUtil.castNonNull;
import static org.hibernate.processor.util.StringUtil.isProperty;
import static org.hibernate.processor.util.StringUtil.removeDollar;

/**
 * Utility class.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public final class TypeUtils {

	public static final String DEFAULT_ANNOTATION_PARAMETER_NAME = "value";
	private static final Map<TypeKind, String> PRIMITIVE_WRAPPERS = new HashMap<>();
	private static final Map<TypeKind, String> PRIMITIVES = new HashMap<>();

	static {
		PRIMITIVE_WRAPPERS.put( TypeKind.CHAR, "Character" );

		PRIMITIVE_WRAPPERS.put( TypeKind.BYTE, "Byte" );
		PRIMITIVE_WRAPPERS.put( TypeKind.SHORT, "Short" );
		PRIMITIVE_WRAPPERS.put( TypeKind.INT, "Integer" );
		PRIMITIVE_WRAPPERS.put( TypeKind.LONG, "Long" );

		PRIMITIVE_WRAPPERS.put( TypeKind.BOOLEAN, "Boolean" );

		PRIMITIVE_WRAPPERS.put( TypeKind.FLOAT, "Float" );
		PRIMITIVE_WRAPPERS.put( TypeKind.DOUBLE, "Double" );

		PRIMITIVES.put( TypeKind.CHAR, "char" );
		PRIMITIVES.put( TypeKind.BYTE, "byte" );
		PRIMITIVES.put( TypeKind.SHORT, "short" );
		PRIMITIVES.put( TypeKind.INT, "int" );
		PRIMITIVES.put( TypeKind.LONG, "long" );
		PRIMITIVES.put( TypeKind.BOOLEAN, "boolean" );
		PRIMITIVES.put( TypeKind.FLOAT, "float" );
		PRIMITIVES.put( TypeKind.DOUBLE, "double" );
	}

	private TypeUtils() {
	}

	public static String toTypeString(TypeMirror type) {
		return type.getKind().isPrimitive()
				? castNonNull( PRIMITIVE_WRAPPERS.get( type.getKind() ) )
				: TypeRenderingVisitor.toString( type );
	}

	public static String toArrayTypeString(ArrayType type, Context context) {
		final TypeMirror componentType = type.getComponentType();
		if ( componentType.getKind().isPrimitive() ) {
			return PRIMITIVES.get( componentType.getKind() ) + "[]";
		}
		else {
			// When an ArrayType is annotated with an annotation which uses TYPE_USE targets,
			// we cannot simply take the TypeMirror returned by #getComponentType because it
			// itself is an AnnotatedType.
			//
			// The simplest approach here to get the TypeMirror for both ArrayType use cases
			// is to use the visitor to retrieve the underlying TypeMirror.
			final TypeMirror component = componentType.accept(
					new SimpleTypeVisitor8<TypeMirror, Void>() {
						@Override
						protected TypeMirror defaultAction(TypeMirror e, Void aVoid) {
							return e;
						}
					},
					null
			);
			return extractClosestRealTypeAsString( component, context ) + "[]";
		}
	}

	public static @Nullable TypeElement getSuperclassTypeElement(TypeElement element) {
		final TypeMirror superclass = element.getSuperclass();
		//superclass of Object is of NoType which returns some other kind
		if ( superclass.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) superclass;
			return (TypeElement) declaredType.asElement();
		}
		else {
			return null;
		}
	}

	public static String extractClosestRealTypeAsString(TypeMirror type, Context context) {
		final TypeMirror mirror = extractClosestRealType( type, context, new HashSet<>() );
		return mirror == null ? "?" : mirror.toString();
	}

	private static @Nullable TypeMirror lowerBound(@Nullable TypeMirror bound) {
		return bound == null || bound.getKind() == TypeKind.NULL ? null : bound;
	}

	private static @Nullable TypeMirror upperBound(@Nullable TypeMirror bound) {
		if ( bound !=null && bound.getKind() == TypeKind.DECLARED ) {
			final DeclaredType type = (DeclaredType) bound;
			return type.asElement().getSimpleName().contentEquals(JAVA_OBJECT) ? null : bound;
		}
		else {
			return null;
		}
	}

	public static @Nullable TypeMirror extractClosestRealType(TypeMirror type, Context context, Set<TypeVariable> beingVisited) {
		if ( type == null ) {
			return null;
		}
		switch ( type.getKind() ) {
			case TYPEVAR:
				final TypeVariable typeVariable = (TypeVariable) type;
				if ( !beingVisited.add( typeVariable ) ) {
					// A self-referential type variable has to be represented as plain wildcard `?`
					return context.getTypeUtils().getWildcardType( null, null );
				}
				else {
					final WildcardType wildcardType = context.getTypeUtils().getWildcardType(
							upperBound( extractClosestRealType( typeVariable.getUpperBound(), context, beingVisited ) ),
							lowerBound( extractClosestRealType( typeVariable.getLowerBound(), context, beingVisited ) )
					);
					beingVisited.remove( typeVariable );
					return wildcardType;
				}
			case WILDCARD:
				final WildcardType wildcardType = (WildcardType) type;
				return context.getTypeUtils().getWildcardType(
						extractClosestRealType( wildcardType.getExtendsBound(), context, beingVisited ),
						extractClosestRealType( wildcardType.getSuperBound(), context, beingVisited )
				);
			case DECLARED:
				final DeclaredType declaredType = (DeclaredType) type;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				return context.getTypeUtils().getDeclaredType(
						typeElement,
						declaredType.getTypeArguments().stream()
								.map( arg -> extractClosestRealType( arg, context, beingVisited ) )
								.toArray( TypeMirror[]::new )
				);
			default:
				return context.getTypeUtils().erasure( type );
		}
	}

	public static boolean containsAnnotation(Element element, String... annotations) {
		assert element != null;
		assert annotations != null;
		final Set<String> annotationClassNames = Set.of(annotations);
		for ( AnnotationMirror mirror : element.getAnnotationMirrors() ) {
			if ( annotationClassNames.contains( mirror.getAnnotationType().toString() ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
	 * This method uses the string class names for comparison. See also
	 * <a href="http://www.retep.org/2009/02/getting-class-values-from-annotations.html">getting-class-values-from-annotations</a>.
	 *
	 * @param annotationMirror The annotation mirror
	 * @param qualifiedName the fully qualified class name to check against
	 *
	 * @return {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
	 */
	public static boolean isAnnotationMirrorOfType(AnnotationMirror annotationMirror, String qualifiedName) {
		assert annotationMirror != null;
		assert qualifiedName != null;
		final Element element = annotationMirror.getAnnotationType().asElement();
		final TypeElement typeElement = (TypeElement) element;
		return typeElement.getQualifiedName().contentEquals( qualifiedName );
	}

	/**
	 * Checks whether the {@code Element} hosts the annotation with the given fully qualified class name.
	 *
	 * @param element the element to check for the hosted annotation
	 * @param qualifiedName the fully qualified class name of the annotation to check for
	 *
	 * @return the annotation mirror for the specified annotation class from the {@code Element} or {@code null} in case
	 *         the {@code TypeElement} does not host the specified annotation.
	 */
	public static @Nullable AnnotationMirror getAnnotationMirror(Element element, String qualifiedName) {
		assert element != null;
		assert qualifiedName != null;
		for ( AnnotationMirror mirror : element.getAnnotationMirrors() ) {
			if ( isAnnotationMirrorOfType( mirror, qualifiedName ) ) {
				return mirror;
			}
		}
		return null;
	}

	/**
	 * Checks whether the {@code Element} hosts the annotation (directly or inherited) with the given fully qualified class name.
	 *
	 * @param element the element to check for the hosted annotation
	 * @param qualifiedName the fully qualified class name of the annotation to check for
	 *
	 * @return the annotation mirror for the specified annotation class from the {@code Element} or {@code null} in case
	 *         the {@code TypeElement} does not host the specified annotation (directly or inherited).
	 */
	public static @Nullable AnnotationMirror getInheritedAnnotationMirror(Elements elements, Element element, String qualifiedName) {
		assert element != null;
		assert qualifiedName != null;
		for ( AnnotationMirror mirror : elements.getAllAnnotationMirrors(element) ) {
			if ( isAnnotationMirrorOfType( mirror, qualifiedName ) ) {
				return mirror;
			}
		}
		return null;
	}

	public static boolean hasAnnotation(Element element, String qualifiedName) {
		return getAnnotationMirror( element, qualifiedName ) != null;
	}

	public static boolean hasAnnotation(Element element, String... qualifiedNames) {
		for ( String qualifiedName : qualifiedNames ) {
			if ( hasAnnotation( element, qualifiedName ) ) {
				return true;
			}
		}
		return false;
	}

	public static @Nullable AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror) {
		return getAnnotationValue( annotationMirror, DEFAULT_ANNOTATION_PARAMETER_NAME );
	}

	public static @Nullable AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String member) {
		assert annotationMirror != null;
		assert member != null;
		for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
				: annotationMirror.getElementValues().entrySet() ) {
			if ( entry.getKey().getSimpleName().contentEquals(member) ) {
				return entry.getValue();
			}
		}
		return null;
	}

	public static void determineAccessTypeForHierarchy(TypeElement searchedElement, Context context) {
		final String qualifiedName = searchedElement.getQualifiedName().toString();
		context.logMessage( Diagnostic.Kind.OTHER, "Determining access type for " + qualifiedName );
		final AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( qualifiedName );
		if ( accessTypeInfo != null && accessTypeInfo.isAccessTypeResolved() ) {
			context.logMessage(
					Diagnostic.Kind.OTHER,
					"AccessType for " + searchedElement + " found in cache: " + accessTypeInfo
			);
		}
		else {
			// check for explicit access type
			final AccessType forcedAccessType = determineAnnotationSpecifiedAccessType( searchedElement) ;
			if ( forcedAccessType != null ) {
				context.logMessage(
						Diagnostic.Kind.OTHER,
						"Explicit access type on " + searchedElement + ":" + forcedAccessType
				);
				final AccessTypeInformation newAccessTypeInfo =
						new AccessTypeInformation( qualifiedName, forcedAccessType, null );
				context.addAccessTypeInformation( qualifiedName, newAccessTypeInfo );
				updateEmbeddableAccessType( searchedElement, context, forcedAccessType );
			}
			else {
				// need to find the default access type for this class
				// let's check first if this entity is the root of the class hierarchy and defines an id. If so the
				// placement of the id annotation determines the access type
				final AccessType defaultAccessType = getAccessTypeInCaseElementIsRoot( searchedElement, context );
				if ( defaultAccessType != null ) {
					final AccessTypeInformation newAccessTypeInfo =
							new AccessTypeInformation(qualifiedName, null, defaultAccessType);
					context.addAccessTypeInformation( qualifiedName, newAccessTypeInfo );
					updateEmbeddableAccessType( searchedElement, context, defaultAccessType );
					setDefaultAccessTypeForMappedSuperclassesInHierarchy( searchedElement, defaultAccessType, context );
				}
				else {
					// if we end up here we need to recursively look for superclasses
					AccessType newDefaultAccessType = getDefaultAccessForHierarchy( searchedElement, context );
					if ( newDefaultAccessType == null ) {
						newDefaultAccessType = DEFAULT_ACCESS_TYPE;
					}
					final AccessTypeInformation newAccessTypeInfo =
							new AccessTypeInformation( qualifiedName, null, newDefaultAccessType );
					context.addAccessTypeInformation( qualifiedName, newAccessTypeInfo );
					updateEmbeddableAccessType( searchedElement, context, newDefaultAccessType );
				}
			}
		}
	}

	public static TypeMirror getCollectionElementType(
			DeclaredType type, String returnTypeName, @Nullable String explicitTargetEntityName, Context context) {
		if ( explicitTargetEntityName != null ) {
			return context.getElementUtils().getTypeElement( explicitTargetEntityName ).asType();
		}
		else {
			final List<? extends TypeMirror> typeArguments = type.getTypeArguments();
			if ( typeArguments.isEmpty() ) {
				throw new MetaModelGenerationException( "Unable to determine collection type" );
			}
			else if ( MAP.equals( returnTypeName ) ) {
				return typeArguments.get( 1 );
			}
			else {
				return typeArguments.get( 0 );
			}
		}
	}

	private static void updateEmbeddableAccessType(TypeElement element, Context context, AccessType defaultAccessType) {
		for ( Element field : ElementFilter.fieldsIn( element.getEnclosedElements() ) ) {
			updateEmbeddableAccessTypeForMember( context, defaultAccessType, field );
		}

		for ( Element method : ElementFilter.methodsIn( element.getEnclosedElements() ) ) {
			updateEmbeddableAccessTypeForMember( context, defaultAccessType, method );
		}
	}

	private static void updateEmbeddableAccessTypeForMember(Context context, AccessType defaultAccessType, Element member) {
		final @Nullable TypeElement embedded = member.asType().accept( new EmbeddedAttributeVisitor( context ), member );
		if ( embedded != null ) {
			updateEmbeddableAccessType( context, defaultAccessType, embedded );
		}
	}

	private static void updateEmbeddableAccessType(Context context, AccessType defaultAccessType, TypeElement embedded) {
		final String embeddedClassName = embedded.getQualifiedName().toString();
		final AccessType forcedAccessType = determineAnnotationSpecifiedAccessType( embedded );
		final AccessTypeInformation accessTypeInfo =
				forcedAccessType != null
						? new AccessTypeInformation( embeddedClassName, null, forcedAccessType )
						: context.getAccessTypeInfo( embeddedClassName );

		if ( accessTypeInfo == null ) {
			final AccessTypeInformation newAccessTypeInfo =
					new AccessTypeInformation( embeddedClassName, null, defaultAccessType );
			context.addAccessTypeInformation( embeddedClassName, newAccessTypeInfo );
			updateEmbeddableAccessType( embedded, context, defaultAccessType );
			final TypeMirror superclass = embedded.getSuperclass();
			if ( superclass.getKind() == TypeKind.DECLARED ) {
				final DeclaredType declaredType = (DeclaredType) superclass;
				final TypeElement element = (TypeElement) declaredType.asElement();
				updateEmbeddableAccessType( context, defaultAccessType, element );
			}
		}
		else {
			accessTypeInfo.setDefaultAccessType(defaultAccessType);
		}
	}

	private static @Nullable AccessType getDefaultAccessForHierarchy(TypeElement element, Context context) {
		AccessType defaultAccessType = null;
		TypeElement superClass = element;
		do {
			superClass = getSuperclassTypeElement( superClass );
			if ( superClass != null ) {
				final String qualifiedName = superClass.getQualifiedName().toString();
				final AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( qualifiedName );
				if ( accessTypeInfo != null && accessTypeInfo.getDefaultAccessType() != null ) {
					return accessTypeInfo.getDefaultAccessType();
				}
				if ( containsAnnotation( superClass, ENTITY, MAPPED_SUPERCLASS ) ) {
					defaultAccessType = getAccessTypeInCaseElementIsRoot( superClass, context );
					if ( defaultAccessType != null ) {
						final AccessTypeInformation newAccessTypeInfo
								= new AccessTypeInformation( qualifiedName, null, defaultAccessType );
						context.addAccessTypeInformation( qualifiedName, newAccessTypeInfo );

						// we found an id within the class hierarchy and determined a default access type
						// there cannot be any super entity classes (otherwise it would be a configuration error)
						// but there might be mapped super classes
						// Also note, that if two different class hierarchies with different access types have a common
						// mapped super class, the access type of the mapped super class will be the one of the last
						// hierarchy processed. The result is not determined which is odd with the spec
						setDefaultAccessTypeForMappedSuperclassesInHierarchy( superClass, defaultAccessType, context );

						// we found a default access type, no need to look further
						break;
					}
					else {
						defaultAccessType = getDefaultAccessForHierarchy( superClass, context );
					}
				}
			}
		}
		while ( superClass != null );
		return defaultAccessType;
	}

	private static void setDefaultAccessTypeForMappedSuperclassesInHierarchy(TypeElement element, AccessType defaultAccessType, Context context) {
		TypeElement superClass = element;
		do {
			superClass = getSuperclassTypeElement( superClass );
			if ( superClass != null ) {
				final String qualifiedName = superClass.getQualifiedName().toString();
				if ( containsAnnotation( superClass, MAPPED_SUPERCLASS ) ) {
					final AccessType forcedAccessType = determineAnnotationSpecifiedAccessType( superClass );
					final AccessTypeInformation accessTypeInfo =
							forcedAccessType != null
									? new AccessTypeInformation( qualifiedName, null, forcedAccessType )
									: new AccessTypeInformation( qualifiedName, null, defaultAccessType );
					context.addAccessTypeInformation( qualifiedName, accessTypeInfo );
				}
			}
		}
		while ( superClass != null );
	}

	/**
	 * Iterates all elements of a type to check whether they contain the id annotation. If so the placement of this
	 * annotation determines the access type
	 *
	 * @param searchedElement the type to be searched
	 * @param context The global execution context
	 *
	 * @return returns the access type of the element annotated with the id annotation. If no element is annotated
	 *         {@code null} is returned.
	 */
	private static @Nullable AccessType getAccessTypeInCaseElementIsRoot(TypeElement searchedElement, Context context) {
		for ( Element subElement : searchedElement.getEnclosedElements() ) {
			for ( AnnotationMirror entityAnnotation :
					context.getElementUtils().getAllAnnotationMirrors( subElement ) ) {
				if ( isIdAnnotation( entityAnnotation ) ) {
					return getAccessTypeOfIdAnnotation( subElement );
				}
			}
		}
		return null;
	}

	private static @Nullable AccessType getAccessTypeOfIdAnnotation(Element element) {
		return switch ( element.getKind() ) {
			case FIELD -> AccessType.FIELD;
			case METHOD -> AccessType.PROPERTY;
			default -> null;
		};
	}

	private static boolean isIdAnnotation(AnnotationMirror annotationMirror) {
		return isAnnotationMirrorOfType( annotationMirror, ID )
			|| isAnnotationMirrorOfType( annotationMirror, EMBEDDED_ID );
	}

	public static @Nullable AccessType determineAnnotationSpecifiedAccessType(Element element) {
		final AnnotationMirror mirror = getAnnotationMirror( element, ACCESS );
		if ( mirror != null ) {
			final AnnotationValue accessType = getAnnotationValue( mirror );
			if ( accessType != null ) {
				final VariableElement enumValue = (VariableElement) accessType.getValue();
				final Name enumValueName = enumValue.getSimpleName();
				if ( enumValueName.contentEquals(AccessType.PROPERTY.name()) ) {
					return AccessType.PROPERTY;
				}
				else if ( enumValueName.contentEquals(AccessType.FIELD.name()) ) {
					return AccessType.FIELD;
				}
			}
		}
		return null;
	}

	public static ElementKind getElementKindForAccessType(AccessType accessType) {
		return accessType == AccessType.FIELD ? ElementKind.FIELD : ElementKind.METHOD;
	}

	public static String getKeyType(DeclaredType type, Context context) {
		final List<? extends TypeMirror> typeArguments = type.getTypeArguments();
		if ( typeArguments.isEmpty() ) {
			context.logMessage( Diagnostic.Kind.ERROR, "Unable to determine type argument for " + type );
			return JAVA_OBJECT;
		}
		else {
			return extractClosestRealTypeAsString( typeArguments.get( 0 ), context );
		}
	}

	public static boolean isClassOrRecordType(Element element) {
		final ElementKind kind = element.getKind();
		// we want to accept classes and records but not enums,
		// and we want to avoid depending on ElementKind.RECORD
		return kind.isClass() && kind != ElementKind.ENUM;
	}

	public static boolean isClassRecordOrInterfaceType(Element element) {
		final ElementKind kind = element.getKind();
		// we want to accept classes and records but not enums,
		// and we want to avoid depending on ElementKind.RECORD
		return kind.isClass() && kind != ElementKind.ENUM
			|| kind.isInterface() && kind != ElementKind.ANNOTATION_TYPE;
	}

	public static boolean primitiveClassMatchesKind(Class<?> itemType, TypeKind kind) {
		return switch ( kind ) {
			case SHORT -> itemType.equals( Short.class );
			case INT -> itemType.equals( Integer.class );
			case LONG -> itemType.equals( Long.class );
			case BOOLEAN -> itemType.equals( Boolean.class );
			case FLOAT -> itemType.equals( Float.class );
			case DOUBLE -> itemType.equals( Double.class );
			case CHAR -> itemType.equals( Character.class );
			case BYTE -> itemType.equals( Byte.class );
			default -> false;
		};
	}

	public static boolean isPropertyGetter(ExecutableType executable, Element element) {
		return element.getKind() == ElementKind.METHOD
			&& isProperty( element.getSimpleName().toString(),
				toTypeString( executable.getReturnType() ) );
	}

	public static boolean isBasicAttribute(Element element, Element returnedElement, Context context) {
		return hasAnnotation( element, BASIC, ONE_TO_ONE, MANY_TO_ONE, EMBEDDED, EMBEDDED_ID, ID )
			|| hasAnnotation( element, "org.hibernate.annotations.Type") // METAGEN-28
			|| returnedElement.asType().accept( new BasicAttributeVisitor( context ), returnedElement );
	}

	public static @Nullable String getFullyQualifiedClassNameOfTargetEntity(
			AnnotationMirror mirror, String member) {
		final AnnotationValue value = getAnnotationValue( mirror, member);
		if ( value != null ) {
			final TypeMirror parameterType = (TypeMirror) value.getValue();
			if ( parameterType.getKind() != TypeKind.VOID ) {
				return parameterType.toString();
			}
		}
		return null;
	}

	/**
	 * @param annotations list of annotation mirrors.
	 *
	 * @return target entity class name as string or {@code null} if no targetEntity is here or if equals to void
	 */
	public static @Nullable String getTargetEntity(List<? extends AnnotationMirror> annotations) {
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
			else if ( isAnnotationMirrorOfType( mirror, "org.hibernate.annotations.TargetEmbeddable") ) {
				return getFullyQualifiedClassNameOfTargetEntity( mirror, "value" );
			}
		}
		return null;
	}

	public static String propertyName(Element element) {
		switch ( element.getKind() ) {
			case FIELD:
				return element.getSimpleName().toString();
			case METHOD:
				final Name name = element.getSimpleName();
				if ( name.length() > 3 && name.subSequence( 0, 3 ).equals( "get" ) ) {
					return decapitalize( name.subSequence( 3, name.length() ).toString() );
				}
				else if ( name.length() > 2 && name.subSequence( 0, 2 ).equals( "is" ) ) {
					return decapitalize( name.subSequence( 2, name.length() ).toString() );
				}
				else {
					return decapitalize( name.toString() );
				}
			default:
				return element.getSimpleName() + "/* " + element.getKind() + " */";
		}
	}

	public static @Nullable Element findMappedSuperElement(Metamodel entity, Context context) {
		final Element element = entity.getElement();
		if ( element instanceof TypeElement typeElement ) {
			TypeMirror superClass = typeElement.getSuperclass();
			//superclass of Object is of NoType which returns some other kind
			while ( superClass.getKind() == TypeKind.DECLARED ) {
				final DeclaredType declaredType = (DeclaredType) superClass;
				final TypeElement superClassElement = (TypeElement) declaredType.asElement();
				if ( extendsSuperMetaModel( superClassElement, entity.isMetaComplete(), context ) ) {
					return superClassElement;
				}
				superClass = superClassElement.getSuperclass();
			}
		}
		return null;
	}

	/**
	 * Checks whether this metamodel class needs to extend another metamodel class.
	 * This method checks whether the processor has generated a metamodel class for the super class, but it also
	 * allows for the possibility that the metamodel class was generated in a previous compilation. (It could be
	 * part of a separate jar. See also METAGEN-35.)
	 *
	 * @param superClassElement the super class element
	 * @param entityMetaComplete flag indicating if the entity for which the metamodel should be generated is
	 * metamodel complete. If so we cannot use reflection to decide whether we have to add the extends clause
	 * @param context the execution context
	 *
	 * @return {@code true} in case there is super class metamodel to extend from {@code false} otherwise.
	 */
	private static boolean extendsSuperMetaModel(Element superClassElement, boolean entityMetaComplete, Context context) {
		// if we processed the superclass in the same run we definitely need to extend
		final TypeElement typeElement = (TypeElement) superClassElement;
		final String superClassName = typeElement.getQualifiedName().toString();
		return context.containsMetaEntity( superClassName )
			|| context.containsMetaEmbeddable( superClassName )
			// to allow for the case that the metamodel class for the super entity is for example contained in another
			// jar file we use reflection. However, we need to consider the fact that there is xml configuration
			// and annotations should be ignored
			|| !entityMetaComplete && containsAnnotation( superClassElement, ENTITY, MAPPED_SUPERCLASS );
	}

	public static boolean implementsInterface(TypeElement type, String interfaceName) {
		for ( TypeMirror iface : type.getInterfaces() ) {
			if ( iface.getKind() == TypeKind.DECLARED ) {
				final DeclaredType declaredType = (DeclaredType) iface;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				if ( typeElement.getQualifiedName().contentEquals( interfaceName )
						|| implementsInterface( typeElement, interfaceName ) ) {
					return true;
				}
			}
		}
		TypeMirror superclass = type.getSuperclass();
		if ( superclass != null && superclass.getKind() == TypeKind.DECLARED  ) {
			final DeclaredType declaredType = (DeclaredType) superclass;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			if ( implementsInterface( typeElement, interfaceName) ) {
				return true;
			}
		}
		return false;
	}

	public static boolean extendsClass(TypeElement type, String className) {
		TypeMirror superclass = type.getSuperclass();
		while ( superclass != null && superclass.getKind() == TypeKind.DECLARED  ) {
			final DeclaredType declaredType = (DeclaredType) superclass;
			final TypeElement typeElement = (TypeElement) declaredType.asElement();
			if ( typeElement.getQualifiedName().contentEquals( className ) ) {
				return true;
			}
			superclass = typeElement.getSuperclass();
		}
		return false;
	}

	public static boolean isMemberType(Element element) {
		return element.getEnclosingElement() instanceof TypeElement;
	}

	public static String getGeneratedClassFullyQualifiedName(TypeElement typeElement, boolean jakartaDataStyle) {
		final String simpleName = typeElement.getSimpleName().toString();
		final Element enclosingElement = typeElement.getEnclosingElement();
		return qualifiedName( enclosingElement, jakartaDataStyle )
				+ "." + (jakartaDataStyle ? '_' + simpleName : simpleName + '_');
	}

	private static String qualifiedName(Element enclosingElement, boolean jakartaDataStyle) {
		if ( enclosingElement instanceof TypeElement typeElement ) {
			return getGeneratedClassFullyQualifiedName( typeElement, jakartaDataStyle );
		}
		else if ( enclosingElement instanceof PackageElement packageElement ) {
			return packageElement.getQualifiedName().toString();
		}
		else {
			throw new MetaModelGenerationException( "Unexpected enclosing element: " + enclosingElement );
		}
	}


	public static String getGeneratedClassFullyQualifiedName(TypeElement element, String packageName, boolean jakartaDataStyle) {
		final StringBuilder builder = new StringBuilder( packageName );
		final Name qualifiedName = element.getQualifiedName();
		final String tail = qualifiedName.subSequence( builder.length(), qualifiedName.length() ).toString();
		for ( String bit : split( ".", tail ) ) {
			final String part = removeDollar( bit );
			if ( !builder.isEmpty() ) {
				builder.append( "." );
			}
			builder.append( jakartaDataStyle ? '_' + part : part + '_' );
		}
		return builder.toString();
	}

	static class EmbeddedAttributeVisitor extends SimpleTypeVisitor8<@Nullable TypeElement, Element> {
		private final Context context;

		EmbeddedAttributeVisitor(Context context) {
			this.context = context;
		}

		@Override
		public @Nullable TypeElement visitDeclared(DeclaredType declaredType, Element element) {
			final TypeElement returnedElement = (TypeElement)
					context.getTypeUtils().asElement( declaredType );
			return containsAnnotation( castNonNull( returnedElement ), EMBEDDABLE ) ? returnedElement : null;
		}

		@Override
		public @Nullable TypeElement visitExecutable(ExecutableType executable, Element element) {
			if ( element.getKind().equals( ElementKind.METHOD ) ) {
				final String string = element.getSimpleName().toString();
				return isProperty( string, toTypeString( executable.getReturnType() ) )
						? executable.getReturnType().accept(this, element)
						: null;
			}
			else {
				return null;
			}
		}
	}

	public static boolean isPrimitive(String paramType) {
		return PRIMITIVE_TYPES.contains( paramType );
	}

	public static final Set<String> PRIMITIVE_TYPES =
			Set.of("boolean", "char", "long", "int", "short", "byte", "double", "float");

	public static String resolveTypeName(TypeElement typeElement, Element element, String name) {
		final var mirror = resolveTypeMirror( typeElement, element, name );
		return mirror == null ? name : mirror.toString();
	}

	public static @Nullable TypeMirror resolveTypeMirror(TypeElement typeElement, Element element, String name) {
		final var mirrorMap = resolveTypeParameters( typeElement.asType(), element, Map.of(), new HashSet<>() );
		return mirrorMap == null ? null : mirrorMap.get( name );
	}

	private static @Nullable Map<String, TypeMirror> resolveTypeParameters(TypeMirror type, Element element, Map<String, TypeMirror> parametersMap, Collection<Element> visited) {
		if ( !(type instanceof DeclaredType declaredType
			&& declaredType.asElement() instanceof TypeElement typeElement) ) {
			return null;
		}
		if ( !visited.add( typeElement ) ) {
			return null;
		}
		final var generic = typeElement.getTypeParameters();
		final var map = new HashMap<String, TypeMirror>();
		var typeArguments = declaredType.getTypeArguments();
		if ( !(typeArguments.isEmpty() || generic.size() == typeArguments.size()) ) {
			return null;
		}
		for ( var n = 0; n < generic.size(); ++n ) {
			final var mirror = typeArguments.isEmpty()
					? generic.get( 0 ).getBounds().get( 0 )
					: typeArguments.get( n );
			final var value = mirror.toString();
			map.put( generic.get( n ).asType().toString(), parametersMap.getOrDefault( value, mirror ) );
		}
		if ( typeElement.equals( element ) ) {
			return map;
		}
		return concat(
				Stream.of( typeElement.getSuperclass() ),
				typeElement.getInterfaces().stream()
		).map( tm -> resolveTypeParameters( tm, element, map, visited ) )
				.filter( Objects::nonNull )
				.findFirst().orElse( null );
	}
}
