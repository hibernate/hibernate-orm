/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.BridgeMembersClassInfo;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerClassLocator;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImplConstants;
import org.hibernate.bytecode.enhance.internal.bytebuddy.ForeignPackageMember;
import org.hibernate.bytecode.enhance.internal.bytebuddy.GetFieldOnArgument;
import org.hibernate.bytecode.enhance.internal.bytebuddy.GetPropertyNames;
import org.hibernate.bytecode.enhance.internal.bytebuddy.GetPropertyValues;
import org.hibernate.bytecode.enhance.internal.bytebuddy.NameEncodeHelper;
import org.hibernate.bytecode.enhance.internal.bytebuddy.SetFieldOnArgument;
import org.hibernate.bytecode.enhance.internal.bytebuddy.SetPropertyValues;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
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
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.jar.asm.Opcodes;
import jakarta.annotation.Nullable;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isPublic;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

public class BytecodeProviderImpl implements BytecodeProvider {

	private static final String INSTANTIATOR_PROXY_NAMING_SUFFIX = "$HibernateInstantiator";
	private static final String OPTIMIZER_PROXY_NAMING_SUFFIX = "HibernateAccessOptimizer";
	private static final String OPTIMIZER_PROXY_BRIDGE_NAMING_SUFFIX = "$HibernateAccessOptimizerBridge";

	public static final Member EMBEDDED_MEMBER = new Member() {
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
	private final EnhancerImplConstants constants;

	private final ByteBuddyProxyHelper byteBuddyProxyHelper;

	/**
	 * Constructs a ByteBuddy BytecodeProvider instance which attempts to auto-detect the target JVM version
	 * from the currently running one, with a fallback on Java 17.
	 */
	public BytecodeProviderImpl() {
		this( ClassFileVersion.ofThisVm( ClassFileVersion.JAVA_V17 ) );
	}

	/**
	 * Constructs a ByteBuddy BytecodeProvider instance which aims to produce code compatible
	 * with the specified target JVM version.
	 */
	public BytecodeProviderImpl(ClassFileVersion targetCompatibleJVM) {
		this.byteBuddyState = new ByteBuddyState( targetCompatibleJVM );
		this.byteBuddyProxyHelper = new ByteBuddyProxyHelper( byteBuddyState );
		this.constants = byteBuddyState.getEnhancerConstants();
	}

	@Nonnull
	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new ProxyFactoryFactoryImpl( byteBuddyState, byteBuddyProxyHelper );
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer(
			@Nonnull final Class<?> clazz,
			@Nonnull final String[] getterNames,
			@Nonnull final String[] setterNames,
			@Nonnull final Class<?>[] types) {
		final var fastClass = getFastClass( clazz );
		final var getters = new Method[getterNames.length];
		final var setters = new Method[setterNames.length];
		try {
			findAccessors( clazz, getterNames, setterNames, types, getters, setters );
		}
		catch (InvalidPropertyAccessorException ex) {
			CORE_LOGGER.unableToGenerateReflectionOptimizer( clazz.getName(), ex.getMessage() );
			return null;
		}

		final var bulkAccessor = getBulkAccessor( clazz, getterNames, getters, setters );

		try {
			return new ReflectionOptimizerImpl(
					fastClass == null ? null : (ReflectionOptimizer.InstantiationOptimizer) fastClass.newInstance(),
					(ReflectionOptimizer.AccessOptimizer) bulkAccessor.newInstance()
			);
		}
		catch (Exception exception) {
			throw new HibernateException( exception );
		}
	}

	@Nonnull
	private Class<?> getBulkAccessor(@Nonnull Class<?> clazz, @Nonnull String[] getterNames, Method[] getters, Method[] setters) {
		return byteBuddyState.load( clazz, byteBuddy -> byteBuddy
				.with( new NamingStrategy.SuffixingRandom(
						OPTIMIZER_PROXY_NAMING_SUFFIX,
						new NamingStrategy.Suffixing.BaseNameResolver.ForFixedValue( clazz.getName() )
				) )
				.subclass( constants.TypeObject )
				.implement( constants.INTERFACES_for_AccessOptimizer )
				.method( constants.getPropertyValuesMethodName )
				.intercept( new Implementation.Simple( new GetPropertyValues( clazz, getterNames, getters, constants ) ) )
				.method( constants.setPropertyValuesMethodName )
				.intercept( new Implementation.Simple( new SetPropertyValues( clazz, getterNames, setters, constants ) ) )
				.method( constants.getPropertyNamesMethodName )
				.intercept( new Implementation.Simple( new GetPropertyNames( getterNames, constants ) ) )
		);
	}

	@Override
	public @Nullable ReflectionOptimizer getReflectionOptimizer(@Nonnull Class<?> clazz, @Nonnull Map<String, PropertyAccess> propertyAccessMap) {
		final var fastClass = getFastClass( clazz );

		final var getters = new Member[propertyAccessMap.size()];
		final var setters = new Member[propertyAccessMap.size()];
		try {
			findAccessors( clazz, propertyAccessMap, getters, setters );
		}
		catch (InvalidPropertyAccessorException ex) {
			CORE_LOGGER.unableToGenerateReflectionOptimizer( clazz.getName(), ex.getMessage() );
			return null;
		}

		final var propertyNames = propertyAccessMap.keySet().toArray( new String[0] );
		final var superClass = determineAccessOptimizerSuperClass( clazz, propertyNames, getters, setters );
		final var bulkAccessor = getBulkAccessor( clazz, superClass, propertyNames, getters, setters );
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

	private Class<?> getBulkAccessor(@Nonnull Class<?> clazz, Class<?> superClass, String[] propertyNames, Member[] getters, Member[] setters) {
		final String className =
				clazz.getName() + "$" + OPTIMIZER_PROXY_NAMING_SUFFIX
					+ NameEncodeHelper.encodeName( propertyNames, getters, setters );
		if ( className.getBytes( StandardCharsets.UTF_8 ).length >= 0x10000 ) {
			// The JVM has a 64K byte limit on class name length,
			// so fallback to random name if encoding exceeds that
			return byteBuddyState.load( clazz, byteBuddy -> byteBuddy
					.with( new NamingStrategy.SuffixingRandom(
							OPTIMIZER_PROXY_NAMING_SUFFIX,
							new NamingStrategy.Suffixing.BaseNameResolver.ForFixedValue( clazz.getName() )
					) )
					.subclass( superClass )
					.implement( constants.INTERFACES_for_AccessOptimizer )
					.method( constants.getPropertyValuesMethodName )
					.intercept( new Implementation.Simple( new GetPropertyValues( clazz, propertyNames, getters, constants ) ) )
					.method( constants.setPropertyValuesMethodName )
					.intercept( new Implementation.Simple( new SetPropertyValues( clazz, propertyNames, setters, constants ) ) )
					.method( constants.getPropertyNamesMethodName )
					.intercept( new Implementation.Simple( new GetPropertyNames( propertyNames, constants ) ) )
			);
		}
		else {
			return byteBuddyState.load( clazz, className, (byteBuddy, namingStrategy) -> byteBuddy
					.with( namingStrategy )
					.subclass( superClass )
					.implement( constants.INTERFACES_for_AccessOptimizer )
					.method( constants.getPropertyValuesMethodName )
					.intercept( new Implementation.Simple( new GetPropertyValues( clazz, propertyNames, getters, constants ) ) )
					.method( constants.setPropertyValuesMethodName )
					.intercept( new Implementation.Simple( new SetPropertyValues( clazz, propertyNames, setters, constants ) ) )
					.method( constants.getPropertyNamesMethodName )
					.intercept( new Implementation.Simple( new GetPropertyNames( propertyNames, constants ) ) )
			);
		}
	}

	@Nullable
	private Class<?> getFastClass(@Nonnull Class<?> clazz) {
		if ( !clazz.isInterface() && !isAbstract( clazz.getModifiers() ) ) {
			// we only provide a fast class instantiator if the class can be instantiated
			final var constructor = findConstructor( clazz );
			if ( constructor == null || isPrivate( constructor.getModifiers() ) ) {
				// In the current implementation of the ReflectionOptimizer contract, we can't call private constructors
				// To support that, we have to inject a static factory method into the class during enhancement
				return null;
			}
			else {
				final String className = clazz.getName() + INSTANTIATOR_PROXY_NAMING_SUFFIX;
				return byteBuddyState.load( clazz, className, (byteBuddy, namingStrategy) -> byteBuddy
						.with( namingStrategy )
						.subclass( constants.TypeInstantiationOptimizer )
						.method( constants.newInstanceMethodName )
						.intercept( MethodCall.construct( constructor ) )
				);
			}
		}
		else {
			return null;
		}
	}

	private Class<?> determineAccessOptimizerSuperClass(Class<?> clazz, String[] propertyNames, Member[] getters, Member[] setters) {
		if ( clazz.isInterface() ) {
			return Object.class;
		}
		// generate access optimizer super classes for super classes that declare members requiring bridge methods
		//  each should declare protected static methods get_FIELDNAME(OWNER)/set_FIELDNAME(OWNER, TYPE)
		//  which should be called then from within GetPropertyValues/SetPropertyValues
		//  Since these super classes will be in the correct package, the package-private entity field access is fine
		final List<BridgeMembersClassInfo> bridgeMembersClassInfos = createBridgeMembersClassInfos( clazz, getters, setters, propertyNames );

		Class<?> superClass = Object.class;
		for ( int i = bridgeMembersClassInfos.size() - 1; i >= 0; i-- ) {
			final BridgeMembersClassInfo bridgeMembersClassInfo = bridgeMembersClassInfos.get( i );
			final Class<?> newSuperClass = superClass;

			final String className = bridgeMembersClassInfo.getClazz().getName() + OPTIMIZER_PROXY_BRIDGE_NAMING_SUFFIX + bridgeMembersClassInfo.encodeName();
			superClass = byteBuddyState.load(
					bridgeMembersClassInfo.getClazz(),
					className,
					(byteBuddy, namingStrategy) -> {
						DynamicType.Builder<?> builder = byteBuddy.with( namingStrategy ).subclass( newSuperClass );
						for ( var getter : bridgeMembersClassInfo.gettersIterable() ) {
							if ( !isPublic( getter.getModifiers() ) ) {
								final Class<?> getterType;
								if ( getter instanceof Field field ) {
									getterType = field.getType();
								}
								else if ( getter instanceof Method method ) {
									getterType = method.getReturnType();
								}
								else {
									throw new AssertionFailure( "Unexpected member" + getter );
								}

								builder = builder.defineMethod(
												"get_" + getter.getName(),
												TypeDescription.Generic.OfNonGenericType.ForLoadedType.of( getterType ),
												Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC
										)
										.withParameter( bridgeMembersClassInfo.getClazz() )
										.intercept( new Implementation.Simple( new GetFieldOnArgument( getter ) ) );
							}
						}
						for ( Member setter : bridgeMembersClassInfo.settersIterable() ) {
							if ( !isPublic( setter.getModifiers() ) ) {
								final Class<?> setterType;
								if ( setter instanceof Field field ) {
									setterType = field.getType();
								}
								else if ( setter instanceof Method method ) {
									setterType = method.getParameterTypes()[0];
								}
							else {
								throw new AssertionFailure( "Unexpected member" + setter );
							}

								builder = builder.defineMethod(
												"set_" + setter.getName(),
												TypeDescription.Generic.VOID,
												Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC
										)
										.withParameter( bridgeMembersClassInfo.getClazz() )
										.withParameter( setterType )
										.intercept( new Implementation.Simple( new SetFieldOnArgument( setter ) ) );
							}
						}

						return builder;
					}
			);

			for ( int j = 0; j < getters.length; j++ ) {
				final Member getter = getters[j];
				final Member setter = setters[j];
				if ( bridgeMembersClassInfo.containsGetter( getter ) && !isPublic( getter.getModifiers() ) ) {
					getters[j] = new ForeignPackageMember( superClass, getter );
				}
				if ( bridgeMembersClassInfo.containsSetter( setter ) && !isPublic( setter.getModifiers() ) ) {
					setters[j] = new ForeignPackageMember( superClass, setter );
				}
			}
		}

		return superClass;
	}

	private List<BridgeMembersClassInfo> createBridgeMembersClassInfos(
			Class<?> clazz,
			Member[] getters,
			Member[] setters,
			String[] propertyNames) {
		final List<BridgeMembersClassInfo> bridgeMembersClassInfos = new ArrayList<>();
		Class<?> c = clazz.getSuperclass();
		while (c != Object.class) {
			final BridgeMembersClassInfo bridgeMemberClassInfo = new BridgeMembersClassInfo( c );
			for ( int i = 0; i < getters.length; i++ ) {
				final Member getter = getters[i];
				final Member setter = setters[i];
				if ( getter.getDeclaringClass() == c && !isPublic( getter.getModifiers() )
						|| setter.getDeclaringClass() == c && !isPublic( setter.getModifiers() ) ) {
					bridgeMemberClassInfo.addGetter( getter );
					bridgeMemberClassInfo.addSetter( setter );
					bridgeMemberClassInfo.addProperty( propertyNames[i] );
				}
			}
			if ( !bridgeMemberClassInfo.propertyNamesIsEmpty() ) {
				bridgeMembersClassInfos.add( bridgeMemberClassInfo );
			}
			c = c.getSuperclass();
		}
		return bridgeMembersClassInfos;
	}

	public ByteBuddyProxyHelper getByteBuddyProxyHelper() {
		return byteBuddyProxyHelper;
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
				else if ( isPrivate( setters[i].getModifiers() ) ) {
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
			else if ( getter instanceof GetterFieldImpl getterField ) {
				getterMember = getterField.getField();
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
			else if ( setter instanceof SetterFieldImpl setterField ) {
				setterMember = setterField.getField();
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
			if ( isPrivate( getterMember.getModifiers() ) ) {
				throw new PrivateAccessorException( "private accessor [" + getterMember.getName() + "]" );
			}
			if ( isPrivate( setterMember.getModifiers() ) ) {
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
				final var method = clazz.getDeclaredMethod( name, params );
				if ( isPrivate( method.getModifiers() ) ) {
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

	@Override
	public @Nullable Enhancer getEnhancer(@Nonnull EnhancementContext enhancementContext) {
		return new EnhancerImpl( enhancementContext, byteBuddyState );
	}

	/**
	 * Similar to {@link #getEnhancer(EnhancementContext)} but intended for advanced users who wish
	 * to customize how ByteBuddy is locating the class files and caching the types.
	 * Used in Quarkus.
	 */
	public @Nullable Enhancer getEnhancer(
			@Nonnull EnhancementContext enhancementContext,
			@Nonnull EnhancerClassLocator classLocator) {
		return new EnhancerImpl( enhancementContext, byteBuddyState, classLocator );
	}

	@Override
	public void resetCaches() {
		byteBuddyState.clearState();
	}

}
