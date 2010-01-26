// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jpamodelgen.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

import org.hibernate.jpamodelgen.Context;

/**
 * Utility class.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class TypeUtils {

	private static final Map<String, String> PRIMITIVES = new HashMap<String, String>();

	static {
		PRIMITIVES.put( "char", "Character" );

		PRIMITIVES.put( "byte", "Byte" );
		PRIMITIVES.put( "short", "Short" );
		PRIMITIVES.put( "int", "Integer" );
		PRIMITIVES.put( "long", "Long" );

		PRIMITIVES.put( "boolean", "Boolean" );

		PRIMITIVES.put( "float", "Float" );
		PRIMITIVES.put( "double", "Double" );

	}

	static public String toTypeString(TypeMirror type) {
		if ( type.getKind().isPrimitive() ) {
			return PRIMITIVES.get( type.toString() );
		}
		return type.toString();
	}

	static public TypeElement getSuperclassTypeElement(TypeElement element) {
		final TypeMirror superClass = element.getSuperclass();
		//superclass of Object is of NoType which returns some other kind
		if ( superClass.getKind() == TypeKind.DECLARED ) {
			//F..king Ch...t Have those people used their horrible APIs even once?
			final Element superClassElement = ( ( DeclaredType ) superClass ).asElement();
			return ( TypeElement ) superClassElement;
		}
		else {
			return null;
		}
	}

	public static String extractClosestRealTypeAsString(TypeMirror type, Context context) {
		if ( type instanceof TypeVariable ) {
			final TypeMirror compositeUpperBound = ( ( TypeVariable ) type ).getUpperBound();
			final Types types = context.getProcessingEnvironment().getTypeUtils();
			final List<? extends TypeMirror> upperBounds = types.directSupertypes( compositeUpperBound );
			if ( upperBounds.size() == 0 ) {
				return compositeUpperBound.toString();
			}
			else {
				//take the first one
				return extractClosestRealTypeAsString( upperBounds.get( 0 ), context );
			}
		}
		else {
			return type.toString();
		}
	}

	public static boolean containsAnnotation(Element element, Class<?>... annotations) {
		assert element != null;
		assert annotations != null;

		List<String> annotationClassNames = new ArrayList<String>();
		for ( Class<?> clazz : annotations ) {
			annotationClassNames.add( clazz.getName() );
		}

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
	 * This method uses the string class names for comparison. See also {@link http://www.retep.org/2009/02/getting-class-values-from-annotations.html}.
	 *
	 * @param annotationMirror The annotation mirror
	 * @param clazz the class name to check again
	 *
	 * @return {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
	 */
	public static boolean isAnnotationMirrorOfType(AnnotationMirror annotationMirror, Class<? extends Annotation> clazz) {
		assert annotationMirror != null;
		assert clazz != null;
		String annotationClassName = annotationMirror.getAnnotationType().toString();
		String className = clazz.getName();

		return annotationClassName.equals( className );
	}

	public static boolean isTypeElementOfType(TypeElement element, Class<?> clazz) {
		assert element != null;
		assert clazz != null;
		String elementClassName = element.getQualifiedName().toString();
		String className = clazz.getName();

		return elementClassName.equals( className );
	}

	/**
	 * Returns the annotation mirror for the specified annotation class from the {@code Element}.
	 *
	 * @param element the element to check for the hosted annotation
	 * @param clazz the annotation class to check for
	 *
	 * @return the annotation mirror for the specified annotation class from the {@code Element} or {@code null} in case
	 *         the {@code TypeElement} does not host the specified annotation.
	 */
	public static AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> clazz) {
		assert element != null;
		assert clazz != null;

		AnnotationMirror mirror = null;
		for ( AnnotationMirror am : element.getAnnotationMirrors() ) {
			if ( isAnnotationMirrorOfType( am, clazz ) ) {
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
}
