/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.MetaModelGenerationException;

/**
 * Utility class.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public final class TypeUtils {

	public static final String DEFAULT_ANNOTATION_PARAMETER_NAME = "value";
	private static final Map<TypeKind, String> PRIMITIVE_WRAPPERS = new HashMap<TypeKind, String>();
	private static final Map<TypeKind, String> PRIMITIVES = new HashMap<TypeKind, String>();

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
		if ( type.getKind().isPrimitive() ) {
			return PRIMITIVE_WRAPPERS.get( type.getKind() );
		}
		return type.toString();
	}

	public static String toArrayTypeString(ArrayType type, Context context) {
		TypeMirror componentType = type.getComponentType();
		if ( componentType.getKind().isPrimitive() ) {
			return PRIMITIVES.get( componentType.getKind() ) + "[]";
		}

		// When an ArrayType is annotated with an annotation which uses TYPE_USE targets,
		// we cannot simply take the TypeMirror returned by #getComponentType because it
		// itself is an AnnotatedType.
		//
		// The simplest approach here to get the TypeMirror for both ArrayType use cases
		// is to use the visitor to retrieve the underlying TypeMirror.
		TypeMirror component = componentType.accept(
				new SimpleTypeVisitor6<TypeMirror, Void>() {
					@Override
					protected TypeMirror defaultAction(TypeMirror e, Void aVoid) {
						return e;
					}
				},
				null
		);

		return extractClosestRealTypeAsString( component, context ) + "[]";
	}

	public static TypeElement getSuperclassTypeElement(TypeElement element) {
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
		if ( type instanceof TypeVariable ) {
			final TypeMirror compositeUpperBound = ( (TypeVariable) type ).getUpperBound();
			return extractClosestRealTypeAsString( compositeUpperBound, context );
		}
		else {
			final TypeMirror erasureType = context.getTypeUtils().erasure( type );
			if ( TypeKind.ARRAY.equals( erasureType.getKind() ) ) {
				// keep old behavior here for arrays since #asElement returns null for them.
				return erasureType.toString();
			}
			else {
				return ( (TypeElement) context.getTypeUtils().asElement( erasureType ) ).getQualifiedName().toString();
			}
		}
	}

	public static boolean containsAnnotation(Element element, String... annotations) {
		assert element != null;
		assert annotations != null;

		List<String> annotationClassNames = new ArrayList<String>();
		Collections.addAll( annotationClassNames, annotations );

		List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
		for ( AnnotationMirror mirror : annotationMirrors ) {
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
	 * @param fqcn the fully qualified class name to check against
	 *
	 * @return {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
	 */
	public static boolean isAnnotationMirrorOfType(AnnotationMirror annotationMirror, String fqcn) {
		assert annotationMirror != null;
		assert fqcn != null;
		String annotationClassName = annotationMirror.getAnnotationType().toString();

		return annotationClassName.equals( fqcn );
	}

	/**
	 * Checks whether the {@code Element} hosts the annotation with the given fully qualified class name.
	 *
	 * @param element the element to check for the hosted annotation
	 * @param fqcn the fully qualified class name of the annotation to check for
	 *
	 * @return the annotation mirror for the specified annotation class from the {@code Element} or {@code null} in case
	 *         the {@code TypeElement} does not host the specified annotation.
	 */
	public static AnnotationMirror getAnnotationMirror(Element element, String fqcn) {
		assert element != null;
		assert fqcn != null;

		AnnotationMirror mirror = null;
		for ( AnnotationMirror am : element.getAnnotationMirrors() ) {
			if ( isAnnotationMirrorOfType( am, fqcn ) ) {
				mirror = am;
				break;
			}
		}
		return mirror;
	}

	public static Object getAnnotationValue(AnnotationMirror annotationMirror, String parameterValue) {
		assert annotationMirror != null;
		assert parameterValue != null;

		Object returnValue = null;
		for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues()
				.entrySet() ) {
			if ( parameterValue.equals( entry.getKey().getSimpleName().toString() ) ) {
				returnValue = entry.getValue().getValue();
				break;
			}
		}
		return returnValue;
	}

	public static void determineAccessTypeForHierarchy(TypeElement searchedElement, Context context) {
		String fqcn = searchedElement.getQualifiedName().toString();
		context.logMessage( Diagnostic.Kind.OTHER, "Determining access type for " + fqcn );
		AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( fqcn );

		if ( accessTypeInfo != null && accessTypeInfo.isAccessTypeResolved() ) {
			context.logMessage(
					Diagnostic.Kind.OTHER,
					"AccessType for " + searchedElement.toString() + " found in cache: " + accessTypeInfo
			);
			return;
		}

		// check for explicit access type
		AccessType forcedAccessType = determineAnnotationSpecifiedAccessType( searchedElement );
		if ( forcedAccessType != null ) {
			context.logMessage(
					Diagnostic.Kind.OTHER, "Explicit access type on " + searchedElement + ":" + forcedAccessType
			);
			accessTypeInfo = new AccessTypeInformation( fqcn, forcedAccessType, null );
			context.addAccessTypeInformation( fqcn, accessTypeInfo );
			updateEmbeddableAccessType( searchedElement, context, forcedAccessType );
			return;
		}

		// need to find the default access type for this class
		// let's check first if this entity is the root of the class hierarchy and defines an id. If so the
		// placement of the id annotation determines the access type
		AccessType defaultAccessType = getAccessTypeInCaseElementIsRoot( searchedElement, context );
		if ( defaultAccessType != null ) {
			accessTypeInfo = new AccessTypeInformation( fqcn, null, defaultAccessType );
			context.addAccessTypeInformation( fqcn, accessTypeInfo );
			updateEmbeddableAccessType( searchedElement, context, defaultAccessType );
			setDefaultAccessTypeForMappedSuperclassesInHierarchy( searchedElement, defaultAccessType, context );
			return;
		}

		// if we end up here we need to recursively look for superclasses
		defaultAccessType = getDefaultAccessForHierarchy( searchedElement, context );
		if ( defaultAccessType == null ) {
			defaultAccessType = AccessType.PROPERTY;
		}
		accessTypeInfo = new AccessTypeInformation( fqcn, null, defaultAccessType );
		context.addAccessTypeInformation( fqcn, accessTypeInfo );
		updateEmbeddableAccessType( searchedElement, context, defaultAccessType );
	}

	public static TypeMirror getCollectionElementType(DeclaredType t, String fqNameOfReturnedType, String explicitTargetEntityName, Context context) {
		TypeMirror collectionElementType;
		if ( explicitTargetEntityName != null ) {
			Elements elements = context.getElementUtils();
			TypeElement element = elements.getTypeElement( explicitTargetEntityName );
			collectionElementType = element.asType();
		}
		else {
			List<? extends TypeMirror> typeArguments = t.getTypeArguments();
			if ( typeArguments.size() == 0 ) {
				throw new MetaModelGenerationException( "Unable to determine collection type" );
			}
			else if ( Map.class.getCanonicalName().equals( fqNameOfReturnedType ) ) {
				collectionElementType = t.getTypeArguments().get( 1 );
			}
			else {
				collectionElementType = t.getTypeArguments().get( 0 );
			}
		}
		return collectionElementType;
	}

	private static void updateEmbeddableAccessType(TypeElement element, Context context, AccessType defaultAccessType) {
		List<? extends Element> fieldsOfClass = ElementFilter.fieldsIn( element.getEnclosedElements() );
		for ( Element field : fieldsOfClass ) {
			updateEmbeddableAccessTypeForMember( context, defaultAccessType, field );
		}

		List<? extends Element> methodOfClass = ElementFilter.methodsIn( element.getEnclosedElements() );
		for ( Element method : methodOfClass ) {
			updateEmbeddableAccessTypeForMember( context, defaultAccessType, method );
		}
	}

	private static void updateEmbeddableAccessTypeForMember(Context context, AccessType defaultAccessType, Element member) {
		EmbeddedAttributeVisitor visitor = new EmbeddedAttributeVisitor( context );
		String embeddedClassName = member.asType().accept( visitor, member );
		if ( embeddedClassName != null ) {
			AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( embeddedClassName );
			if ( accessTypeInfo == null ) {
				accessTypeInfo = new AccessTypeInformation( embeddedClassName, null, defaultAccessType );
				context.addAccessTypeInformation( embeddedClassName, accessTypeInfo );
			}
			else {
				accessTypeInfo.setDefaultAccessType( defaultAccessType );
			}
		}
	}

	private static AccessType getDefaultAccessForHierarchy(TypeElement element, Context context) {
		AccessType defaultAccessType = null;
		TypeElement superClass = element;
		do {
			superClass = TypeUtils.getSuperclassTypeElement( superClass );
			if ( superClass != null ) {
				String fqcn = superClass.getQualifiedName().toString();
				AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( fqcn );
				if ( accessTypeInfo != null && accessTypeInfo.getDefaultAccessType() != null ) {
					return accessTypeInfo.getDefaultAccessType();
				}
				if ( TypeUtils.containsAnnotation( superClass, Constants.ENTITY, Constants.MAPPED_SUPERCLASS ) ) {
					defaultAccessType = getAccessTypeInCaseElementIsRoot( superClass, context );
					if ( defaultAccessType != null ) {
						accessTypeInfo = new AccessTypeInformation( fqcn, null, defaultAccessType );
						context.addAccessTypeInformation( fqcn, accessTypeInfo );

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
			superClass = TypeUtils.getSuperclassTypeElement( superClass );
			if ( superClass != null ) {
				String fqcn = superClass.getQualifiedName().toString();
				if ( TypeUtils.containsAnnotation( superClass, Constants.MAPPED_SUPERCLASS ) ) {
					AccessTypeInformation accessTypeInfo;
					AccessType forcedAccessType = determineAnnotationSpecifiedAccessType( superClass );
					if ( forcedAccessType != null ) {
						accessTypeInfo = new AccessTypeInformation( fqcn, null, forcedAccessType );
					}
					else {
						accessTypeInfo = new AccessTypeInformation( fqcn, null, defaultAccessType );
					}
					context.addAccessTypeInformation( fqcn, accessTypeInfo );
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
	private static AccessType getAccessTypeInCaseElementIsRoot(TypeElement searchedElement, Context context) {
		List<? extends Element> myMembers = searchedElement.getEnclosedElements();
		for ( Element subElement : myMembers ) {
			List<? extends AnnotationMirror> entityAnnotations =
					context.getElementUtils().getAllAnnotationMirrors( subElement );
			for ( Object entityAnnotation : entityAnnotations ) {
				AnnotationMirror annotationMirror = (AnnotationMirror) entityAnnotation;
				if ( isIdAnnotation( annotationMirror ) ) {
					return getAccessTypeOfIdAnnotation( subElement );
				}
			}
		}
		return null;
	}

	private static AccessType getAccessTypeOfIdAnnotation(Element element) {
		AccessType accessType = null;
		final ElementKind kind = element.getKind();
		if ( kind == ElementKind.FIELD || kind == ElementKind.METHOD ) {
			accessType = kind == ElementKind.FIELD ? AccessType.FIELD : AccessType.PROPERTY;
		}
		return accessType;
	}

	private static boolean isIdAnnotation(AnnotationMirror annotationMirror) {
		return TypeUtils.isAnnotationMirrorOfType( annotationMirror, Constants.ID )
				|| TypeUtils.isAnnotationMirrorOfType( annotationMirror, Constants.EMBEDDED_ID );
	}

	public static AccessType determineAnnotationSpecifiedAccessType(Element element) {
		final AnnotationMirror accessAnnotationMirror = TypeUtils.getAnnotationMirror( element, Constants.ACCESS );
		AccessType forcedAccessType = null;
		if ( accessAnnotationMirror != null ) {
			Element accessElement = (Element) TypeUtils.getAnnotationValue(
					accessAnnotationMirror,
					DEFAULT_ANNOTATION_PARAMETER_NAME
			);
			if ( accessElement.getKind().equals( ElementKind.ENUM_CONSTANT ) ) {
				if ( accessElement.getSimpleName().toString().equals( AccessType.PROPERTY.toString() ) ) {
					forcedAccessType = AccessType.PROPERTY;
				}
				else if ( accessElement.getSimpleName().toString().equals( AccessType.FIELD.toString() ) ) {
					forcedAccessType = AccessType.FIELD;
				}
			}
		}
		return forcedAccessType;
	}

	public static ElementKind getElementKindForAccessType(AccessType accessType) {
		if ( AccessType.FIELD.equals( accessType ) ) {
			return ElementKind.FIELD;
		}
		else {
			return ElementKind.METHOD;
		}
	}

	public static String getKeyType(DeclaredType t, Context context) {
		List<? extends TypeMirror> typeArguments = t.getTypeArguments();
		if ( typeArguments.size() == 0 ) {
			context.logMessage( Diagnostic.Kind.ERROR, "Unable to determine type argument for " + t );
		}
		return extractClosestRealTypeAsString( typeArguments.get( 0 ), context );
	}

	static class EmbeddedAttributeVisitor extends SimpleTypeVisitor6<String, Element> {
		private Context context;

		EmbeddedAttributeVisitor(Context context) {
			this.context = context;
		}

		@Override
		public String visitDeclared(DeclaredType declaredType, Element element) {
			TypeElement returnedElement = (TypeElement) context.getTypeUtils().asElement( declaredType );
			String fqNameOfReturnType = null;
			if ( containsAnnotation( returnedElement, Constants.EMBEDDABLE ) ) {
				fqNameOfReturnType = returnedElement.getQualifiedName().toString();
			}
			return fqNameOfReturnType;
		}

		@Override
		public String visitExecutable(ExecutableType t, Element p) {
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
	}
}
