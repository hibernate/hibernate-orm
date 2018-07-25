/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;

import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveBoxingDelegate;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveUnboxingDelegate;
import net.bytebuddy.implementation.bytecode.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class BytecodeProviderImpl implements BytecodeProvider {

	private static final String INSTANTIATOR_PROXY_NAMING_SUFFIX = "HibernateInstantiator";
	private static final String OPTIMIZER_PROXY_NAMING_SUFFIX = "HibernateAccessOptimizer";
	private static final ElementMatcher.Junction newInstanceMethodName = ElementMatchers.named( "newInstance" );
	private static final ElementMatcher.Junction getPropertyValuesMethodName = ElementMatchers.named( "getPropertyValues" );
	private static final ElementMatcher.Junction setPropertyValuesMethodName = ElementMatchers.named( "setPropertyValues" );
	private static final ElementMatcher.Junction getPropertyNamesMethodName = ElementMatchers.named( "getPropertyNames" );

	private final ByteBuddyState byteBuddyState;

	private final ByteBuddyProxyHelper byteBuddyProxyHelper;

	public BytecodeProviderImpl() {
		this.byteBuddyState = new ByteBuddyState();
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
		final Class fastClass;
		if ( !clazz.isInterface() && !Modifier.isAbstract( clazz.getModifiers() ) ) {
			// we only provide a fast class instantiator if the class can be instantiated
			final Constructor<?> constructor = findConstructor( clazz );

			fastClass = byteBuddyState.load( clazz, byteBuddy -> byteBuddy
					.with( new NamingStrategy.SuffixingRandom( INSTANTIATOR_PROXY_NAMING_SUFFIX,
							new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( clazz.getName() ) ) )
					.subclass( ReflectionOptimizer.InstantiationOptimizer.class )
					.method( newInstanceMethodName )
							.intercept( MethodCall.construct( constructor ) )
			);
		}
		else {
			fastClass = null;
		}

		final Method[] getters = new Method[getterNames.length];
		final Method[] setters = new Method[setterNames.length];
		findAccessors( clazz, getterNames, setterNames, types, getters, setters );

		final Class bulkAccessor = byteBuddyState.load( clazz, byteBuddy -> byteBuddy
				.with( new NamingStrategy.SuffixingRandom( OPTIMIZER_PROXY_NAMING_SUFFIX,
						new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( clazz.getName() ) ) )
				.subclass( ReflectionOptimizer.AccessOptimizer.class )
				.method( getPropertyValuesMethodName )
						.intercept( new Implementation.Simple( new GetPropertyValues( clazz, getters ) ) )
				.method( setPropertyValuesMethodName )
						.intercept( new Implementation.Simple( new SetPropertyValues( clazz, setters ) ) )
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

	public ByteBuddyProxyHelper getByteBuddyProxyHelper() {
		return byteBuddyProxyHelper;
	}

	private static class GetPropertyValues implements ByteCodeAppender {

		private final Class clazz;

		private final Method[] getters;

		public GetPropertyValues(Class clazz, Method[] getters) {
			this.clazz = clazz;
			this.getters = getters;
		}

		@Override
		public Size apply(
				MethodVisitor methodVisitor,
				Implementation.Context implementationContext,
				MethodDescription instrumentedMethod) {
			methodVisitor.visitLdcInsn( getters.length );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( Object.class ) );
			int index = 0;
			for ( Method getter : getters ) {
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( index++ );
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
				methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( clazz ) );
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKEVIRTUAL,
						Type.getInternalName( clazz ),
						getter.getName(),
						Type.getMethodDescriptor( getter ),
						false
				);
				if ( getter.getReturnType().isPrimitive() ) {
					PrimitiveBoxingDelegate.forPrimitive( new TypeDescription.ForLoadedType( getter.getReturnType() ) )
							.assignBoxedTo(
									TypeDescription.Generic.OBJECT,
									ReferenceTypeAwareAssigner.INSTANCE,
									Assigner.Typing.STATIC
							)
							.apply( methodVisitor, implementationContext );
				}
				methodVisitor.visitInsn( Opcodes.AASTORE );
			}
			methodVisitor.visitInsn( Opcodes.ARETURN );
			return new Size( 6, instrumentedMethod.getStackSize() );
		}
	}

	private static class SetPropertyValues implements ByteCodeAppender {

		private final Class clazz;

		private final Method[] setters;

		public SetPropertyValues(Class clazz, Method[] setters) {
			this.clazz = clazz;
			this.setters = setters;
		}

		@Override
		public Size apply(
				MethodVisitor methodVisitor,
				Implementation.Context implementationContext,
				MethodDescription instrumentedMethod) {
			int index = 0;
			for ( Method setter : setters ) {
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
				methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( clazz ) );
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );
				methodVisitor.visitLdcInsn( index++ );
				methodVisitor.visitInsn( Opcodes.AALOAD );
				if ( setter.getParameterTypes()[0].isPrimitive() ) {
					PrimitiveUnboxingDelegate.forReferenceType( TypeDescription.Generic.OBJECT )
							.assignUnboxedTo(
									new TypeDescription.Generic.OfNonGenericType.ForLoadedType( setter.getParameterTypes()[0] ),
									ReferenceTypeAwareAssigner.INSTANCE,
									Assigner.Typing.DYNAMIC
							)
							.apply( methodVisitor, implementationContext );
				}
				else {
					methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( setter.getParameterTypes()[0] ) );
				}
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKEVIRTUAL,
						Type.getInternalName( clazz ),
						setter.getName(),
						Type.getMethodDescriptor( setter ),
						false
				);
			}
			methodVisitor.visitInsn( Opcodes.RETURN );
			return new Size( 4, instrumentedMethod.getStackSize() );
		}
	}

	private static void findAccessors(
			Class clazz,
			String[] getterNames,
			String[] setterNames,
			Class[] types,
			Method[] getters,
			Method[] setters) {
		final int length = types.length;
		if ( setterNames.length != length || getterNames.length != length ) {
			throw new BulkAccessorException( "bad number of accessors" );
		}

		final Class[] getParam = new Class[0];
		final Class[] setParam = new Class[1];
		for ( int i = 0; i < length; i++ ) {
			if ( getterNames[i] != null ) {
				final Method getter = findAccessor( clazz, getterNames[i], getParam, i );
				if ( getter.getReturnType() != types[i] ) {
					throw new BulkAccessorException( "wrong return type: " + getterNames[i], i );
				}

				getters[i] = getter;
			}

			if ( setterNames[i] != null ) {
				setParam[0] = types[i];
				setters[i] = findAccessor( clazz, setterNames[i], setParam, i );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Method findAccessor(Class clazz, String name, Class[] params, int index)
			throws BulkAccessorException {
		try {
			final Method method = clazz.getDeclaredMethod( name, params );
			if ( Modifier.isPrivate( method.getModifiers() ) ) {
				throw new BulkAccessorException( "private property", index );
			}

			return method;
		}
		catch (NoSuchMethodException e) {
			throw new BulkAccessorException( "cannot find an accessor", index );
		}
	}

	private static Constructor<?> findConstructor(Class clazz) {
		try {
			return clazz.getDeclaredConstructor();
		}
		catch (NoSuchMethodException e) {
			throw new HibernateException( e );
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
	public Enhancer getEnhancer(EnhancementContext enhancementContext) {
		return new EnhancerImpl( enhancementContext, byteBuddyState );
	}

	@Override
	public void resetCaches() {
		byteBuddyState.clearState();
	}

}
