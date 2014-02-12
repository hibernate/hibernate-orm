/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.reflite.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.Name;
import org.hibernate.metamodel.reflite.spi.Repository;
import org.hibernate.metamodel.reflite.spi.TypeDescriptor;
import org.hibernate.service.ServiceRegistry;

import org.jboss.logging.Logger;

/**
 * This is the "interim" implementation of Repository that loads Classes to ascertain this
 * information.  Ultimately the goal is to hand this responsibility off to Jandex once
 * (if) it exposes these capabilities.
 *
 * @author Steve Ebersole
 */
public class RepositoryImpl implements Repository {
	private static final Logger log = Logger.getLogger( RepositoryImpl.class );

	private ClassLoader jpaTempClassLoader;
	private final ClassLoaderService classLoaderService;

	private Map<Name,TypeDescriptor> typeDescriptorMap = new HashMap<Name, TypeDescriptor>();

	public RepositoryImpl(ClassLoader jpaTempClassLoader, ServiceRegistry serviceRegistry) {
		this.jpaTempClassLoader = jpaTempClassLoader;
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	@Override
	public Name buildName(String name) {
		return new DotNameAdapter( name );
	}

	@Override
	public TypeDescriptor getType(Name className) {
		TypeDescriptor descriptor = typeDescriptorMap.get( className );
		if ( descriptor == null ) {
			descriptor = makeTypeDescriptor( className );
		}
		return descriptor;
	}

	protected TypeDescriptor makeTypeDescriptor(Name typeName) {
		if ( isSafeClass( typeName ) ) {
			return makeTypeDescriptor( typeName, classLoaderService.classForName( typeName.toString() ) );
		}
		else {
			if ( jpaTempClassLoader == null ) {
				log.debug(
						"Request to makeTypeDescriptor(%s) - but passed class is not known to be " +
								"safe (it might be, it might not be). However, there was no " +
								"temp ClassLoader provided; we will use the live ClassLoader"
				);
				// this reference is "safe" because it was loaded from the live ClassLoader
				return makeTypeDescriptor( typeName, classLoaderService.classForName( typeName.toString() ) );
			}
			else {
				log.debug(
						"Request to makeTypeDescriptor(%s) - passed class is not known to be " +
								"safe (it might be, it might not be). There was a temp ClassLoader " +
								"provided, so we will use that"
				);
				// this is the Class reference that is unsafe to keep around...
				final Class unSafeReference;
				try {
					unSafeReference = jpaTempClassLoader.loadClass( typeName.toString() );
				}
				catch (ClassNotFoundException e) {
					throw new ClassLoadingException(
							"Unable to load class " + typeName.toString() + " using temp ClassLoader",
							e
					);
				}
				return makeTypeDescriptor( typeName, unSafeReference );
			}
		}
	}

	private boolean isSafeClass(Name className) {
		final String classNameString = className.toString();
		return classNameString.startsWith( "java." )
				|| classNameString.startsWith( "javax." )
				|| classNameString.startsWith( "org.hibernate" );

	}

	private static TypeDescriptor[] NO_TYPES = new TypeDescriptor[0];
	private static FieldDescriptor[] NO_FIELDS = new FieldDescriptor[0];
	private static MethodDescriptor[] NO_METHODS = new MethodDescriptor[0];


	// todo : this is not circular reference safe in terms of fields/methods referring back to this type

	private TypeDescriptor makeTypeDescriptor(Name typeName, Class clazz) {
		// we build and register it now to protect against circular references
		final TypeDescriptorImpl typeDescriptor = new TypeDescriptorImpl(
				typeName,
				clazz.isInterface(),
				hasDefaultCtor( clazz )
		);
		typeDescriptorMap.put( typeName, typeDescriptor );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// super type
		TypeDescriptor superType = null;
		if ( !clazz.isInterface() ) {
			final Class superclass = clazz.getSuperclass();
			if ( superclass != null ) {
				superType = getType( buildName( superclass.getName() ) );
			}
		}
		typeDescriptor.setSuperType( superType );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// interfaces
		TypeDescriptor[] interfaceTypes = null;
		final Class[] interfaces = clazz.getInterfaces();
		if ( interfaces != null && interfaces.length > 0 ) {
			interfaceTypes = new TypeDescriptor[ interfaces.length ];
			for ( int i = 0; i < interfaces.length; i++ ) {
				interfaceTypes[i] = getType( buildName( interfaces[i].getName() ) );
			}
		}
		typeDescriptor.setInterfaces( interfaceTypes == null ? NO_TYPES : interfaceTypes );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// fields
		FieldDescriptor[] fieldDescriptors = null;
		final Field[] fields = clazz.getDeclaredFields();
		if ( fields != null && fields.length > 0 ) {
			fieldDescriptors = new FieldDescriptor[ fields.length ];
			for ( int i = 0; i < fields.length; i++ ) {
				fieldDescriptors[i] = new FieldDescriptorImpl(
						fields[i].getName(),
						getType( buildName( fields[i].getType().getName() ) ),
						typeDescriptor
				);
			}
		}
		typeDescriptor.setFields( fieldDescriptors == null ? NO_FIELDS : fieldDescriptors );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// methods
		MethodDescriptor[] methodDescriptors = null;
		final Method[] methods = clazz.getDeclaredMethods();
		if ( methods != null && methods.length > 0 ) {
			methodDescriptors = new MethodDescriptor[ methods.length ];
			for ( int i = 0; i < methods.length; i++ ) {
				final Class[] parameterTypes = methods[i].getParameterTypes();
				final TypeDescriptor[] argumentTypes;
				if ( parameterTypes.length == 0 ) {
					argumentTypes = NO_TYPES;
				}
				else {
					argumentTypes = new TypeDescriptor[ parameterTypes.length ];
					for ( int x = 0; x < parameterTypes.length; x++ ) {
						argumentTypes[x] = getType( buildName( parameterTypes[x].getName() ) );
					}
				}
				methodDescriptors[i] = new MethodDescriptorImpl(
						methods[i].getName(),
						typeDescriptor,
						getType( buildName( methods[i].getReturnType().getName() ) ),
						argumentTypes
				);
			}
		}
		typeDescriptor.setMethods( methodDescriptors == null ? NO_METHODS : methodDescriptors );

		return typeDescriptor;
	}


	@SuppressWarnings("unchecked")
	private static boolean hasDefaultCtor(Class clazz) {
		try {
			return !clazz.isInterface() && clazz.getConstructor() != null;
		}
		catch (NoSuchMethodException ignore) {
			return false;
		}
	}

}
