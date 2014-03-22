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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.reflite.spi.ArrayDescriptor;
import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.DynamicTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.InterfaceDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptorRepository;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.ParameterizedType;
import org.hibernate.metamodel.reflite.spi.PrimitiveTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.VoidDescriptor;
import org.hibernate.metamodel.source.internal.annotations.JandexAccessImpl;
import org.hibernate.metamodel.spi.ClassLoaderAccess;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.logging.Logger;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.classmate.members.ResolvedMethod;

/**
 * This is the "interim" implementation of JavaTypeDescriptorRepository that loads Classes to ascertain this
 * information.  Ultimately the goal is to hand this responsibility off to Jandex once
 * (if) it exposes these capabilities.
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorRepositoryImpl implements JavaTypeDescriptorRepository {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRepositoryImpl.class );

	private final JandexAccessImpl jandexAccess;
	private final ClassLoaderAccess classLoaderAccess;

	private final TypeResolver classmateTypeResolver;
	private final MemberResolver classmateMemberResolver;

	private Map<DotName,JavaTypeDescriptor> typeDescriptorMap = new HashMap<DotName, JavaTypeDescriptor>();

	private final InterfaceDescriptor jdkCollectionDescriptor;
	private final InterfaceDescriptor jdkListDescriptor;
	private final InterfaceDescriptor jdkSetDescriptor;
	private final InterfaceDescriptor jdkMapDescriptor;

	public JavaTypeDescriptorRepositoryImpl(
			JandexAccessImpl jandexAccess,
			ClassLoaderAccess classLoaderAccess) {
		this.jandexAccess = jandexAccess;
		this.classLoaderAccess = classLoaderAccess;

		this.classmateTypeResolver = new TypeResolver();
		this.classmateMemberResolver = new MemberResolver( classmateTypeResolver );

		this.jdkCollectionDescriptor = (InterfaceDescriptor) getType( DotName.createSimple( Collection.class.getName() ) );
		this.jdkListDescriptor = (InterfaceDescriptor) getType( DotName.createSimple( List.class.getName() ) );
		this.jdkSetDescriptor = (InterfaceDescriptor) getType( DotName.createSimple( Set.class.getName() ) );
		this.jdkMapDescriptor = (InterfaceDescriptor) getType( DotName.createSimple( Map.class.getName() ) );
	}

	@Override
	public DotName buildName(String name) {
		return DotName.createSimple( name );
	}

	@Override
	public DynamicTypeDescriptor makeDynamicType(DotName typeName, DynamicTypeDescriptor superType) {
		final JavaTypeDescriptor existingRegistration = typeDescriptorMap.get( typeName );
		if ( existingRegistration != null ) {
			if ( !DynamicTypeDescriptor.class.isInstance( existingRegistration ) ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ENGLISH,
								"Found existing type descriptor for given type name [%s], " +
										"but it was not a dynamic type : %s",
								typeName,
								existingRegistration
						)
				);
			}

			final DynamicTypeDescriptor existingDynamicTypeDescriptor = (DynamicTypeDescriptor) existingRegistration;
			if ( existingDynamicTypeDescriptor.getSuperType() != null ) {
				if ( !existingDynamicTypeDescriptor.getSuperType().equals( superType ) ) {
					throw new IllegalArgumentException(
							String.format(
									Locale.ENGLISH,
									"Found existing type descriptor for given type name [%s], " +
											"but it had mismatched super-type; expecting : %s, found : %s",
									typeName,
									superType,
									existingDynamicTypeDescriptor.getSuperType()
							)
					);
				}
			}
			else {
				( (DynamicTypeDescriptorImpl) existingDynamicTypeDescriptor ).setSuperType( (DynamicTypeDescriptorImpl) superType );
			}

			return existingDynamicTypeDescriptor;
		}

		DynamicTypeDescriptor type = new DynamicTypeDescriptorImpl( typeName, (DynamicTypeDescriptorImpl) superType );
		typeDescriptorMap.put( typeName, type );
		return type;
	}

	@Override
	public JavaTypeDescriptor getType(DotName typeName) {
		final String typeNameString = typeName.toString();
		if ( "void".equals( typeNameString ) ) {
			return VoidDescriptor.INSTANCE;
		}

		if ( typeNameString.startsWith( "[" ) ) {
			return decipherArrayTypeRequest( typeNameString );
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

	private JavaTypeDescriptor decipherArrayTypeRequest(String typeNameString) {
		final String componentTypeNameString = typeNameString.substring( 1 );
		final JavaTypeDescriptor componentType = resolveArrayComponent( componentTypeNameString );
		if ( componentType == null ) {
			throw new IllegalArgumentException(
					"Could not interpret requested array component to descriptor : " + typeNameString
			);
		}

		return new ArrayDescriptorImpl(
				DotName.createSimple( typeNameString ),
				Modifier.PUBLIC,
				componentType
		);
	}

	private JavaTypeDescriptor resolveArrayComponent(String componentTypeName) {
		if ( componentTypeName.startsWith( "[" ) ) {
			// the component itself is an array (multiple dimension array)
			return decipherArrayTypeRequest( componentTypeName );
		}

		final char arrayComponentCode = componentTypeName.charAt( 0 );
		if ( arrayComponentCode == 'L' ) {
			// the array component is an object.. we need to strip
			// off the leading 'L' and the trailing ';' and resolve
			// the component descriptor
			final String componentClassName = componentTypeName.substring( 1, componentTypeName.length() - 1 );
			return getType( DotName.createSimple( componentClassName ) );
		}
		else {
			// should indicate we have a primitive array
			// ask the Primitives class to help us decipher it
			return Primitives.decipherArrayComponentCode( arrayComponentCode );
		}
	}

	@Override
	public ArrayDescriptor arrayType(JavaTypeDescriptor componentType) {
		if ( PrimitiveTypeDescriptor.class.isInstance( componentType ) ) {
			return Primitives.primitiveArrayDescriptor( (PrimitiveTypeDescriptor) componentType );
		}

		final String componentTypeName = componentType.getName().toString();
		final String arrayTypeName;
		if ( ArrayDescriptor.class.isInstance( componentType ) ) {
			arrayTypeName = '[' + componentTypeName;
		}
		else {
			arrayTypeName = "[L" + componentTypeName + ';';
		}
		return new ArrayDescriptorImpl(
				DotName.createSimple( arrayTypeName ),
				Modifier.PUBLIC,
				componentType
		);
	}

	@Override
	public InterfaceDescriptor jdkCollectionDescriptor() {
		return jdkCollectionDescriptor;
	}

	@Override
	public InterfaceDescriptor jdkListDescriptor() {
		return jdkListDescriptor;
	}

	@Override
	public InterfaceDescriptor jdkSetDescriptor() {
		return jdkSetDescriptor;
	}

	@Override
	public InterfaceDescriptor jdkMapDescriptor() {
		return jdkMapDescriptor;
	}

	protected JavaTypeDescriptor makeTypeDescriptor(DotName typeName) {
		final String classNameToLoad = typeName.toString();
		try {
			return makeTypeDescriptor( typeName, classLoaderAccess.classForName( classNameToLoad ) );
		}
		catch (ClassLoadingException e) {
			// assume a dynamic type
			log.debugf( "Creating an implicit DynamicTypeDescriptor : %s", typeName );
			return new DynamicTypeDescriptorImpl( typeName, null );
		}
	}

	private JavaTypeDescriptor makeTypeDescriptor(DotName typeName, Class clazz) {
		final JandexPivot jandexPivot = pivotAnnotations( typeName );

		if ( clazz.isInterface() ) {
			final InterfaceDescriptorImpl typeDescriptor = new InterfaceDescriptorImpl(
					jandexPivot.classInfo,
					clazz.getModifiers(),
					jandexPivot.typeAnnotations,
					jandexPivot.allAnnotations
			);
			typeDescriptorMap.put( typeName, typeDescriptor );

			typeDescriptor.setExtendedInterfaceTypes( extractInterfaces( clazz ) );

			final ResolvedType resolvedType = classmateTypeResolver.resolve( clazz );
			typeDescriptor.setTypeParameters( extractTypeParameters( resolvedType ) );
			final ResolvedTypeWithMembers resolvedTypeWithMembers = classmateMemberResolver.resolve( resolvedType, null, null );

			//interface fields would need to be static, and we don't care about static fields
			//we exclude the statics in extractFields but why even bother iterating them as here
			//we will exclude them all
			//typeDescriptor.setFields( extractFields( clazz, typeDescriptor, jandexPivot, resolvedTypeWithMembers ) );
			typeDescriptor.setMethods( extractMethods( clazz, typeDescriptor, jandexPivot, resolvedTypeWithMembers ) );


			return typeDescriptor;
		}
		else {
			final ClassDescriptorImpl typeDescriptor = new ClassDescriptorImpl(
					jandexPivot.classInfo,
					clazz.getModifiers(),
					hasDefaultCtor( clazz ),
					jandexPivot.typeAnnotations,
					jandexPivot.allAnnotations
			);
			typeDescriptorMap.put( typeName, typeDescriptor );

			typeDescriptor.setSuperType( extractSuper( clazz ) );
			typeDescriptor.setInterfaces( extractInterfaces( clazz ) );

			if ( !Object.class.equals( clazz ) ) {
				final ResolvedType resolvedType = classmateTypeResolver.resolve( clazz );
				typeDescriptor.setTypeParameters( extractTypeParameters( resolvedType ) );
				final ResolvedTypeWithMembers resolvedTypeWithMembers = classmateMemberResolver.resolve( resolvedType, null, null );

				typeDescriptor.setFields( extractFields( clazz, typeDescriptor, jandexPivot, resolvedTypeWithMembers ) );
				typeDescriptor.setMethods( extractMethods( clazz, typeDescriptor, jandexPivot, resolvedTypeWithMembers ) );
			}

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

	private static final JandexPivot NO_JANDEX_PIVOT = new JandexPivot( null );

	private JandexPivot pivotAnnotations(DotName typeName) {
		if ( jandexAccess.getIndex() == null ) {
			return NO_JANDEX_PIVOT;
		}

		final ClassInfo jandexClassInfo = jandexAccess.getIndex().getClassByName( typeName );
		if ( jandexClassInfo == null ) {
			return new JandexPivot(
					ClassInfo.create(
							typeName,
							null,
							(short)0,
							new DotName[0],
							Collections.<DotName, List<AnnotationInstance>>emptyMap()
					)
			);
		}

		final Map<DotName, List<AnnotationInstance>> annotations = jandexClassInfo.annotations();
		final JandexPivot pivot = new JandexPivot( jandexClassInfo );
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
			JandexPivot jandexPivot,
			ResolvedTypeWithMembers resolvedTypeWithMembers) {
		final Field[] declaredFields = clazz.getDeclaredFields();
		final Field[] fields = clazz.getFields();

		if ( declaredFields.length <= 0 && fields.length <= 0 ) {
			return Collections.emptyList();
		}

		final ResolvedField[] resolvedFields = resolvedTypeWithMembers.getMemberFields();
		final List<FieldDescriptor> fieldDescriptors = CollectionHelper.arrayList( fields.length );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// first,  extract declared fields
		for ( Field field : declaredFields ) {
			if ( Modifier.isStatic( field.getModifiers() ) ) {
				continue;
			}
			fieldDescriptors.add(
					new FieldDescriptorImpl(
							field.getName(),
							makeParameterizedType(
									toTypeDescriptor( field.getType() ),
									findResolvedFieldInfo( field, resolvedFields )
							),
							field.getModifiers(),
							declaringType,
							jandexPivot.fieldAnnotations.get( field.getName() )
					)
			);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// then, extract all fields
//		for ( Field field : fields ) {
//			if ( clazz.equals( field.getDeclaringClass() ) ) {
//				// skip declared fields, since we already processed them above
//				continue;
//			}
//
//			if ( Modifier.isStatic( field.getModifiers() ) ) {
//				continue;
//			}
//
//			final JavaTypeDescriptor fieldDeclarer = getType( buildName( field.getDeclaringClass().getName() ) );
//			fieldDescriptors.add(
//					new FieldDescriptorImpl(
//							field.getName(),
//							makeParameterizedType(
//									toTypeDescriptor( field.getType() ),
//									findResolvedFieldInfo( field, resolvedFields )
//							),
//							field.getModifiers(),
//							fieldDeclarer,
//							jandexPivot.fieldAnnotations.get( field.getName() )
//					)
//			);
//		}
		return fieldDescriptors;
	}

	private ResolvedField findResolvedFieldInfo(Field field, ResolvedField[] resolvedFields) {
		for ( ResolvedField resolvedField : resolvedFields ) {
			if ( resolvedField.getName().equals( field.getName() ) ) {
				return resolvedField;
			}
		}
//		throw new AssertionFailure(
//				String.format(
//						"Unable to resolve type information of field : %s",
//						field.toGenericString()
//				)
//		);
		return null;
	}

	private ParameterizedType makeParameterizedType(
			JavaTypeDescriptor erasedTypeDescriptor,
			ResolvedMember resolvedMember) {
		if ( resolvedMember == null
				|| resolvedMember.getType() == null ) {
			return new ParameterizedTypeImpl(
					erasedTypeDescriptor,
					Collections.<JavaTypeDescriptor>emptyList()
			);
		}

		List<ResolvedType> classmateResolvedTypes = resolvedMember.getType().getTypeParameters();
		if ( classmateResolvedTypes.isEmpty() ) {
			return new ParameterizedTypeImpl( erasedTypeDescriptor, Collections.<JavaTypeDescriptor>emptyList() );
		}

		List<JavaTypeDescriptor> resolvedTypeDescriptors = new ArrayList<JavaTypeDescriptor>();
		for ( ResolvedType classmateResolvedType : classmateResolvedTypes ) {
			resolvedTypeDescriptors.add(
					toTypeDescriptor( classmateResolvedType.getErasedType() )
			);
		}
		return new ParameterizedTypeImpl( erasedTypeDescriptor, resolvedTypeDescriptors );
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
					buildName( "[L" + type.getName() + ";" ),
					type.getModifiers(),
					getType( buildName( type.getName() ) )
			);
		}
	}

	private Collection<MethodDescriptor> extractMethods(
			Class clazz,
			JavaTypeDescriptor declaringType,
			JandexPivot jandexPivot,
			ResolvedTypeWithMembers resolvedTypeWithMembers) {
		final Method[] declaredMethods = clazz.getDeclaredMethods();
		final Method[] methods = clazz.getMethods();

		if ( declaredMethods.length <= 0 && methods.length <= 0 ) {
			return Collections.emptyList();
		}

		final ResolvedMethod[] resolvedMethods = resolvedTypeWithMembers.getMemberMethods();
		final List<MethodDescriptor> methodDescriptors = CollectionHelper.arrayList( methods.length );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// first,  extract declared methods
		for ( Method method : declaredMethods ) {
			if ( Modifier.isStatic( method.getModifiers() ) ) {
				continue;
			}
			methodDescriptors.add( fromMethod( method, declaringType, jandexPivot, resolvedMethods ) );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// then, extract all methods
//		for ( Method method : methods ) {
//			if ( clazz.equals( method.getDeclaringClass() ) ) {
//				// skip declared methods, since we already processed them above
//				continue;
//			}
//			if ( Modifier.isStatic( method.getModifiers() ) ) {
//				continue;
//			}
//
//			final JavaTypeDescriptor methodDeclarer = getType( buildName( method.getDeclaringClass().getName() ) );
//			methodDescriptors.add( fromMethod( method, methodDeclarer, jandexPivot, resolvedMethods ) );
//		}

		return methodDescriptors;
	}

	private MethodDescriptor fromMethod(
			Method method,
			JavaTypeDescriptor declaringType,
			JandexPivot jandexPivot,
			ResolvedMethod[] resolvedMethods) {
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
				makeParameterizedType(
						toTypeDescriptor( method.getReturnType() ),
						findResolvedMethodInfo( method, resolvedMethods )
				),
				argumentTypes,
				jandexPivot.methodAnnotations.get( buildMethodAnnotationsKey( method ) )
		);
	}

	private ResolvedMethod findResolvedMethodInfo(Method method, ResolvedMethod[] resolvedMethods) {
		for ( ResolvedMethod resolvedMethod : resolvedMethods ) {
			if ( resolvedMethod.getName().equals( method.getName() ) ) {
				// todo : we really need to check signatures too
				return resolvedMethod;
			}
		}
//		throw new AssertionFailure(
//				String.format(
//						"Unable to resolve type information of method : %s",
//						method.toGenericString()
//				)
//		);
		return null;
	}

	private String buildMethodAnnotationsKey(Method method) {
		StringBuilder buff = new StringBuilder();
		buff.append( method.getReturnType().getName() )
				.append( ' ' )
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

	private static class JandexPivot {
		private final ClassInfo classInfo;

		private Map<DotName,List<AnnotationInstance>> allAnnotations;
		private Map<DotName,AnnotationInstance> typeAnnotations = new HashMap<DotName, AnnotationInstance>();
		private Map<String,Map<DotName,AnnotationInstance>> fieldAnnotations = new HashMap<String, Map<DotName, AnnotationInstance>>();
		private Map<String,Map<DotName,AnnotationInstance>> methodAnnotations = new HashMap<String, Map<DotName, AnnotationInstance>>();

		private JandexPivot(ClassInfo classInfo) {
			this.classInfo = classInfo;
			this.allAnnotations = classInfo == null
					? Collections.<DotName, List<AnnotationInstance>>emptyMap()
					: classInfo.annotations();
		}

	}

}
