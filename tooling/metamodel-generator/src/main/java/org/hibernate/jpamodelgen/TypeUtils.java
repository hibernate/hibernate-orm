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
package org.hibernate.jpamodelgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

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
		if(type.getKind().isPrimitive()) {
			return PRIMITIVES.get(type.toString());
		}
	
		return type.toString();
	}

	static public TypeElement getSuperclass(TypeElement element) {
		final TypeMirror superClass = element.getSuperclass();
		//superclass of Object is of NoType which returns some other kind
		String superclassDeclaration = "";
		if (superClass.getKind() == TypeKind.DECLARED ) {
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
			if (upperBounds.size() == 0) {
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
}
