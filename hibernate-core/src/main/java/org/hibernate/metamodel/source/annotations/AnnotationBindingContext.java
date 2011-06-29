/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Helper class for keeping some context information needed during the processing of mapped classes.
 *
 * @author Hardy Ferentschik
 */
public class AnnotationBindingContext {
	private final TypeResolver typeResolver;
	private final ServiceRegistry serviceRegistry;
	private final Index index;
	private final Map<Class<?>, ResolvedType> resolvedTypeCache;

	private ClassLoaderService classLoaderService;

	public AnnotationBindingContext(Index index, ServiceRegistry serviceRegistry) {
		this.index = index;
		this.serviceRegistry = serviceRegistry;
		this.typeResolver = new TypeResolver();
		this.resolvedTypeCache = new HashMap<Class<?>, ResolvedType>();
	}

	public Index getIndex() {
		return index;
	}

	public ClassLoaderService classLoaderService() {
		if ( classLoaderService == null ) {
			classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		}
		return classLoaderService;
	}

	public ClassInfo getClassInfo(String className) {
		DotName dotName = DotName.createSimple( className );
		return index.getClassByName( dotName );
	}

	public Class<?> loadClass(String className) {
		return classLoaderService.classForName( className );
	}

	public void resolveAllTypes(String className) {
		// the resolved type for the top level class in the hierarchy
		Class<?> clazz = classLoaderService().classForName( className );
		ResolvedType resolvedType = typeResolver.resolve( clazz );
		while ( resolvedType != null ) {
			// todo - check whether there is already something in the map
			resolvedTypeCache.put( clazz, resolvedType );
			resolvedType = resolvedType.getParentClass();
			if ( resolvedType != null ) {
				clazz = resolvedType.getErasedType();
			}
		}
	}

	public ResolvedType getResolvedType(Class<?> clazz) {
		// todo - error handling
		return resolvedTypeCache.get( clazz );
	}

	public ResolvedTypeWithMembers resolveMemberTypes(ResolvedType type) {
		MemberResolver memberResolver = new MemberResolver( typeResolver );
		return memberResolver.resolve( type, null, null );
	}
}


