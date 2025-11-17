/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveBoxingDelegate;
import net.bytebuddy.implementation.bytecode.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to generate bytecode for getting property values, used by BytecodeProviderImpl.
 * Moved into package org.hibernate.bytecode.enhance.internal.bytebuddy to benefit from the constants.
 */
public class GetPropertyValues implements ByteCodeAppender {

	private final String[] propertyNames;
	private final Member[] getters;
	private final boolean persistentAttributeInterceptable;
	private final String internalClazzName;
	private final EnhancerImplConstants constants;


	public GetPropertyValues(Class<?> clazz, String[] propertyNames, Member[] getters, EnhancerImplConstants constants) {
		this.propertyNames = propertyNames;
		this.getters = getters;
		this.persistentAttributeInterceptable = PersistentAttributeInterceptable.class.isAssignableFrom( clazz );
		this.internalClazzName = Type.getInternalName( clazz );
		this.constants = constants;
	}

	@Override
	public Size apply(
			MethodVisitor methodVisitor,
			Implementation.Context implementationContext,
			MethodDescription instrumentedMethod) {
		if ( persistentAttributeInterceptable ) {
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, internalClazzName );

			// Extract the interceptor
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKEVIRTUAL,
					internalClazzName,
					"$$_hibernate_getInterceptor",
					constants.methodDescriptor_getInterceptor,
					false
			);
			// Duplicate the interceptor on the stack and check if it implements LazyAttributeLoadingInterceptor
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitTypeInsn(
					Opcodes.INSTANCEOF,
					constants.internalName_LazyAttributeLoadingInterceptor
			);

			// Jump to the false label if the instanceof check fails
			final Label instanceofFalseLabel = new Label();
			methodVisitor.visitJumpInsn( Opcodes.IFEQ, instanceofFalseLabel );

			// Cast to the subtype, so we can mark the property as initialized
			methodVisitor.visitTypeInsn(
					Opcodes.CHECKCAST,
					constants.internalName_LazyAttributeLoadingInterceptor
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
					constants.INTERFACES_for_PersistentAttributeInterceptor,
					Arrays.asList(
							implementationContext.getInstrumentedType(),
							constants.TypeObject
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
							constants.TypeObject,
							constants.TypeLazyAttributeLoadingInterceptor
					)
			);
		}
		methodVisitor.visitLdcInsn( getters.length );
		methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, constants.internalName_Object );
		for ( int index = 0; index < getters.length; index++ ) {
			final Member getterMember = getters[index];
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitLdcInsn( index );

			final Label arrayStoreLabel = new Label();
			if ( getterMember == BytecodeProviderImpl.EMBEDDED_MEMBER ) {
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
							constants.internalName_LazyAttributeLoadingInterceptor,
							"isAttributeLoaded",
							constants.methodDescriptor_isAttributeLoaded,
							false
					);
					// If the attribute is loaded, jump to extraction
					methodVisitor.visitJumpInsn( Opcodes.IFNE, extractValueLabel );

					// Push LazyPropertyInitializer.UNFETCHED_PROPERTY on the stack
					methodVisitor.visitFieldInsn(
							Opcodes.GETSTATIC,
							constants.internalName_LazyPropertyInitializer,
							"UNFETCHED_PROPERTY",
							constants.Serializable_TYPE_DESCRIPTOR
					);
					// Jump to the label where we handle storing the unfetched property
					methodVisitor.visitJumpInsn( Opcodes.GOTO, arrayStoreLabel );

					// This is the end of the lazy check i.e. the start of extraction
					methodVisitor.visitLabel( extractValueLabel );
					implementationContext.getFrameGeneration().full(
							methodVisitor,
							Arrays.asList(
									constants.Type_Array_Object,
									constants.Type_Array_Object,
									constants.TypeIntegerPrimitive
							),
							Arrays.asList(
									implementationContext.getInstrumentedType(),
									constants.TypeObject,
									constants.TypeLazyAttributeLoadingInterceptor
							)
					);
				}

				// Load the entity to extract the property
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
				methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, internalClazzName );

				final Class<?> type;
				if ( getterMember instanceof Method getter ) {
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
				else if ( getterMember instanceof Field getter ) {
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
					if ( underlyingMember instanceof Method getter ) {
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
								constants.Type_Array_Object,
								constants.Type_Array_Object,
								constants.TypeIntegerPrimitive,
								constants.TypeObject
						),
						List.of(
								implementationContext.getInstrumentedType(),
								constants.TypeObject,
								constants.TypeLazyAttributeLoadingInterceptor
						)
				);
			}
			methodVisitor.visitInsn( Opcodes.AASTORE );
		}
		methodVisitor.visitInsn( Opcodes.ARETURN );
		return new Size( 6, instrumentedMethod.getStackSize() + 1 );
	}
}
