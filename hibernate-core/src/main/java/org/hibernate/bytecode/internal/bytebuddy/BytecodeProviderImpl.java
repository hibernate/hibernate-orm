/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerClassLocator;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.internal.PropertyAccessEmbeddedImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;
import org.hibernate.property.access.spi.SetterMethodImpl;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveBoxingDelegate;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveUnboxingDelegate;
import net.bytebuddy.implementation.bytecode.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BytecodeProviderImpl implements BytecodeProvider {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( BytecodeProviderImpl.class );

	private static final String INSTANTIATOR_PROXY_NAMING_SUFFIX = "HibernateInstantiator";
	private static final String OPTIMIZER_PROXY_NAMING_SUFFIX = "HibernateAccessOptimizer";
	private static final ElementMatcher.Junction<NamedElement> newInstanceMethodName = ElementMatchers.named(
			"newInstance" );
	private static final ElementMatcher.Junction<NamedElement> getPropertyValuesMethodName = ElementMatchers.named(
			"getPropertyValues" );
	private static final ElementMatcher.Junction<NamedElement> setPropertyValuesMethodName = ElementMatchers.named(
			"setPropertyValues" );
	private static final ElementMatcher.Junction<NamedElement> getPropertyNamesMethodName = ElementMatchers.named(
			"getPropertyNames" );
	private static final Member EMBEDDED_MEMBER = new Member() {
		@Override
		public Class<?> getDeclaringClass() {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public int getModifiers() {
			return 0;
		}

		@Override
		public boolean isSynthetic() {
			return false;
		}
	};

	private final ByteBuddyState byteBuddyState;

	private final ByteBuddyProxyHelper byteBuddyProxyHelper;

	/**
	 * Constructs a ByteBuddy BytecodeProvider instance which attempts to auto-detect the target JVM version
	 * from the currently running one, with a fallback on Java 11.
	 */
	public BytecodeProviderImpl() {
		this( ClassFileVersion.ofThisVm( ClassFileVersion.JAVA_V11 ) );
	}

	/**
	 * Constructs a ByteBuddy BytecodeProvider instance which aims to produce code compatible
	 * with the specified target JVM version.
	 */
	public BytecodeProviderImpl(ClassFileVersion targetCompatibleJVM) {
		this.byteBuddyState = new ByteBuddyState( targetCompatibleJVM );
		this.byteBuddyProxyHelper = new ByteBuddyProxyHelper( byteBuddyState );
	}

	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new ProxyFactoryFactoryImpl( byteBuddyState, byteBuddyProxyHelper );
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer(
			final Class clazz,
			final String[] getterNames,
			final String[] setterNames,
			final Class[] types) {
		final Class<?> fastClass;
		if ( !clazz.isInterface() && !Modifier.isAbstract( clazz.getModifiers() ) ) {
			// we only provide a fast class instantiator if the class can be instantiated
			final Constructor<?> constructor = findConstructor( clazz );

			if ( constructor == null || Modifier.isPrivate( constructor.getModifiers() ) ) {
				// In the current implementation of the ReflectionOptimizer contract, we can't call private constructors
				// To support that, we have to inject a static factory method into the class during enhancement
				fastClass = null;
			}
			else {
				fastClass = byteBuddyState.load( clazz, byteBuddy -> byteBuddy
						.with( new NamingStrategy.SuffixingRandom(
								INSTANTIATOR_PROXY_NAMING_SUFFIX,
								new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( clazz.getName() )
						) )
						.subclass( ReflectionOptimizer.InstantiationOptimizer.class )
						.method( newInstanceMethodName )
						.intercept( MethodCall.construct( constructor ) )
				);
			}
		}
		else {
			fastClass = null;
		}

		final Method[] getters = new Method[getterNames.length];
		final Method[] setters = new Method[setterNames.length];
		try {
			findAccessors( clazz, getterNames, setterNames, types, getters, setters );
		}
		catch (InvalidPropertyAccessorException ex) {
			LOG.unableToGenerateReflectionOptimizer( clazz.getName(), ex.getMessage() );
			return null;
		}

		final Class<?> bulkAccessor = byteBuddyState.load( clazz, byteBuddy -> byteBuddy
				.with( new NamingStrategy.SuffixingRandom(
						OPTIMIZER_PROXY_NAMING_SUFFIX,
						new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( clazz.getName() )
				) )
				.subclass( Object.class )
				.implement( ReflectionOptimizer.AccessOptimizer.class )
				.method( getPropertyValuesMethodName )
				.intercept( new Implementation.Simple( new GetPropertyValues( clazz, getterNames, getters ) ) )
				.method( setPropertyValuesMethodName )
				.intercept( new Implementation.Simple( new SetPropertyValues( clazz, getterNames, setters ) ) )
				.method( getPropertyNamesMethodName )
				.intercept( MethodCall.call( new CloningPropertyCall( getterNames ) ) )
		);

		try {
			return new ReflectionOptimizerImpl(
					fastClass != null ? (ReflectionOptimizer.InstantiationOptimizer) fastClass.newInstance() : null,
					(ReflectionOptimizer.AccessOptimizer) bulkAccessor.newInstance()
			);
		}
		catch (Exception exception) {
			throw new HibernateException( exception );
		}
	}

	@Override
	public @Nullable ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, Map<String, PropertyAccess> propertyAccessMap) {
		final Class<?> fastClass;
		if ( !clazz.isInterface() && !Modifier.isAbstract( clazz.getModifiers() ) ) {
			// we only provide a fast class instantiator if the class can be instantiated
			final Constructor<?> constructor = findConstructor( clazz );

			if ( constructor == null || Modifier.isPrivate( constructor.getModifiers() ) ) {
				// In the current implementation of the ReflectionOptimizer contract, we can't call private constructors
				// To support that, we have to inject a static factory method into the class during enhancement
				fastClass = null;
			}
			else {
				fastClass = byteBuddyState.load( clazz, byteBuddy -> byteBuddy
						.with( new NamingStrategy.SuffixingRandom(
								INSTANTIATOR_PROXY_NAMING_SUFFIX,
								new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( clazz.getName() )
						) )
						.subclass( ReflectionOptimizer.InstantiationOptimizer.class )
						.method( newInstanceMethodName )
						.intercept( MethodCall.construct( constructor ) )
				);
			}
		}
		else {
			fastClass = null;
		}

		final Member[] getters = new Member[propertyAccessMap.size()];
		final Member[] setters = new Member[propertyAccessMap.size()];
		try {
			findAccessors( clazz, propertyAccessMap, getters, setters );
		}
		catch (InvalidPropertyAccessorException ex) {
			LOG.unableToGenerateReflectionOptimizer( clazz.getName(), ex.getMessage() );
			return null;
		}

		Class<?> superClass = determineAccessOptimizerSuperClass( clazz, getters, setters );

		final String[] propertyNames = propertyAccessMap.keySet().toArray( new String[0] );
		final Class<?> bulkAccessor = byteBuddyState.load( clazz, byteBuddy -> byteBuddy
				.with( new NamingStrategy.SuffixingRandom(
						OPTIMIZER_PROXY_NAMING_SUFFIX,
						new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( clazz.getName() )
				) )
				.subclass( superClass )
				.implement( ReflectionOptimizer.AccessOptimizer.class )
				.method( getPropertyValuesMethodName )
				.intercept( new Implementation.Simple( new GetPropertyValues( clazz, propertyNames, getters ) ) )
				.method( setPropertyValuesMethodName )
				.intercept( new Implementation.Simple( new SetPropertyValues( clazz, propertyNames, setters ) ) )
				.method( getPropertyNamesMethodName )
				.intercept( MethodCall.call( new CloningPropertyCall( propertyNames ) ) )
		);

		try {
			return new ReflectionOptimizerImpl(
					fastClass != null ? (ReflectionOptimizer.InstantiationOptimizer) fastClass.newInstance() : null,
					(ReflectionOptimizer.AccessOptimizer) bulkAccessor.newInstance()
			);
		}
		catch (Exception exception) {
			throw new HibernateException( exception );
		}
	}

	private static class ForeignPackageClassInfo {
		final Class<?> clazz;
		final List<Member> getters = new ArrayList<>();
		final List<Member> setters = new ArrayList<>();

		public ForeignPackageClassInfo(Class<?> clazz) {
			this.clazz = clazz;
		}
	}

	private Class<?> determineAccessOptimizerSuperClass(Class<?> clazz, Member[] getters, Member[] setters) {
		if ( clazz.isInterface() ) {
			return Object.class;
		}
		// generate access optimizer super classes for foreign package super classes that declare fields
		//  each should declare protected static methods get_FIELDNAME(OWNER)/set_FIELDNAME(OWNER, TYPE)
		//  which should be called then from within GetPropertyValues/SetPropertyValues
		//  Since these super classes will be in the correct package, the package-private entity field access is fine
		final List<ForeignPackageClassInfo> foreignPackageClassInfos = createForeignPackageClassInfos( clazz );
		for ( Iterator<ForeignPackageClassInfo> iterator = foreignPackageClassInfos.iterator(); iterator.hasNext(); ) {
			final ForeignPackageClassInfo foreignPackageClassInfo = iterator.next();
			for ( int i = 0; i < getters.length; i++ ) {
				final Member getter = getters[i];
				final Member setter = setters[i];
				if ( getter.getDeclaringClass() == foreignPackageClassInfo.clazz && !Modifier.isPublic( getter.getModifiers() ) ) {
					foreignPackageClassInfo.getters.add( getter );
				}
				if ( setter.getDeclaringClass() == foreignPackageClassInfo.clazz && !Modifier.isPublic( setter.getModifiers() ) ) {
					foreignPackageClassInfo.setters.add( setter );
				}
			}
			if ( foreignPackageClassInfo.getters.isEmpty() && foreignPackageClassInfo.setters.isEmpty() ) {
				iterator.remove();
			}
		}

		Class<?> superClass = Object.class;
		for ( int i = foreignPackageClassInfos.size() - 1; i >= 0; i-- ) {
			final ForeignPackageClassInfo foreignPackageClassInfo = foreignPackageClassInfos.get( i );
			final Class<?> newSuperClass = superClass;
			superClass = byteBuddyState.load(
					foreignPackageClassInfo.clazz,
					byteBuddy -> {
						DynamicType.Builder<?> builder = byteBuddy.with(
								new NamingStrategy.SuffixingRandom(
										OPTIMIZER_PROXY_NAMING_SUFFIX,
										new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue(
												foreignPackageClassInfo.clazz.getName() )
								)
						).subclass( newSuperClass );
						for ( Member getter : foreignPackageClassInfo.getters ) {
							final Class<?> getterType;
							if ( getter instanceof Field ) {
								getterType = ( (Field) getter ).getType();
							}
							else {
								getterType = ( (Method) getter ).getReturnType();
							}

							builder = builder.defineMethod(
											"get_" + getter.getName(),
											TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(
													getterType
											),
											Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC
									)
									.withParameter( foreignPackageClassInfo.clazz )
									.intercept(
											new Implementation.Simple(
													new GetFieldOnArgument(
															getter
													)
											)
									);
						}
						for ( Member setter : foreignPackageClassInfo.setters ) {
							final Class<?> setterType;
							if ( setter instanceof Field ) {
								setterType = ( (Field) setter ).getType();
							}
							else {
								setterType = ( (Method) setter ).getParameterTypes()[0];
							}

							builder = builder.defineMethod(
											"set_" + setter.getName(),
											TypeDescription.Generic.VOID,
											Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC
									)
									.withParameter( foreignPackageClassInfo.clazz )
									.withParameter( setterType )
									.intercept(
											new Implementation.Simple(
													new SetFieldOnArgument(
															setter
													)
											)
									);
						}

						return builder;
					}
			);

			for ( int j = 0; j < getters.length; j++ ) {
				final Member getter = getters[j];
				final Member setter = setters[j];
				if ( foreignPackageClassInfo.getters.contains( getter ) ) {
					getters[j] = new ForeignPackageMember( superClass, getter );
				}
				if ( foreignPackageClassInfo.setters.contains( setter ) ) {
					setters[j] = new ForeignPackageMember( superClass, setter );
				}
			}
		}

		return superClass;
	}

	private static class ForeignPackageMember implements Member {

		private final Class<?> foreignPackageAccessor;
		private final Member member;

		public ForeignPackageMember(Class<?> foreignPackageAccessor, Member member) {
			this.foreignPackageAccessor = foreignPackageAccessor;
			this.member = member;
		}

		public Class<?> getForeignPackageAccessor() {
			return foreignPackageAccessor;
		}

		public Member getMember() {
			return member;
		}

		@Override
		public Class<?> getDeclaringClass() {
			return member.getDeclaringClass();
		}

		@Override
		public String getName() {
			return member.getName();
		}

		@Override
		public int getModifiers() {
			return member.getModifiers();
		}

		@Override
		public boolean isSynthetic() {
			return member.isSynthetic();
		}
	}

	private static class GetFieldOnArgument implements ByteCodeAppender {

		private final Member getterMember;

		public GetFieldOnArgument(Member getterMember) {
			this.getterMember = getterMember;
		}

		@Override
		public Size apply(
				MethodVisitor methodVisitor,
				Implementation.Context implementationContext,
				MethodDescription instrumentedMethod) {
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			final Class<?> type;
			if ( getterMember instanceof Method ) {
				final Method getter = (Method) getterMember;
				type = getter.getReturnType();
				methodVisitor.visitMethodInsn(
						getter.getDeclaringClass().isInterface() ?
								Opcodes.INVOKEINTERFACE :
								Opcodes.INVOKEVIRTUAL,
						Type.getInternalName( getter.getDeclaringClass() ),
						getter.getName(),
						Type.getMethodDescriptor( getter ),
						getter.getDeclaringClass().isInterface()
				);
			}
			else {
				final Field getter = (Field) getterMember;
				type = getter.getType();
				methodVisitor.visitFieldInsn(
						Opcodes.GETFIELD,
						Type.getInternalName( getter.getDeclaringClass() ),
						getter.getName(),
						Type.getDescriptor( type )
				);
			}
			methodVisitor.visitInsn( getReturnOpCode( type ) );
			return new Size( 2, instrumentedMethod.getStackSize() );
		}

		private int getReturnOpCode(Class<?> type) {
			if ( type.isPrimitive() ) {
				switch ( type.getTypeName() ) {
					case "long":
						return Opcodes.LRETURN;
					case "float":
						return Opcodes.FRETURN;
					case "double":
						return Opcodes.DRETURN;
				}
				return Opcodes.IRETURN;
			}
			return Opcodes.ARETURN;
		}
	}

	private static class SetFieldOnArgument implements ByteCodeAppender {

		private final Member setterMember;

		public SetFieldOnArgument(Member setterMember) {
			this.setterMember = setterMember;
		}

		@Override
		public Size apply(
				MethodVisitor methodVisitor,
				Implementation.Context implementationContext,
				MethodDescription instrumentedMethod) {
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			final Class<?> type;
			if ( setterMember instanceof Method ) {
				final Method setter = (Method) setterMember;
				type = setter.getParameterTypes()[0];
				methodVisitor.visitVarInsn( getLoadOpCode( type ), 1 );
				methodVisitor.visitMethodInsn(
						setter.getDeclaringClass().isInterface() ?
								Opcodes.INVOKEINTERFACE :
								Opcodes.INVOKEVIRTUAL,
						Type.getInternalName( setter.getDeclaringClass() ),
						setter.getName(),
						Type.getMethodDescriptor(
								Type.getType( void.class ),
								Type.getType( type )
						),
						setter.getDeclaringClass().isInterface()
				);
				if ( setter.getReturnType() != void.class ) {
					// Setters could return something which we have to ignore
					switch ( setter.getReturnType().getTypeName() ) {
						case "long":
						case "double":
							methodVisitor.visitInsn( Opcodes.POP2 );
							break;
						default:
							methodVisitor.visitInsn( Opcodes.POP );
							break;
					}
				}
			}
			else {
				final Field setter = (Field) setterMember;
				type = setter.getType();
				methodVisitor.visitVarInsn( getLoadOpCode( type ), 1 );
				methodVisitor.visitFieldInsn(
						Opcodes.PUTFIELD,
						Type.getInternalName( setter.getDeclaringClass() ),
						setter.getName(),
						Type.getDescriptor( type )
				);
			}
			methodVisitor.visitInsn( Opcodes.RETURN );
			return new Size(
					is64BitType( type ) ? 3 : 2,
					instrumentedMethod.getStackSize()
			);
		}

		private int getLoadOpCode(Class<?> type) {
			if ( type.isPrimitive() ) {
				switch ( type.getTypeName() ) {
					case "long":
						return Opcodes.LLOAD;
					case "float":
						return Opcodes.FLOAD;
					case "double":
						return Opcodes.DLOAD;
				}
				return Opcodes.ILOAD;
			}
			return Opcodes.ALOAD;
		}

		private boolean is64BitType(Class<?> type) {
			return type == long.class || type == double.class;
		}
	}

	private List<ForeignPackageClassInfo> createForeignPackageClassInfos(Class<?> clazz) {
		final List<ForeignPackageClassInfo> foreignPackageClassInfos = new ArrayList<>();
		Class<?> c = clazz.getSuperclass();
		while (c != Object.class) {
			if ( !c.getPackageName().equals( clazz.getPackageName() ) ) {
				foreignPackageClassInfos.add( new ForeignPackageClassInfo( c ) );
			}
			c = c.getSuperclass();
		}
		return foreignPackageClassInfos;
	}

	public ByteBuddyProxyHelper getByteBuddyProxyHelper() {
		return byteBuddyProxyHelper;
	}

	private static class GetPropertyValues implements ByteCodeAppender {

		private final Class<?> clazz;
		private final String[] propertyNames;
		private final Member[] getters;
		private final boolean persistentAttributeInterceptable;

		public GetPropertyValues(Class<?> clazz, String[] propertyNames, Member[] getters) {
			this.clazz = clazz;
			this.propertyNames = propertyNames;
			this.getters = getters;
			this.persistentAttributeInterceptable = PersistentAttributeInterceptable.class.isAssignableFrom( clazz );
		}

		@Override
		public Size apply(
				MethodVisitor methodVisitor,
				Implementation.Context implementationContext,
				MethodDescription instrumentedMethod) {
			if ( persistentAttributeInterceptable ) {
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
				methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( clazz ) );

				// Extract the interceptor
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKEVIRTUAL,
						Type.getInternalName( clazz ),
						"$$_hibernate_getInterceptor",
						Type.getMethodDescriptor( Type.getType( PersistentAttributeInterceptor.class ) ),
						false
				);
				// Duplicate the interceptor on the stack and check if it implements LazyAttributeLoadingInterceptor
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitTypeInsn(
						Opcodes.INSTANCEOF,
						Type.getInternalName( LazyAttributeLoadingInterceptor.class )
				);

				// Jump to the false label if the instanceof check fails
				final Label instanceofFalseLabel = new Label();
				methodVisitor.visitJumpInsn( Opcodes.IFEQ, instanceofFalseLabel );

				// Cast to the subtype, so we can mark the property as initialized
				methodVisitor.visitTypeInsn(
						Opcodes.CHECKCAST,
						Type.getInternalName( LazyAttributeLoadingInterceptor.class )
				);
				// Store the LazyAttributeLoadingInterceptor at index 2
				methodVisitor.visitVarInsn( Opcodes.ASTORE, 2 );

				// Skip the cleanup
				final Label instanceofEndLabel = new Label();
				methodVisitor.visitJumpInsn( Opcodes.GOTO, instanceofEndLabel );

				// Here is the cleanup section for the false branch
				methodVisitor.visitLabel( instanceofFalseLabel );
				// We still have the duplicated interceptor on the stack
				implementationContext.getFrameGeneration().full(
						methodVisitor,
						Arrays.asList(
								TypeDescription.ForLoadedType.of( PersistentAttributeInterceptor.class )
						),
						Arrays.asList(
								implementationContext.getInstrumentedType(),
								TypeDescription.ForLoadedType.of( Object.class )
						)
				);
				// Pop that duplicated interceptor from the stack
				methodVisitor.visitInsn( Opcodes.POP );
				methodVisitor.visitInsn( Opcodes.ACONST_NULL );
				methodVisitor.visitVarInsn( Opcodes.ASTORE, 2 );

				methodVisitor.visitLabel( instanceofEndLabel );
				implementationContext.getFrameGeneration().full(
						methodVisitor,
						Collections.emptyList(),
						Arrays.asList(
								implementationContext.getInstrumentedType(),
								TypeDescription.ForLoadedType.of( Object.class ),
								TypeDescription.ForLoadedType.of( LazyAttributeLoadingInterceptor.class )
						)
				);
			}
			methodVisitor.visitLdcInsn( getters.length );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( Object.class ) );
			for ( int index = 0; index < getters.length; index++ ) {
				final Member getterMember = getters[index];
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( index );

				final Label arrayStoreLabel = new Label();
				if ( getterMember == EMBEDDED_MEMBER ) {
					// The embedded property access returns the owner
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
				}
				else {
					if ( persistentAttributeInterceptable ) {
						final Label extractValueLabel = new Label();

						// Load the LazyAttributeLoadingInterceptor
						methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );
						// If that is null, then assume attributes are loaded and jump to extraction
						methodVisitor.visitJumpInsn( Opcodes.IFNULL, extractValueLabel );
						// Load the LazyAttributeLoadingInterceptor
						methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );
						// Load the current property name
						methodVisitor.visitLdcInsn( propertyNames[index] );
						// Invoke isAttributeLoaded on the interceptor
						methodVisitor.visitMethodInsn(
								Opcodes.INVOKEVIRTUAL,
								Type.getInternalName( LazyAttributeLoadingInterceptor.class ),
								"isAttributeLoaded",
								Type.getMethodDescriptor( Type.getType( boolean.class ), Type.getType( String.class ) ),
								false
						);
						// If the attribute is loaded, jump to extraction
						methodVisitor.visitJumpInsn( Opcodes.IFNE, extractValueLabel );

						// Push LazyPropertyInitializer.UNFETCHED_PROPERTY on the stack
						methodVisitor.visitFieldInsn(
								Opcodes.GETSTATIC,
								Type.getInternalName( LazyPropertyInitializer.class ),
								"UNFETCHED_PROPERTY",
								Type.getDescriptor( Serializable.class )
						);
						// Jump to the label where we handle storing the unfetched property
						methodVisitor.visitJumpInsn( Opcodes.GOTO, arrayStoreLabel );

						// This is the end of the lazy check i.e. the start of extraction
						methodVisitor.visitLabel( extractValueLabel );
						implementationContext.getFrameGeneration().full(
								methodVisitor,
								Arrays.asList(
										TypeDescription.ForLoadedType.of( Object[].class ),
										TypeDescription.ForLoadedType.of( Object[].class ),
										TypeDescription.ForLoadedType.of( int.class )
								),
								Arrays.asList(
										implementationContext.getInstrumentedType(),
										TypeDescription.ForLoadedType.of( Object.class ),
										TypeDescription.ForLoadedType.of( LazyAttributeLoadingInterceptor.class )
								)
						);
					}

					// Load the entity to extract the property
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
					methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( clazz ) );

					final Class<?> type;
					if ( getterMember instanceof Method ) {
						final Method getter = (Method) getterMember;
						type = getter.getReturnType();
						methodVisitor.visitMethodInsn(
								getter.getDeclaringClass().isInterface() ?
										Opcodes.INVOKEINTERFACE :
										Opcodes.INVOKEVIRTUAL,
								Type.getInternalName( getter.getDeclaringClass() ),
								getter.getName(),
								Type.getMethodDescriptor( getter ),
								getter.getDeclaringClass().isInterface()
						);
					}
					else if ( getterMember instanceof Field ) {
						final Field getter = (Field) getterMember;
						type = getter.getType();
						methodVisitor.visitFieldInsn(
								Opcodes.GETFIELD,
								Type.getInternalName( getter.getDeclaringClass() ),
								getter.getName(),
								Type.getDescriptor( type )
						);
					}
					else {
						assert getterMember instanceof ForeignPackageMember;
						final ForeignPackageMember foreignPackageMember = (ForeignPackageMember) getterMember;
						final Member underlyingMember = foreignPackageMember.getMember();
						if ( underlyingMember instanceof Method ) {
							final Method getter = (Method) underlyingMember;
							type = getter.getReturnType();
						}
						else {
							final Field getter = (Field) underlyingMember;
							type = getter.getType();
						}
						methodVisitor.visitMethodInsn(
								Opcodes.INVOKESTATIC,
								Type.getInternalName( foreignPackageMember.getForeignPackageAccessor() ),
								"get_" + getterMember.getName(),
								Type.getMethodDescriptor(
										Type.getType( type ),
										Type.getType( underlyingMember.getDeclaringClass() )
								),
								false
						);
					}
					if ( type.isPrimitive() ) {
						PrimitiveBoxingDelegate.forPrimitive( new TypeDescription.ForLoadedType( type ) )
								.assignBoxedTo(
										TypeDescription.Generic.OBJECT,
										ReferenceTypeAwareAssigner.INSTANCE,
										Assigner.Typing.STATIC
								)
								.apply( methodVisitor, implementationContext );
					}
				}
				if ( persistentAttributeInterceptable ) {
					methodVisitor.visitLabel( arrayStoreLabel );
					implementationContext.getFrameGeneration().full(
							methodVisitor,
							Arrays.asList(
									TypeDescription.ForLoadedType.of( Object[].class ),
									TypeDescription.ForLoadedType.of( Object[].class ),
									TypeDescription.ForLoadedType.of( int.class ),
									TypeDescription.ForLoadedType.of( Object.class )
							),
							Arrays.asList(
									implementationContext.getInstrumentedType(),
									TypeDescription.ForLoadedType.of( Object.class ),
									TypeDescription.ForLoadedType.of( LazyAttributeLoadingInterceptor.class )
							)
					);
				}
				methodVisitor.visitInsn( Opcodes.AASTORE );
			}
			methodVisitor.visitInsn( Opcodes.ARETURN );
			return new Size( 6, instrumentedMethod.getStackSize() + 1 );
		}
	}

	private static class SetPropertyValues implements ByteCodeAppender {

		private final Class<?> clazz;
		private final String[] propertyNames;
		private final Member[] setters;
		private final boolean enhanced;

		public SetPropertyValues(Class<?> clazz, String[] propertyNames, Member[] setters) {
			this.clazz = clazz;
			this.propertyNames = propertyNames;
			this.setters = setters;
			this.enhanced = Managed.class.isAssignableFrom( clazz );
		}

		@Override
		public Size apply(
				MethodVisitor methodVisitor,
				Implementation.Context implementationContext,
				MethodDescription instrumentedMethod) {
			final boolean persistentAttributeInterceptable = PersistentAttributeInterceptable.class.isAssignableFrom( clazz );
			final boolean compositeOwner = CompositeOwner.class.isAssignableFrom( clazz );
			Label currentLabel = null;
			Label nextLabel = new Label();
			for ( int index = 0; index < setters.length; index++ ) {
				final Member setterMember = setters[index];
				if ( enhanced && currentLabel != null ) {
					methodVisitor.visitLabel( currentLabel );
					implementationContext.getFrameGeneration().same(
							methodVisitor,
							instrumentedMethod.getParameters().asTypeList()
					);
				}
				if ( setterMember == EMBEDDED_MEMBER ) {
					// The embedded property access does a no-op
					continue;
				}
				// Push entity on stack
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
				methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( clazz ) );
				// Push values array on stack
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );
				methodVisitor.visitLdcInsn( index );
				// Load value for property from array
				methodVisitor.visitInsn( Opcodes.AALOAD );
				if ( enhanced ) {
					// Duplicate the property value
					methodVisitor.visitInsn( Opcodes.DUP );
					// Push LazyPropertyInitializer.UNFETCHED_PROPERTY on the stack
					methodVisitor.visitFieldInsn(
							Opcodes.GETSTATIC,
							Type.getInternalName( LazyPropertyInitializer.class ),
							"UNFETCHED_PROPERTY",
							Type.getDescriptor( Serializable.class )
					);
					Label setterLabel = new Label();
					// Compare property value against LazyPropertyInitializer.UNFETCHED_PROPERTY
					// and jump to the setter label if that is unequal
					methodVisitor.visitJumpInsn( Opcodes.IF_ACMPNE, setterLabel );

					// When we get here, we need to clean up the stack before proceeding with the next property
					// Pop the property value
					methodVisitor.visitInsn( Opcodes.POP );
					// Pop the entity
					methodVisitor.visitInsn( Opcodes.POP );
					methodVisitor.visitJumpInsn( Opcodes.GOTO, nextLabel );

					// This label is jumped to when property value != LazyPropertyInitializer.UNFETCHED_PROPERTY
					// At which point we have the entity and the value on the stack
					methodVisitor.visitLabel( setterLabel );
					implementationContext.getFrameGeneration().full(
							methodVisitor,
							Arrays.asList(
									TypeDescription.ForLoadedType.of( clazz ),
									TypeDescription.ForLoadedType.of( Object.class )
							),
							Arrays.asList(
									implementationContext.getInstrumentedType(),
									TypeDescription.ForLoadedType.of( Object.class ),
									TypeDescription.ForLoadedType.of( Object[].class )
							)
					);
				}
				final Class<?> type;
				if ( setterMember instanceof Method ) {
					final Method setter = (Method) setterMember;
					type = setter.getParameterTypes()[0];
				}
				else if ( setterMember instanceof Field ) {
					final Field field = (Field) setterMember;
					type = field.getType();
				}
				else {
					final ForeignPackageMember foreignPackageMember = (ForeignPackageMember) setterMember;
					final Member underlyingMember = foreignPackageMember.getMember();
					if ( underlyingMember instanceof Method ) {
						final Method setter = (Method) underlyingMember;
						type = setter.getParameterTypes()[0];
					}
					else {
						final Field field = (Field) underlyingMember;
						type = field.getType();
					}
				}
				if ( type.isPrimitive() ) {
					PrimitiveUnboxingDelegate.forReferenceType( TypeDescription.Generic.OBJECT )
							.assignUnboxedTo(
									new TypeDescription.Generic.OfNonGenericType.ForLoadedType( type ),
									ReferenceTypeAwareAssigner.INSTANCE,
									Assigner.Typing.DYNAMIC
							)
							.apply( methodVisitor, implementationContext );
				}
				else {
					methodVisitor.visitTypeInsn(
							Opcodes.CHECKCAST,
							Type.getInternalName( type )
					);
				}
				if ( setterMember instanceof Method ) {
					final Method setter = (Method) setterMember;
					methodVisitor.visitMethodInsn(
							setter.getDeclaringClass().isInterface() ?
									Opcodes.INVOKEINTERFACE :
									Opcodes.INVOKEVIRTUAL,
							Type.getInternalName( setter.getDeclaringClass() ),
							setter.getName(),
							Type.getMethodDescriptor( setter ),
							setter.getDeclaringClass().isInterface()
					);
					if ( setter.getReturnType() != void.class ) {
						// Setters could return something which we have to ignore
						switch ( setter.getReturnType().getTypeName() ) {
							case "long":
							case "double":
								methodVisitor.visitInsn( Opcodes.POP2 );
								break;
							default:
								methodVisitor.visitInsn( Opcodes.POP );
								break;
						}
					}
				}
				else if ( setterMember instanceof Field ) {
					final Field field = (Field) setterMember;
					methodVisitor.visitFieldInsn(
							Opcodes.PUTFIELD,
							Type.getInternalName( field.getDeclaringClass() ),
							field.getName(),
							Type.getDescriptor( type )
					);
				}
				else {
					final ForeignPackageMember foreignPackageMember = (ForeignPackageMember) setterMember;
					methodVisitor.visitMethodInsn(
							Opcodes.INVOKESTATIC,
							Type.getInternalName( foreignPackageMember.getForeignPackageAccessor() ),
							"set_" + setterMember.getName(),
							Type.getMethodDescriptor(
									Type.getType( void.class ),
									Type.getType( foreignPackageMember.getMember().getDeclaringClass() ),
									Type.getType( type )
							),
							false
					);
				}
				if ( enhanced ) {
					final boolean compositeTracker = CompositeTracker.class.isAssignableFrom( type );
					// The composite owner check and setting only makes sense if
					//  * the value type is a composite tracker
					//  * a value subtype can be a composite tracker
					//
					// Final classes that don't already implement the interface never need to be checked.
					// This helps a bit with common final types which otherwise would have to be checked a lot.
					if ( compositeOwner && ( compositeTracker || !Modifier.isFinal( type.getModifiers() ) ) ) {
						// Push values array on stack
						methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );
						methodVisitor.visitLdcInsn( index );
						// Load value for property from array
						methodVisitor.visitInsn( Opcodes.AALOAD );

						// Check if value implements composite tracker
						methodVisitor.visitInsn( Opcodes.DUP );
						final Label compositeTrackerFalseLabel = new Label();
						final String compositeTrackerType;
						final boolean isInterface;
						if ( compositeTracker ) {
							// If the known type already implements that interface, we use that type,
							// so we just do a null check
							compositeTrackerType = Type.getInternalName( type );
							isInterface = false;
							methodVisitor.visitJumpInsn( Opcodes.IFNULL, compositeTrackerFalseLabel );
						}
						else {
							// If we don't know for sure, we do an instanceof check
							methodVisitor.visitTypeInsn(
									Opcodes.INSTANCEOF,
									compositeTrackerType = Type.getInternalName( CompositeTracker.class )
							);
							isInterface = true;
							methodVisitor.visitJumpInsn( Opcodes.IFEQ, compositeTrackerFalseLabel );
						}

						// Load the tracker on which we will call $$_hibernate_setOwner
						methodVisitor.visitTypeInsn(
								Opcodes.CHECKCAST,
								compositeTrackerType
						);
						methodVisitor.visitLdcInsn( propertyNames[index] );
						// Load the owner and cast it to the owner class, as we know it implements CompositeOwner
						methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
						methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( clazz ) );
						// Invoke the method to set the owner
						methodVisitor.visitMethodInsn(
								isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
								compositeTrackerType,
								"$$_hibernate_setOwner",
								Type.getMethodDescriptor(
										Type.getType( void.class ),
										Type.getType( String.class ),
										Type.getType( CompositeOwner.class )
								),
								isInterface
						);

						// Skip the cleanup
						final Label compositeTrackerEndLabel = new Label();
						methodVisitor.visitJumpInsn( Opcodes.GOTO, compositeTrackerEndLabel );

						// Here is the cleanup section for the false branch
						methodVisitor.visitLabel( compositeTrackerFalseLabel );
						// We still have the duplicated value on the stack
						implementationContext.getFrameGeneration().full(
								methodVisitor,
								Arrays.asList(
										TypeDescription.ForLoadedType.of( Object.class )
								),
								Arrays.asList(
										implementationContext.getInstrumentedType(),
										TypeDescription.ForLoadedType.of( Object.class ),
										TypeDescription.ForLoadedType.of( Object[].class )
								)
						);
						// Pop that duplicated property value from the stack
						methodVisitor.visitInsn( Opcodes.POP );

						// Clean stack after the if block
						methodVisitor.visitLabel( compositeTrackerEndLabel );
						implementationContext.getFrameGeneration().same(methodVisitor, instrumentedMethod.getParameters().asTypeList());
					}
					if ( persistentAttributeInterceptable ) {
						// Load the owner
						methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
						methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( clazz ) );
						// Extract the interceptor
						methodVisitor.visitMethodInsn(
								Opcodes.INVOKEVIRTUAL,
								Type.getInternalName( clazz ),
								"$$_hibernate_getInterceptor",
								Type.getMethodDescriptor( Type.getType( PersistentAttributeInterceptor.class ) ),
								false
						);
						// Duplicate the interceptor on the stack and check if it implements BytecodeLazyAttributeInterceptor
						methodVisitor.visitInsn( Opcodes.DUP );
						methodVisitor.visitTypeInsn(
								Opcodes.INSTANCEOF,
								Type.getInternalName( BytecodeLazyAttributeInterceptor.class )
						);

						// Jump to the false label if the instanceof check fails
						final Label instanceofFalseLabel = new Label();
						methodVisitor.visitJumpInsn( Opcodes.IFEQ, instanceofFalseLabel );

						// Cast to the subtype, so we can mark the property as initialized
						methodVisitor.visitTypeInsn(
								Opcodes.CHECKCAST,
								Type.getInternalName( BytecodeLazyAttributeInterceptor.class )
						);
						// Load the property name
						methodVisitor.visitLdcInsn( propertyNames[index] );
						// Invoke the method to mark the property as initialized
						methodVisitor.visitMethodInsn(
								Opcodes.INVOKEINTERFACE,
								Type.getInternalName( BytecodeLazyAttributeInterceptor.class ),
								"attributeInitialized",
								Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( String.class ) ),
								true
						);

						// Skip the cleanup
						final Label instanceofEndLabel = new Label();
						methodVisitor.visitJumpInsn( Opcodes.GOTO, instanceofEndLabel );

						// Here is the cleanup section for the false branch
						methodVisitor.visitLabel( instanceofFalseLabel );
						// We still have the duplicated interceptor on the stack
						implementationContext.getFrameGeneration().full(
								methodVisitor,
								Arrays.asList(
										TypeDescription.ForLoadedType.of( PersistentAttributeInterceptor.class )
								),
								Arrays.asList(
										implementationContext.getInstrumentedType(),
										TypeDescription.ForLoadedType.of( Object.class ),
										TypeDescription.ForLoadedType.of( Object[].class )
								)
						);
						// Pop that duplicated interceptor from the stack
						methodVisitor.visitInsn( Opcodes.POP );

						// Clean stack after the if block
						methodVisitor.visitLabel( instanceofEndLabel );
						implementationContext.getFrameGeneration().same(methodVisitor, instrumentedMethod.getParameters().asTypeList());
					}

					currentLabel = nextLabel;
					nextLabel = new Label();
				}
			}
			if ( currentLabel != null ) {
				methodVisitor.visitLabel( currentLabel );
				implementationContext.getFrameGeneration().same(methodVisitor, instrumentedMethod.getParameters().asTypeList());
			}
			methodVisitor.visitInsn( Opcodes.RETURN );
			return new Size( 4, instrumentedMethod.getStackSize() );
		}
	}

	private static void findAccessors(
			Class<?> clazz,
			String[] getterNames,
			String[] setterNames,
			Class<?>[] types,
			Method[] getters,
			Method[] setters) {
		final int length = types.length;
		if ( setterNames.length != length || getterNames.length != length ) {
			throw new HibernateException( "bad number of accessors" );
		}

		final Class<?>[] getParam = new Class[0];
		final Class<?>[] setParam = new Class[1];
		for ( int i = 0; i < length; i++ ) {
			if ( getterNames[i] != null ) {
				final Method getter = findAccessor( clazz, getterNames[i], getParam );
				if ( getter.getReturnType() != types[i] ) {
					throw new HibernateException( "wrong return type: " + getterNames[i] );
				}

				getters[i] = getter;
			}

			if ( setterNames[i] != null ) {
				setters[i] = ReflectHelper.setterMethodOrNullBySetterName( clazz, setterNames[i], types[i] );
				if ( setters[i] == null ) {
					throw new HibernateException(
							String.format(
									"cannot find an accessor [%s] on type [%s]",
									setterNames[i],
									clazz.getName()
							)
					);
				}
				else if ( Modifier.isPrivate( setters[i].getModifiers() ) ) {
					throw new PrivateAccessorException( "private accessor [" + setterNames[i] + "]" );
				}
			}
		}
	}

	private static void findAccessors(
			Class<?> clazz,
			Map<String, PropertyAccess> propertyAccessMap,
			Member[] getters,
			Member[] setters) {
		int i = 0;
		for ( Map.Entry<String, PropertyAccess> entry : propertyAccessMap.entrySet() ) {
			final PropertyAccess propertyAccess = entry.getValue();
			if ( propertyAccess instanceof PropertyAccessEmbeddedImpl ) {
				getters[i] = EMBEDDED_MEMBER;
				setters[i] = EMBEDDED_MEMBER;
				i++;
				continue;
			}
			final Getter getter = propertyAccess.getGetter();
			if ( getter == null ) {
				throw new InvalidPropertyAccessorException( "invalid getter for property [" + entry.getKey() + "]" );
			}
			final Setter setter = propertyAccess.getSetter();
			if ( setter == null ) {
				throw new InvalidPropertyAccessorException(
						String.format(
								"cannot find a setter for [%s] on type [%s]",
								entry.getKey(),
								clazz.getName()
						)
				);
			}
			final Member getterMember;
			if ( getter instanceof GetterMethodImpl ) {
				getterMember = getter.getMethod();
			}
			else if ( getter instanceof GetterFieldImpl ) {
				getterMember = ((GetterFieldImpl) getter).getField();
			}
			else {
				throw new InvalidPropertyAccessorException(
						String.format(
								"cannot find a getter for [%s] on type [%s]",
								entry.getKey(),
								clazz.getName()
						)
				);
			}
			final Member setterMember;
			if ( setter instanceof SetterMethodImpl ) {
				setterMember = setter.getMethod();
			}
			else if ( setter instanceof SetterFieldImpl ) {
				setterMember = ( (SetterFieldImpl) setter ).getField();
				if ( Modifier.isFinal( setterMember.getModifiers() ) ) {
					throw new InvalidPropertyAccessorException( "final accessor [" + setterMember.getName() + "]" );
				}
			}
			else {
				throw new InvalidPropertyAccessorException(
						String.format(
								"cannot find a setter for [%s] on type [%s]",
								entry.getKey(),
								clazz.getName()
						)
				);
			}
			if ( Modifier.isPrivate( getterMember.getModifiers() ) ) {
				throw new PrivateAccessorException( "private accessor [" + getterMember.getName() + "]" );
			}
			if ( Modifier.isPrivate( setterMember.getModifiers() ) ) {
				throw new PrivateAccessorException( "private accessor [" + setterMember.getName() + "]" );
			}
			getters[i] = getterMember;
			setters[i] = setterMember;
			i++;
		}
	}

	private static Method findAccessor(Class<?> containerClazz, String name, Class<?>[] params)
			throws PrivateAccessorException {
		Class<?> clazz = containerClazz;
		try {
			return clazz.getMethod( name, params );
		}
		catch (NoSuchMethodException e) {
			// Ignore if we didn't find a public method
		}
		do {
			try {
				final Method method = clazz.getDeclaredMethod( name, params );
				if ( Modifier.isPrivate( method.getModifiers() ) ) {
					throw new PrivateAccessorException( "private accessor [" + name + "]" );
				}

				return method;
			}
			catch (NoSuchMethodException e) {
				clazz = clazz.getSuperclass();
			}
		} while ( clazz != null );
		throw new HibernateException(
				String.format(
						"cannot find an accessor [%s] on type [%s]",
						name,
						containerClazz.getName()
				)
		);
	}

	private static Constructor<?> findConstructor(Class<?> clazz) {
		try {
			return clazz.getDeclaredConstructor();
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static class CloningPropertyCall implements Callable<String[]> {

		private final String[] propertyNames;

		private CloningPropertyCall(String[] propertyNames) {
			this.propertyNames = propertyNames;
		}

		@Override
		public String[] call() {
			return propertyNames.clone();
		}
	}

	@Override
	public @Nullable Enhancer getEnhancer(EnhancementContext enhancementContext) {
		return new EnhancerImpl( enhancementContext, byteBuddyState );
	}

	/**
	 * Similar to {@link #getEnhancer(EnhancementContext)} but intended for advanced users who wish
	 * to customize how ByteBuddy is locating the class files and caching the types.
	 * Used in Quarkus.
	 * @param enhancementContext
	 * @param classLocator
	 * @return
	 */
	public @Nullable Enhancer getEnhancer(EnhancementContext enhancementContext, EnhancerClassLocator classLocator) {
		return new EnhancerImpl( enhancementContext, byteBuddyState, classLocator );
	}

	@Override
	public void resetCaches() {
		byteBuddyState.clearState();
	}

}
