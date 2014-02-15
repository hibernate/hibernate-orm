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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.InterfaceDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptorRepository;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.Name;
import org.hibernate.metamodel.reflite.spi.VoidDescriptor;
import org.hibernate.service.ServiceRegistry;

import org.jboss.logging.Logger;

/**
 * This is the "interim" implementation of JavaTypeDescriptorRepository that loads Classes to ascertain this
 * information.  Ultimately the goal is to hand this responsibility off to Jandex once
 * (if) it exposes these capabilities.
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorRepositoryImpl implements JavaTypeDescriptorRepository {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRepositoryImpl.class );

	private ClassLoader jpaTempClassLoader;
	private final ClassLoaderService classLoaderService;

	private Map<Name,JavaTypeDescriptor> typeDescriptorMap = new HashMap<Name, JavaTypeDescriptor>();

	public JavaTypeDescriptorRepositoryImpl(ClassLoader jpaTempClassLoader, ServiceRegistry serviceRegistry) {
		this( jpaTempClassLoader, serviceRegistry.getService( ClassLoaderService.class ) );
	}

	public JavaTypeDescriptorRepositoryImpl(ClassLoader jpaTempClassLoader, ClassLoaderService classLoaderService) {
		this.jpaTempClassLoader = jpaTempClassLoader;
		this.classLoaderService = classLoaderService;
	}

	@Override
	public Name buildName(String name) {
		return new DotNameAdapter( name );
	}

	private JavaTypeDescriptor arrayOfType(Class type) {
		if ( type.isArray() ) {
			return new ArrayDescriptorImpl(
					buildName( "[" + type.getName() ),
					type.getModifiers(),
					arrayOfType( type.getComponentType() )
			);
		}

		if ( type.isPrimitive() ) {
			return Primitives.primitiveArrayDescriptor( type );
		}
		else {
			return new ArrayDescriptorImpl(
					buildName( "[" + type.getName() ),
					type.getModifiers(),
					getType( buildName( type.getName() ) )
			);
		}
	}

	@Override
	public JavaTypeDescriptor getType(Name typeName) {
		if ( typeName == null ) {
			return null;
		}

		final String typeNameString = typeName.fullName();
		if ( "void".equals( typeNameString ) ) {
			return VoidDescriptor.INSTANCE;
		}

		JavaTypeDescriptor descriptor = typeDescriptorMap.get( typeName );
		if ( descriptor == null ) {
			descriptor = Primitives.resolveByName( typeName );
		}

		if ( descriptor == null ) {
			descriptor = makeTypeDescriptor( typeName );
			typeDescriptorMap.put( typeName, descriptor );
		}

		return descriptor;
	}

	protected JavaTypeDescriptor makeTypeDescriptor(Name typeName) {
		final String classNameToLoad = typeName.fullName();

		if ( isSafeClass( typeName ) ) {
			return makeTypeDescriptor( typeName, classLoaderService.classForName( classNameToLoad ) );
		}
		else {
			if ( jpaTempClassLoader == null ) {
				log.debug(
						"Request to makeTypeDescriptor(%s) - but passed class is not known to be " +
								"safe (it might be, it might not be). However, there was no " +
								"temp ClassLoader provided; we will use the live ClassLoader"
				);
				try {
					// this reference is "safe" because it was loaded from the live ClassLoader
					return makeTypeDescriptor( typeName, classLoaderService.classForName( classNameToLoad ) );
				}
				catch (ClassLoadingException e) {
					return new NoSuchClassTypeDescriptor( typeName );
				}
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
					unSafeReference = jpaTempClassLoader.loadClass( classNameToLoad );
				}
				catch (ClassNotFoundException e) {
					return new NoSuchClassTypeDescriptor( typeName );
				}

				return makeTypeDescriptor( typeName, unSafeReference );
			}
		}
	}

	private boolean isSafeClass(Name className) {
		final String classNameString = className.fullName();
		return classNameString.startsWith( "java." )
				|| classNameString.startsWith( "javax." )
				|| classNameString.startsWith( "org.hibernate" );

	}



	private JavaTypeDescriptor makeTypeDescriptor(Name typeName, Class clazz) {
		if ( clazz.isInterface() ) {
			final InterfaceDescriptorImpl typeDescriptor = new InterfaceDescriptorImpl( typeName, clazz.getModifiers() );
			typeDescriptorMap.put( typeName, typeDescriptor );

			typeDescriptor.setExtendedInterfaceTypes( fromInterfaces( clazz ) );
			typeDescriptor.setDeclaredFields( fromFields( clazz, typeDescriptor ) );
			typeDescriptor.setDeclaredMethods( fromMethods( clazz, typeDescriptor ) );

			return typeDescriptor;
		}
		else {
			final ClassDescriptorImpl typeDescriptor = new ClassDescriptorImpl(
					typeName,
					clazz.getModifiers(),
					hasDefaultCtor( clazz )
			);
			typeDescriptorMap.put( typeName, typeDescriptor );

			typeDescriptor.setSuperType( fromSuper( clazz ) );
			typeDescriptor.setInterfaces( fromInterfaces( clazz ) );
			typeDescriptor.setFields( fromFields( clazz, typeDescriptor ) );
			typeDescriptor.setMethods( fromMethods( clazz, typeDescriptor ) );

			return typeDescriptor;
		}
	}

	private ClassDescriptor fromSuper(Class clazz) {
		final Class superclass = clazz.getSuperclass();
		if ( superclass == null ) {
			return null;
		}

		return (ClassDescriptor) getType( buildName( superclass.getName() ) );
	}

	private Collection<InterfaceDescriptor> fromInterfaces(Class clazz) {
		final Class[] interfaces = clazz.getInterfaces();
		if ( interfaces == null || interfaces.length <= 0 ) {
			return Collections.emptyList();
		}

		final Collection<InterfaceDescriptor> interfaceTypes = CollectionHelper.arrayList( interfaces.length );
		for ( Class anInterface : interfaces ) {
			interfaceTypes.add( (InterfaceDescriptor) getType( buildName( anInterface.getName() ) ) );
		}
		return interfaceTypes;
	}

	private Collection<FieldDescriptor> fromFields(Class clazz, JavaTypeDescriptor declaringType) {
		final Field[] fields = clazz.getDeclaredFields();
		if ( fields == null || fields.length <= 0 ) {
			return Collections.emptyList();
		}

		final List<FieldDescriptor> fieldDescriptors = CollectionHelper.arrayList( fields.length );
		for ( Field field : fields ) {
			final Class fieldType = field.getType();
			fieldDescriptors.add(
					new FieldDescriptorImpl(
							field.getName(),
							toTypeDescriptor( fieldType ),
							field.getModifiers(),
							declaringType
					)
			);
		}
		return fieldDescriptors;
	}

	private JavaTypeDescriptor toTypeDescriptor(Class clazz) {
		final JavaTypeDescriptor fieldTypeDescriptor;
		if ( clazz.isArray() ) {
			fieldTypeDescriptor = arrayOfType( clazz.getComponentType() );
		}
		else {
			fieldTypeDescriptor = getType( buildName( clazz.getName() ) );
		}
		return fieldTypeDescriptor;
	}

	private Collection<MethodDescriptor> fromMethods(Class clazz, JavaTypeDescriptor declaringType) {
		final Method[] methods = clazz.getDeclaredMethods();
		if ( methods == null || methods.length <= 0 ) {
			return Collections.emptyList();
		}

		final List<MethodDescriptor> methodDescriptors = CollectionHelper.arrayList( methods.length );
		for ( Method method : methods ) {
			final Class[] parameterTypes = method.getParameterTypes();
			final Collection<JavaTypeDescriptor> argumentTypes;
			if ( parameterTypes.length == 0 ) {
				argumentTypes = Collections.emptyList();
			}
			else {
				argumentTypes = CollectionHelper.arrayList( parameterTypes.length );
				for ( Class parameterType : parameterTypes ) {
					argumentTypes.add( toTypeDescriptor( parameterType ) );
				}
			}
			methodDescriptors.add(
					new MethodDescriptorImpl(
							method.getName(),
							declaringType,
							method.getModifiers(),
							toTypeDescriptor( method.getReturnType() ),
							argumentTypes
					)
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
		JavaTypeDescriptorRepositoryImpl repo = new JavaTypeDescriptorRepositoryImpl(
				JavaTypeDescriptorRepositoryImpl.class.getClassLoader(),
				new BootstrapServiceRegistryBuilder().build()
		);

		JavaTypeDescriptor td = repo.getType( repo.buildName( JavaTypeDescriptorRepositoryImpl.class.getName() ) );
		assert ClassDescriptorImpl.class.isInstance( td );
	}

	private static class NoSuchClassTypeDescriptor implements JavaTypeDescriptor {
		private final Name name;

		private NoSuchClassTypeDescriptor(Name name) {
			this.name = name;
		}

		@Override
		public Name getName() {
			return name;
		}

		@Override
		public int getModifiers() {
			return 0;
		}

		@Override
		public Collection<FieldDescriptor> getDeclaredFields() {
			return Collections.emptyList();
		}

		@Override
		public Collection<MethodDescriptor> getDeclaredMethods() {
			return Collections.emptyList();
		}
	}
}
