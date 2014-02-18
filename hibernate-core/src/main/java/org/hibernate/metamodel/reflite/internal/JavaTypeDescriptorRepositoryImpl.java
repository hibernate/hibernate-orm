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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.logging.Logger;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;

/**
 * This is the "interim" implementation of JavaTypeDescriptorRepository that loads Classes to ascertain this
 * information.  Ultimately the goal is to hand this responsibility off to Jandex once
 * (if) it exposes these capabilities.
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorRepositoryImpl implements JavaTypeDescriptorRepository {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRepositoryImpl.class );

	private final ClassLoader jpaTempClassLoader;
	private final ClassLoaderService classLoaderService;

	private final IndexView jandexIndex;

	private final TypeResolver classmateTypeResolver;
	private final MemberResolver classmateMemberResolver;

	private Map<Name,JavaTypeDescriptor> typeDescriptorMap = new HashMap<Name, JavaTypeDescriptor>();

	public JavaTypeDescriptorRepositoryImpl(
			IndexView jandexIndex,
			ClassLoader jpaTempClassLoader,
			ServiceRegistry serviceRegistry) {
		this( jandexIndex, jpaTempClassLoader, serviceRegistry.getService( ClassLoaderService.class ) );
	}

	public JavaTypeDescriptorRepositoryImpl(
			IndexView jandexIndex,
			ClassLoader jpaTempClassLoader,
			ClassLoaderService classLoaderService) {
		this.jandexIndex = jandexIndex;
		this.jpaTempClassLoader = jpaTempClassLoader;
		this.classLoaderService = classLoaderService;

		this.classmateTypeResolver = new TypeResolver();
		this.classmateMemberResolver = new MemberResolver( classmateTypeResolver );
	}

	@Override
	public Name buildName(String name) {
		return new DotNameAdapter( name );
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
		// classes in any of these packages are safe to load through the "live" ClassLoader
		return classNameString.startsWith( "java." )
				|| classNameString.startsWith( "javax." )
				|| classNameString.startsWith( "org.hibernate" );

	}

	private JavaTypeDescriptor makeTypeDescriptor(Name typeName, Class clazz) {
		final JandexPivot jandexPivot = pivotAnnotations( toJandexName( typeName ) );

		if ( clazz.isInterface() ) {
			final InterfaceDescriptorImpl typeDescriptor = new InterfaceDescriptorImpl(
					typeName,
					clazz.getModifiers(),
					jandexPivot.typeAnnotations
			);
			typeDescriptorMap.put( typeName, typeDescriptor );

			typeDescriptor.setExtendedInterfaceTypes( extractInterfaces( clazz ) );

			final ResolvedType resolvedType = classmateTypeResolver.resolve( clazz );
			typeDescriptor.setTypeParameters( extractTypeParameters( resolvedType ) );
//			final ResolvedTypeWithMembers resolvedTypeWithMembers = classmateMemberResolver.resolve( resolvedType, null, null );

			typeDescriptor.setFields( extractFields( clazz, typeDescriptor, jandexPivot ) );
			typeDescriptor.setMethods( extractMethods( clazz, typeDescriptor, jandexPivot ) );


			return typeDescriptor;
		}
		else {
			final ClassDescriptorImpl typeDescriptor = new ClassDescriptorImpl(
					typeName,
					clazz.getModifiers(),
					hasDefaultCtor( clazz ),
					jandexPivot.typeAnnotations
			);
			typeDescriptorMap.put( typeName, typeDescriptor );

			typeDescriptor.setSuperType( extractSuper( clazz ) );
			typeDescriptor.setInterfaces( extractInterfaces( clazz ) );

			final ResolvedType resolvedType = classmateTypeResolver.resolve( clazz );
			typeDescriptor.setTypeParameters( extractTypeParameters( resolvedType ) );

			typeDescriptor.setFields( extractFields( clazz, typeDescriptor, jandexPivot ) );
			typeDescriptor.setMethods( extractMethods( clazz, typeDescriptor, jandexPivot ) );

			return typeDescriptor;
		}
	}

	private List<JavaTypeDescriptor> extractTypeParameters(ResolvedType resolvedType) {
		if ( resolvedType.getTypeParameters().isEmpty() ) {
			return Collections.emptyList();
		}

		final List<JavaTypeDescriptor> result = CollectionHelper.arrayList( resolvedType.getTypeParameters().size() );
		for ( ResolvedType typeParameter : resolvedType.getTypeParameters() ) {
			result.add( getType( buildName( typeParameter.getErasedSignature() ) ) );
		}
		return result;
	}

	private DotName toJandexName(Name typeName) {
		if ( DotNameAdapter.class.isInstance( typeName ) ) {
			return ( (DotNameAdapter) typeName ).jandexName();
		}
		else {
			return DotName.createSimple( typeName.fullName() );
		}
	}

	private static final JandexPivot NO_JANDEX_PIVOT = new JandexPivot();

	private JandexPivot pivotAnnotations(DotName typeName) {
		if ( jandexIndex == null ) {
			return NO_JANDEX_PIVOT;
		}

		final ClassInfo jandexClassInfo = jandexIndex.getClassByName( typeName );
		if ( jandexClassInfo == null ) {
			return NO_JANDEX_PIVOT;
		}

		final Map<DotName, List<AnnotationInstance>> annotations = jandexClassInfo.annotations();
		final JandexPivot pivot = new JandexPivot();
		for ( Map.Entry<DotName, List<AnnotationInstance>> annotationInstances : annotations.entrySet() ) {
			for ( AnnotationInstance annotationInstance : annotationInstances.getValue() ) {
				if ( MethodParameterInfo.class.isInstance( annotationInstance.target() ) ) {
					continue;
				}

				if ( FieldInfo.class.isInstance( annotationInstance.target() ) ) {
					final FieldInfo fieldInfo = (FieldInfo) annotationInstance.target();
					Map<DotName,AnnotationInstance> fieldAnnotations = pivot.fieldAnnotations.get( fieldInfo.name() );
					if ( fieldAnnotations == null ) {
						fieldAnnotations = new HashMap<DotName, AnnotationInstance>();
						pivot.fieldAnnotations.put( fieldInfo.name(), fieldAnnotations );
						fieldAnnotations.put( annotationInstance.name(), annotationInstance );
					}
					else {
						final Object oldEntry = fieldAnnotations.put( annotationInstance.name(), annotationInstance );
						if ( oldEntry != null ) {
							log.debugf(
									"Encountered duplicate annotation [%s] on field [%s]",
									annotationInstance.name(),
									fieldInfo.name()
							);
						}
					}
				}
				else if ( MethodInfo.class.isInstance( annotationInstance.target() ) ) {
					final MethodInfo methodInfo = (MethodInfo) annotationInstance.target();
					final String methodKey = buildBuildKey( methodInfo );
					Map<DotName,AnnotationInstance> methodAnnotations = pivot.methodAnnotations.get( methodKey );
					if ( methodAnnotations == null ) {
						methodAnnotations = new HashMap<DotName, AnnotationInstance>();
						pivot.methodAnnotations.put( methodKey, methodAnnotations );
						methodAnnotations.put( annotationInstance.name(), annotationInstance );
					}
					else {
						final Object oldEntry = methodAnnotations.put( annotationInstance.name(), annotationInstance );
						if ( oldEntry != null ) {
							log.debugf(
									"Encountered duplicate annotation [%s] on method [%s -> %s]",
									annotationInstance.name(),
									jandexClassInfo.name(),
									methodKey
							);
						}
					}
				}
				else if ( ClassInfo.class.isInstance( annotationInstance.target() ) ) {
					// todo : validate its the type we are processing?
					final Object oldEntry = pivot.typeAnnotations.put( annotationInstance.name(), annotationInstance );
					if ( oldEntry != null ) {
						log.debugf(
								"Encountered duplicate annotation [%s] on type [%s]",
								annotationInstance.name(),
								jandexClassInfo.name()
						);
					}
				}
			}
		}

		return pivot;
	}

	private String buildBuildKey(MethodInfo methodInfo) {
		final StringBuilder buff = new StringBuilder();
		buff.append( methodInfo.returnType().toString() )
				.append( ' ' )
				.append( methodInfo.name() )
				.append( '(' );
		for ( int i = 0; i < methodInfo.args().length; i++ ) {
			if ( i > 0 ) {
				buff.append( ',' );
			}
			buff.append( methodInfo.args()[i].toString() );
		}

		return buff.append( ')' ).toString();
	}

	private Collection<AnnotationInstance> getAnnotations(DotName dotName) {
		return jandexIndex.getAnnotations( dotName );
	}

	private ClassDescriptor extractSuper(Class clazz) {
		final Class superclass = clazz.getSuperclass();
		if ( superclass == null ) {
			return null;
		}

		return (ClassDescriptor) getType( buildName( superclass.getName() ) );
	}

	private Collection<InterfaceDescriptor> extractInterfaces(Class clazz) {
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

	private Collection<FieldDescriptor> extractFields(
			Class clazz,
			JavaTypeDescriptor declaringType,
			JandexPivot jandexPivot) {
		final Field[] declaredFields = clazz.getDeclaredFields();
		final Field[] fields = clazz.getFields();

		if ( declaredFields.length <= 0 && fields.length <= 0 ) {
			return Collections.emptyList();
		}

		final List<FieldDescriptor> fieldDescriptors = CollectionHelper.arrayList( fields.length );

		for ( Field field : declaredFields ) {
			fieldDescriptors.add(
					new FieldDescriptorImpl(
							field.getName(),
							toTypeDescriptor( field.getType() ),
							field.getModifiers(),
							declaringType,
							jandexPivot.fieldAnnotations.get( field.getName() )
					)
			);
		}

		for ( Field field : fields ) {
			if ( clazz.equals( field.getDeclaringClass() ) ) {
				continue;
			}

			final JavaTypeDescriptor fieldDeclarer = getType( buildName( field.getDeclaringClass().getName() ) );
			fieldDescriptors.add(
					new FieldDescriptorImpl(
							field.getName(),
							toTypeDescriptor( field.getType() ),
							field.getModifiers(),
							fieldDeclarer,
							jandexPivot.fieldAnnotations.get( field.getName() )
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

	private Collection<MethodDescriptor> extractMethods(
			Class clazz,
			JavaTypeDescriptor declaringType,
			JandexPivot jandexPivot) {
		final Method[] declaredMethods = clazz.getDeclaredMethods();
		final Method[] methods = clazz.getMethods();

		if ( declaredMethods.length <= 0 && methods.length <= 0 ) {
			return Collections.emptyList();
		}

		final List<MethodDescriptor> methodDescriptors = CollectionHelper.arrayList( methods.length );

		for ( Method method : declaredMethods ) {
			methodDescriptors.add( fromMethod( method, declaringType, jandexPivot ) );
		}

		for ( Method method : methods ) {
			if ( clazz.equals( method.getDeclaringClass() ) ) {
				continue;
			}

			final JavaTypeDescriptor methodDeclarer = getType( buildName( method.getDeclaringClass().getName() ) );
			methodDescriptors.add( fromMethod( method, methodDeclarer, jandexPivot ) );
		}

		return methodDescriptors;
	}

	private MethodDescriptor fromMethod(
			Method method,
			JavaTypeDescriptor declaringType,
			JandexPivot jandexPivot) {
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

		return new MethodDescriptorImpl(
				method.getName(),
				declaringType,
				method.getModifiers(),
				toTypeDescriptor( method.getReturnType() ),
				argumentTypes,
				jandexPivot.methodAnnotations.get( buildMethodAnnotationsKey( method ) )
		);
	}

	private String buildMethodAnnotationsKey(Method method) {
		StringBuilder buff = new StringBuilder();
		buff.append( method.getReturnType().getName() )
				.append( method.getName() )
				.append( '(' );
		for ( int i = 0; i < method.getParameterTypes().length; i++ ) {
			if ( i > 0 ) {
				buff.append( ',' );
			}
			buff.append( method.getParameterTypes()[i].getName() );
		}
		return buff.append( ')' ).toString();
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

	private static class JandexPivot {
		private Map<DotName,AnnotationInstance> typeAnnotations
				= new HashMap<DotName, AnnotationInstance>();
		private Map<String,Map<DotName,AnnotationInstance>> fieldAnnotations
				= new HashMap<String, Map<DotName, AnnotationInstance>>();
		private Map<String,Map<DotName,AnnotationInstance>> methodAnnotations
				= new HashMap<String, Map<DotName, AnnotationInstance>>();
	}

}
