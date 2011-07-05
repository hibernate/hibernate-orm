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
package org.hibernate.metamodel.source.annotations.entity;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.binder.source.MappingDefaults;
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.annotations.AnnotationsBindingContext;
import org.hibernate.metamodel.domain.JavaType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.internal.BasicServiceRegistryImpl;

/**
* @author Steve Ebersole
*/
public class AnnotationsBindingContextImpl implements AnnotationsBindingContext {
	private final Index index;
	private final BasicServiceRegistryImpl serviceRegistry;

	private final TypeResolver typeResolver = new TypeResolver();
	private final Map<Class<?>, ResolvedType> resolvedTypeCache = new HashMap<Class<?>, ResolvedType>();

	public AnnotationsBindingContextImpl(Index index, BasicServiceRegistryImpl serviceRegistry) {
		this.index = index;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public Index getIndex() {
		return index;
	}

	@Override
	public ClassInfo getClassInfo(String name) {
		DotName dotName = DotName.createSimple( name );
		return index.getClassByName( dotName );
	}

	@Override
	public void resolveAllTypes(String className) {
		// the resolved type for the top level class in the hierarchy
		Class<?> clazz = locateClassByName( className );
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

	@Override
	public ResolvedType getResolvedType(Class<?> clazz) {
		return resolvedTypeCache.get( clazz );
	}

	@Override
	public ResolvedTypeWithMembers resolveMemberTypes(ResolvedType type) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return EJB3NamingStrategy.INSTANCE;
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return null;
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return null;
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return serviceRegistry.getService( ClassLoaderService.class ).classForName( name );
	}

	@Override
	public JavaType makeJavaType(String className) {
		return null;
	}

	@Override
	public boolean isGloballyQuotedIdentifiers() {
		return false;
	}
}
