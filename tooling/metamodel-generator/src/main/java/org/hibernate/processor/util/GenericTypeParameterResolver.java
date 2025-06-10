/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;

public class GenericTypeParameterResolver {

	private final Map<Element, Map<String, TypeMirror>> typeParameterMap;

	public GenericTypeParameterResolver(TypeElement element) {
		typeParameterMap = new HashMap<>();
		resolveTypeParameters( element, Map.of() );
	}

	public String resolveTypeName(Element el, String name) {
		final var mirror = resolveTypeMirror( el, name );
		return mirror == null ? name : mirror.toString();
	}

	public TypeMirror resolveTypeMirror(Element el, String name) {
		return typeParameterMap.getOrDefault( el, Map.of() ).get( name );
	}

	public void resolveTypeParameters(TypeElement element, Map<String, TypeMirror> map) {
		resolveElementTypeParameters( element.getSuperclass(), map );
		element.getInterfaces().forEach( iface -> resolveElementTypeParameters( iface, map ) );
	}

	private void resolveElementTypeParameters(TypeMirror type, Map<String, TypeMirror> parametersMap) {
		if ( type instanceof DeclaredType declaredType
				&& declaredType.asElement() instanceof TypeElement typeElement ) {
			final var generic = typeElement.getTypeParameters();
			final var map = new HashMap<String, TypeMirror>();
			var typeArguments = declaredType.getTypeArguments();
			if ( !( typeArguments.isEmpty() || generic.size() == typeArguments.size() ) ) {
				throw new IndexOutOfBoundsException(
						"Type %s : %d vs %d".formatted( type, generic.size(), typeArguments.size() ) );
			}
			for ( var n = 0; n < generic.size(); ++n ) {
				final var mirror = typeArguments.isEmpty()
						? generic.get( 0 ).getBounds().get( 0 )
						: typeArguments.get( n );
				final var value = mirror.toString();
				map.put( generic.get( n ).toString(), parametersMap.getOrDefault( value, mirror ) );
			}
			typeParameterMap.put( typeElement, map );
			resolveTypeParameters( typeElement, map );
		}
	}
}
