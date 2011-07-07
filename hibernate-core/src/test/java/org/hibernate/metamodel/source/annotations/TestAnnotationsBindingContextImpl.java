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

import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.binder.source.MappingDefaults;
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.annotations.AnnotationsBindingContext;
import org.hibernate.metamodel.domain.JavaType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Steve Ebersole
 */
public class TestAnnotationsBindingContextImpl implements AnnotationsBindingContext {
	private Index index;
	private ServiceRegistry serviceRegistry;

	private NamingStrategy namingStrategy = EJB3NamingStrategy.INSTANCE;

	private final TypeResolver typeResolver = new TypeResolver();
	private final Map<Class<?>, ResolvedType> resolvedTypeCache = new HashMap<Class<?>, ResolvedType>();

	public TestAnnotationsBindingContextImpl(Index index, ServiceRegistry serviceRegistry) {
		this.index = index;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public Index getIndex() {
		return index;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		throw new NotYetImplementedException();
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		throw new NotYetImplementedException();
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return serviceRegistry.getService( ClassLoaderService.class ).classForName( name );
	}

	@Override
	public JavaType makeJavaType(String className) {
		throw new NotYetImplementedException();
	}

	@Override
	public Value<Class<?>> makeClassReference(String className) {
		throw new NotYetImplementedException();
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
		// todo : is there a reason we create this resolver every time?
		MemberResolver memberResolver = new MemberResolver( typeResolver );
		return memberResolver.resolve( type, null, null );
	}

	@Override
	public boolean isGloballyQuotedIdentifiers() {
		return false;
	}
}
