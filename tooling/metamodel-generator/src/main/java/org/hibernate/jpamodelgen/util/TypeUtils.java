/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
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
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.MetaModelGenerationException;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.jpamodelgen.util.NullnessUtil.castNonNull;
import static org.hibernate.jpamodelgen.util.StringUtil.isProperty;

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
		final TypeMirror superClass = element.getSuperclass();
		//superclass of Object is of NoType which returns some other kind
		if ( superClass.getKind() == TypeKind.DECLARED ) {
			//F..king Ch...t Have those people used their horrible APIs even once?
			final Element superClassElement = ( (DeclaredType) superClass ).asElement();
			return (TypeElement) superClassElement;
		}
		else {
			return null;
		}
	}

	public static String extractClosestRealTypeAsString(TypeMirror type, Context context) {
		final TypeMirror mirror = extractClosestRealType( type, context );
		return mirror == null ? "?" : mirror.toString();
	}

	private static @Nullable TypeMirror lowerBound(@Nullable TypeMirror bound) {
		return bound == null || bound.getKind() == TypeKind.NULL ? null : bound;
	}

	private static @Nullable TypeMirror upperBound(@Nullable TypeMirror bound) {
		return bound == null || (bound.getKind() == TypeKind.DECLARED && bound.toString().equals("java.lang.Object")) ? null : bound;
	}

	public static @Nullable TypeMirror extractClosestRealType(TypeMirror type, Context context) {
		if ( type == null ) {
			return null;
		}
		switch ( type.getKind() ) {
			case TYPEVAR:
				final TypeVariable typeVariable = (TypeVariable) type;
				return context.getTypeUtils().getWildcardType(
						upperBound( extractClosestRealType( typeVariable.getUpperBound(), context ) ),
						lowerBound( extractClosestRealType( typeVariable.getLowerBound(), context ) )
				);
			case WILDCARD:
				final WildcardType wildcardType = (WildcardType) type;
				return context.getTypeUtils().getWildcardType(
						extractClosestRealType( wildcardType.getExtendsBound(), context ),
						extractClosestRealType( wildcardType.getSuperBound(), context )
				);
			case DECLARED:
				final DeclaredType declaredType = (DeclaredType) type;
				final TypeElement typeElement = (TypeElement) declaredType.asElement();
				return context.getTypeUtils().getDeclaredType(
						typeElement,
						declaredType.getTypeArguments().stream()
								.map( new Function<TypeMirror, TypeMirror>() {
											@Override
											public @Nullable TypeMirror apply(TypeMirror arg) {
												return extractClosestRealType( arg, context );
											}
										} )
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
		return ((TypeElement) element).getQualifiedName().contentEquals( qualifiedName );
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

	public static boolean hasAnnotation(Element element, String qualifiedName) {
		return getAnnotationMirror( element, qualifiedName ) != null;
	}

	public static @Nullable Object getAnnotationValue(AnnotationMirror annotationMirror, String parameterValue) {
		assert annotationMirror != null;
		assert parameterValue != null;
		for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
				: annotationMirror.getElementValues().entrySet() ) {
			if ( entry.getKey().getSimpleName().contentEquals( parameterValue ) ) {
				return entry.getValue().getValue();
			}
		}
		return null;
	}

	public static @Nullable AnnotationValue getAnnotationValueRef(AnnotationMirror annotationMirror, String parameterValue) {
		assert annotationMirror != null;
		assert parameterValue != null;
		for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
				: annotationMirror.getElementValues().entrySet() ) {
			if ( entry.getKey().getSimpleName().contentEquals( parameterValue ) ) {
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
						//TODO: this default is arbitrary and very questionable!
						newDefaultAccessType = AccessType.PROPERTY;
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
			DeclaredType t, String fqNameOfReturnedType, @Nullable String explicitTargetEntityName, Context context) {
		if ( explicitTargetEntityName != null ) {
			return context.getElementUtils().getTypeElement( explicitTargetEntityName ).asType();
		}
		else {
			final List<? extends TypeMirror> typeArguments = t.getTypeArguments();
			if ( typeArguments.size() == 0 ) {
				throw new MetaModelGenerationException( "Unable to determine collection type" );
			}
			else if ( Map.class.getCanonicalName().equals( fqNameOfReturnedType ) ) {
				return t.getTypeArguments().get( 1 );
			}
			else {
				return t.getTypeArguments().get( 0 );
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
		final String embeddedClassName = member.asType().accept( new EmbeddedAttributeVisitor( context ), member );
		if ( embeddedClassName != null ) {
			final AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( embeddedClassName );
			if ( accessTypeInfo == null ) {
				final AccessTypeInformation newAccessTypeInfo =
						new AccessTypeInformation( embeddedClassName, null, defaultAccessType );
				context.addAccessTypeInformation( embeddedClassName, newAccessTypeInfo );
			}
			else {
				accessTypeInfo.setDefaultAccessType( defaultAccessType );
			}
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
				if ( containsAnnotation( superClass, Constants.ENTITY, Constants.MAPPED_SUPERCLASS ) ) {
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
				if ( containsAnnotation( superClass, Constants.MAPPED_SUPERCLASS ) ) {
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
		final ElementKind kind = element.getKind();
		if ( kind == ElementKind.FIELD || kind == ElementKind.METHOD ) {
			return kind == ElementKind.FIELD ? AccessType.FIELD : AccessType.PROPERTY;
		}
		else {
			return null;
		}
	}

	private static boolean isIdAnnotation(AnnotationMirror annotationMirror) {
		return isAnnotationMirrorOfType( annotationMirror, Constants.ID )
			|| isAnnotationMirrorOfType( annotationMirror, Constants.EMBEDDED_ID );
	}

	public static @Nullable AccessType determineAnnotationSpecifiedAccessType(Element element) {
		final AnnotationMirror mirror = getAnnotationMirror( element, Constants.ACCESS );
		if ( mirror != null ) {
			final Object accessType = getAnnotationValue( mirror, DEFAULT_ANNOTATION_PARAMETER_NAME );
			if ( accessType instanceof VariableElement) {
				final VariableElement enumValue = (VariableElement) accessType;
				if ( enumValue.getSimpleName().contentEquals( AccessType.PROPERTY.toString() ) ) {
					return AccessType.PROPERTY;
				}
				else if ( enumValue.getSimpleName().contentEquals( AccessType.FIELD.toString() ) ) {
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
			return "java.lang.Object";
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

	static class EmbeddedAttributeVisitor extends SimpleTypeVisitor8<@Nullable String, Element> {
		private final Context context;

		EmbeddedAttributeVisitor(Context context) {
			this.context = context;
		}

		@Override
		public @Nullable String visitDeclared(DeclaredType declaredType, Element element) {
			final TypeElement returnedElement = (TypeElement)
					context.getTypeUtils().asElement( declaredType );
			return containsAnnotation( returnedElement, Constants.EMBEDDABLE )
					? returnedElement.getQualifiedName().toString()
					: null;
		}

		@Override
		public @Nullable String visitExecutable(ExecutableType t, Element p) {
			if ( p.getKind().equals( ElementKind.METHOD ) ) {
				String string = p.getSimpleName().toString();
				return isProperty( string, toTypeString( t.getReturnType() ) )
						? t.getReturnType().accept(this, p)
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
}
