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

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
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

	private TypeDescriptor arrayOfType(Class type) {
		if ( type.isArray() ) {
			return new ArrayTypeDescriptorImpl(
					buildName( "[" + type.getName() ),
					arrayOfType( type.getComponentType() )
			);
		}

		return new ArrayTypeDescriptorImpl(
				buildName( "[" + type.getName() ),
				getType( buildName( type.getName() ) )
		);
	}

	@Override
	public TypeDescriptor getType(Name typeName) {
		if ( typeName == null ) {
			return null;
		}

		final String typeNameString = typeName.toString();
		if ( "void".equals( typeNameString ) ) {
			return VoidTypeDescriptor.INSTANCE;
		}

		TypeDescriptor descriptor = typeDescriptorMap.get( typeName );
		if ( descriptor == null ) {
			descriptor = Primitives.resolveByName( typeName );
		}

		if ( descriptor == null ) {
			descriptor = makeTypeDescriptor( typeName );
			typeDescriptorMap.put( typeName, descriptor );
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



	private TypeDescriptor makeTypeDescriptor(Name typeName, Class clazz) {
		if ( clazz.isInterface() ) {
			final InterfaceDescriptorImpl typeDescriptor = new InterfaceDescriptorImpl( typeName );
			typeDescriptorMap.put( typeName, typeDescriptor );

			typeDescriptor.setExtendedInterfaceTypes( fromInterfaces( clazz ) );
			typeDescriptor.setDeclaredFields( fromFields( clazz, typeDescriptor ) );
			typeDescriptor.setDeclaredMethods( fromMethods( clazz, typeDescriptor ) );

			return typeDescriptor;
		}
		else {
			final ClassDescriptorImpl typeDescriptor = new ClassDescriptorImpl( typeName, hasDefaultCtor( clazz ) );
			typeDescriptorMap.put( typeName, typeDescriptor );

			typeDescriptor.setSuperType( fromSuper( clazz ) );
			typeDescriptor.setInterfaces( fromInterfaces( clazz ) );
			typeDescriptor.setFields( fromFields( clazz, typeDescriptor ) );
			typeDescriptor.setMethods( fromMethods( clazz, typeDescriptor ) );

			return typeDescriptor;
		}
	}

	private TypeDescriptor fromSuper(Class clazz) {
		final Class superclass = clazz.getSuperclass();
		if ( superclass == null ) {
			return null;
		}

		return getType( buildName( superclass.getName() ) );
	}

	private static TypeDescriptor[] NO_TYPES = new TypeDescriptor[0];

	private TypeDescriptor[] fromInterfaces(Class clazz) {
		final Class[] interfaces = clazz.getInterfaces();
		if ( interfaces == null || interfaces.length <= 0 ) {
			return NO_TYPES;
		}

		final TypeDescriptor[] interfaceTypes = new TypeDescriptor[ interfaces.length ];
		for ( int i = 0; i < interfaces.length; i++ ) {
			interfaceTypes[i] = getType( buildName( interfaces[i].getName() ) );
		}
		return interfaceTypes;
	}

	private static FieldDescriptor[] NO_FIELDS = new FieldDescriptor[0];

	private FieldDescriptor[] fromFields(Class clazz, TypeDescriptor declaringType) {
		final Field[] fields = clazz.getDeclaredFields();
		if ( fields == null || fields.length <= 0 ) {
			return NO_FIELDS;
		}

		FieldDescriptor[] fieldDescriptors = new FieldDescriptor[ fields.length ];
		for ( int i = 0; i < fields.length; i++ ) {
			final Class fieldType =  fields[i].getType();
			fieldDescriptors[i] = new FieldDescriptorImpl(
					fields[i].getName(),
					toTypeDescriptor( fieldType ),
					declaringType
			);
		}
		return fieldDescriptors;
	}

	private TypeDescriptor toTypeDescriptor(Class clazz) {
		final TypeDescriptor fieldTypeDescriptor;
		if ( clazz.isArray() ) {
			fieldTypeDescriptor = arrayOfType( clazz.getComponentType() );
		}
		else {
			fieldTypeDescriptor = getType( buildName( clazz.getName() ) );
		}
		return fieldTypeDescriptor;
	}

	private static MethodDescriptor[] NO_METHODS = new MethodDescriptor[0];

	private MethodDescriptor[] fromMethods(Class clazz, TypeDescriptor declaringType) {
		final Method[] methods = clazz.getDeclaredMethods();
		if ( methods == null || methods.length <= 0 ) {
			return NO_METHODS;
		}

		MethodDescriptor[] methodDescriptors = new MethodDescriptor[ methods.length ];
		for ( int i = 0; i < methods.length; i++ ) {
			final Class[] parameterTypes = methods[i].getParameterTypes();
			final TypeDescriptor[] argumentTypes;
			if ( parameterTypes.length == 0 ) {
				argumentTypes = NO_TYPES;
			}
			else {
				argumentTypes = new TypeDescriptor[ parameterTypes.length ];
				for ( int x = 0; x < parameterTypes.length; x++ ) {
					argumentTypes[x] = toTypeDescriptor( parameterTypes[x] );
				}
			}
			methodDescriptors[i] = new MethodDescriptorImpl(
					methods[i].getName(),
					declaringType,
					toTypeDescriptor( methods[i].getReturnType() ),
					argumentTypes
			);
		}
		return methodDescriptors;
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

	public static void main(String... args) {
		RepositoryImpl repo = new RepositoryImpl(
				RepositoryImpl.class.getClassLoader(),
				new BootstrapServiceRegistryBuilder().build()
		);

		TypeDescriptor td = repo.getType( repo.buildName( RepositoryImpl.class.getName() ) );
		assert !td.isInterface();
	}

	private static class VoidTypeDescriptor implements TypeDescriptor {
		/**
		 * Singleton access
		 */
		public static final VoidTypeDescriptor INSTANCE = new VoidTypeDescriptor();

		private final Name name = new DotNameAdapter( "void" );

		@Override
		public Name getName() {
			return name;
		}

		@Override
		public boolean isInterface() {
			return false;
		}

		@Override
		public boolean isVoid() {
			return true;
		}

		@Override
		public boolean isArray() {
			return false;
		}

		@Override
		public boolean isPrimitive() {
			return false;
		}
	}

}
