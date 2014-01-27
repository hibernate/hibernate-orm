/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.internal.source.annotations.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;

import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class ClassmateHelper {
	/**
	 * @param clazz
	 * @param contextResolveType
	 * @param bindingContext
	 *
	 * @return
	 */
	public static Map<String, ResolvedTypeWithMembers> resolveClassHierarchyTypesFromAttributeType(
			final Class<?> clazz,
			final ResolvedType contextResolveType,
			final AnnotationBindingContext bindingContext) {
		if ( clazz == null ) {
			return Collections.emptyMap();
		}
		final Map<String, ResolvedTypeWithMembers> resolvedTypes = new HashMap<String, ResolvedTypeWithMembers>();
		ResolvedType resolvedType = resolveClassType( clazz, contextResolveType, bindingContext );
//		String className = clazz.getName();
		if ( resolvedType != null ) {
			//move this out of loop, since it returns all members from the hierarchy
			final ResolvedTypeWithMembers resolvedTypeWithMembers = bindingContext.getMemberResolver()
					.resolve( resolvedType, null, null );
			Class<?> currentClass = clazz;
			while ( currentClass != null && !Object.class.equals( currentClass ) ) {
				resolvedTypes.put( currentClass.getName(), resolvedTypeWithMembers );
				currentClass = currentClass.getSuperclass();

			}

//			while ( resolvedType != null && !Object.class.equals( resolvedType.getErasedType() ) ) {
//				resolvedTypes.put( className, resolvedTypeWithMembers );
//				resolvedType = resolvedType.getParentClass();
//				if ( resolvedType != null ) {
//					className = resolvedType.getErasedType().getName();
//				}
//			}
		}


		return resolvedTypes;
	}

	private static ResolvedType resolveClassType(
			final Class<?> clazz,
			final ResolvedType contextResolveType,
			final AnnotationBindingContext bindingContext) {
		if ( contextResolveType == null || contextResolveType.isPrimitive() || clazz.isPrimitive() || clazz.getTypeParameters().length == 0 ) {
			return bindingContext.getTypeResolver().resolve( clazz );
		}
		else if ( contextResolveType.canCreateSubtype( clazz ) ) {
			return bindingContext.getTypeResolver()
					.resolve(
							clazz,
							contextResolveType.getTypeParameters()
									.toArray( new ResolvedType[contextResolveType.getTypeBindings().size()] )
					);
		}
		else if ( contextResolveType.isArray() ) {
			return resolveClassType( clazz, contextResolveType.getArrayElementType(), bindingContext );
		}
		else if ( contextResolveType.isInstanceOf( Collection.class ) || contextResolveType.isInstanceOf( Map.class ) ) {
			return resolveClassType( clazz, contextResolveType.getTypeParameters(), bindingContext );
		}
		return null;
	}

	private static ResolvedType resolveClassType(
			final Class<?> clazz,
			final List<ResolvedType> contextResolveTypes,
			final AnnotationBindingContext bindingContext) {
		if ( contextResolveTypes != null ) {
			for ( ResolvedType contextResolveType : contextResolveTypes ) {
				ResolvedType type = resolveClassType( clazz, contextResolveType, bindingContext );
				if ( type != null ) {
					return type;
				}
			}
		}
		return null;

	}
}
