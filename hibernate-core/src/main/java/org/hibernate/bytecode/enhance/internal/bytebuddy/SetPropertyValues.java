/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveUnboxingDelegate;
import net.bytebuddy.implementation.bytecode.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import jakarta.annotation.Nonnull;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;

import static java.lang.reflect.Modifier.isFinal;

public class SetPropertyValues implements ByteCodeAppender {

	private final Class<?> clazz;
	private final String[] propertyNames;
	private final Member[] setters;
	private final boolean enhanced;
	private final String internalClazzName;
	private final EnhancerImplConstants constants;

	public SetPropertyValues(Class<?> clazz, String[] propertyNames, Member[] setters, EnhancerImplConstants constants) {
		this.clazz = clazz;
		this.propertyNames = propertyNames;
		this.setters = setters;
		this.enhanced = Managed.class.isAssignableFrom( clazz );
		this.internalClazzName = Type.getInternalName( clazz );
		this.constants = constants;
	}

	@Override
	public Size apply(
			MethodVisitor methodVisitor,
			Implementation.Context implementationContext,
			MethodDescription instrumentedMethod) {
		final boolean persistentAttributeInterceptable =
				PersistentAttributeInterceptable.class.isAssignableFrom( clazz );
		final boolean compositeOwner = CompositeOwner.class.isAssignableFrom( clazz );
		final List<TypeDefinition> baseLocals;
		if ( persistentAttributeInterceptable ) {
			// Extract interceptor once and store in local 3
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, internalClazzName );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKEVIRTUAL,
					internalClazzName,
					"$$_hibernate_getInterceptor",
					constants.methodDescriptor_getInterceptor,
					false
			);
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitTypeInsn(
					Opcodes.INSTANCEOF,
					constants.internalName_BytecodeLazyAttributeInterceptor
			);

			final var interceptorFalseLabel = new Label();
			methodVisitor.visitJumpInsn( Opcodes.IFEQ, interceptorFalseLabel );

			methodVisitor.visitTypeInsn(
					Opcodes.CHECKCAST,
					constants.internalName_BytecodeLazyAttributeInterceptor
			);
			methodVisitor.visitVarInsn( Opcodes.ASTORE, 3 );

			final var interceptorEndLabel = new Label();
			methodVisitor.visitJumpInsn( Opcodes.GOTO, interceptorEndLabel );

			methodVisitor.visitLabel( interceptorFalseLabel );
			implementationContext.getFrameGeneration().full(
					methodVisitor,
					constants.INTERFACES_for_PersistentAttributeInterceptor,
					List.of(
							implementationContext.getInstrumentedType(),
							constants.TypeObject,
							constants.Type_Array_Object
					)
			);
			methodVisitor.visitInsn( Opcodes.POP );
			methodVisitor.visitInsn( Opcodes.ACONST_NULL );
			methodVisitor.visitVarInsn( Opcodes.ASTORE, 3 );

			methodVisitor.visitLabel( interceptorEndLabel );
			baseLocals = List.of(
					implementationContext.getInstrumentedType(),
					constants.TypeObject,
					constants.Type_Array_Object,
					constants.TypeBytecodeLazyAttributeInterceptor
			);
			implementationContext.getFrameGeneration().full(
					methodVisitor,
					List.of(),
					baseLocals
			);
		}
		else {
			baseLocals = List.of(
					implementationContext.getInstrumentedType(),
					constants.TypeObject,
					constants.Type_Array_Object
			);
		}
		Label currentLabel = null;
		Label nextLabel = new Label();
		for ( int index = 0; index < setters.length; index++ ) {
			final Member setterMember = setters[index];
			if ( setterMember == BytecodeProviderImpl.EMBEDDED_MEMBER ) {
				// The embedded property access does a no-op
				continue;
			}
			if ( currentLabel != null ) {
				methodVisitor.visitLabel( currentLabel );
				implementationContext.getFrameGeneration().same(
						methodVisitor,
						instrumentedMethod.getParameters().asTypeList()
				);
			}
			if ( setterMember instanceof DelegatingAccessorMember dm ) {
				if ( enhanced ) {
					// UNFETCHED check: both references consumed by IF_ACMPEQ
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );
					methodVisitor.visitLdcInsn( index );
					methodVisitor.visitInsn( Opcodes.AALOAD );
					methodVisitor.visitFieldInsn(
							Opcodes.GETSTATIC,
							constants.internalName_LazyPropertyInitializer,
							"UNFETCHED_PROPERTY",
							constants.Serializable_TYPE_DESCRIPTOR
					);
					methodVisitor.visitJumpInsn( Opcodes.IF_ACMPEQ, nextLabel );
				}

				// writer.set(entity, value)
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
				methodVisitor.visitFieldInsn(
						Opcodes.GETFIELD,
						implementationContext.getInstrumentedType().getInternalName(),
						dm.getFieldName(),
						constants.descriptor_HibernateAccessorValueWriter
				);
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );
				methodVisitor.visitLdcInsn( index );
				methodVisitor.visitInsn( Opcodes.AALOAD );
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKEINTERFACE,
						constants.internalName_HibernateAccessorValueWriter,
						"set",
						constants.methodDescriptor_ValueWriter_set,
						true
				);

				if ( enhanced ) {
					boolean alreadyHasFrame = false;

					// CompositeTracker check
					final Class<?> delegateType = delegateMemberType( dm.getMember() );
					if ( compositeOwner && !isFinal( delegateType.getModifiers() ) ) {
						methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );
						methodVisitor.visitLdcInsn( index );
						methodVisitor.visitInsn( Opcodes.AALOAD );
						methodVisitor.visitInsn( Opcodes.DUP );
						final Label ctFalseLabel = new Label();
						methodVisitor.visitTypeInsn(
								Opcodes.INSTANCEOF, constants.internalName_CompositeTracker
						);
						methodVisitor.visitJumpInsn( Opcodes.IFEQ, ctFalseLabel );
						methodVisitor.visitTypeInsn(
								Opcodes.CHECKCAST, constants.internalName_CompositeTracker
						);
						methodVisitor.visitLdcInsn( propertyNames[index] );
						methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
						methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, internalClazzName );
						methodVisitor.visitMethodInsn(
								Opcodes.INVOKEINTERFACE,
								constants.internalName_CompositeTracker,
								"$$_hibernate_setOwner",
								constants.methodDescriptor_SetOwner,
								true
						);
						final var ctEndLabel = new Label();
						methodVisitor.visitJumpInsn( Opcodes.GOTO, ctEndLabel );
						methodVisitor.visitLabel( ctFalseLabel );
						implementationContext.getFrameGeneration().full(
								methodVisitor,
								List.of( constants.TypeObject ),
								baseLocals
						);
						methodVisitor.visitInsn( Opcodes.POP );
						methodVisitor.visitLabel( ctEndLabel );
						implementationContext.getFrameGeneration()
								.same( methodVisitor, instrumentedMethod.getParameters().asTypeList() );
						alreadyHasFrame = true;
					}

					if ( persistentAttributeInterceptable ) {
						methodVisitor.visitVarInsn( Opcodes.ALOAD, 3 );
						final var skipLabel = new Label();
						methodVisitor.visitJumpInsn( Opcodes.IFNULL, skipLabel );
						methodVisitor.visitVarInsn( Opcodes.ALOAD, 3 );
						methodVisitor.visitLdcInsn( propertyNames[index] );
						methodVisitor.visitMethodInsn(
								Opcodes.INVOKEINTERFACE,
								constants.internalName_BytecodeLazyAttributeInterceptor,
								"attributeInitialized",
								constants.methodDescriptor_attributeInitialized,
								true
						);
						methodVisitor.visitLabel( skipLabel );
						implementationContext.getFrameGeneration()
								.same( methodVisitor, instrumentedMethod.getParameters().asTypeList() );
						alreadyHasFrame = true;
					}

					if ( alreadyHasFrame ) {
						methodVisitor.visitLabel( nextLabel );
						currentLabel = null;
					}
					else {
						currentLabel = nextLabel;
					}
					nextLabel = new Label();
				}
				continue;
			}
			// Push entity on stack
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, internalClazzName );
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
						constants.internalName_LazyPropertyInitializer,
						"UNFETCHED_PROPERTY",
						constants.Serializable_TYPE_DESCRIPTOR
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
						List.of(
								TypeDescription.ForLoadedType.of( clazz ),
								constants.TypeObject
						),
						baseLocals
				);
			}
			final var type = setterTypee( setterMember );
			if ( type.isPrimitive() ) {
				PrimitiveUnboxingDelegate.forReferenceType( constants.TypeObject )
						.assignUnboxedTo(
								new TypeDescription.Generic.OfNonGenericType.ForLoadedType( type ),
								ReferenceTypeAwareAssigner.INSTANCE,
								Assigner.Typing.DYNAMIC
						)
						.apply( methodVisitor, implementationContext );
			}
			else {
				methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( type ) );
			}
			visitSetter( methodVisitor, setterMember, type );
			if ( enhanced ) {
				final boolean compositeTracker = CompositeTracker.class.isAssignableFrom( type );
				boolean alreadyHasFrame = false;
				// The composite owner check and setting only makes sense if
				//  * the value type is a composite tracker
				//  * a value subtype can be a composite tracker
				//
				// Final classes that don't already implement the interface never need to be checked.
				// This helps a bit with common final types which otherwise would have to be checked a lot.
				if ( compositeOwner && (compositeTracker || !isFinal( type.getModifiers() )) ) {
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
						compositeTrackerType = constants.internalName_CompositeTracker;
						// If we don't know for sure, we do an instanceof check
						methodVisitor.visitTypeInsn( Opcodes.INSTANCEOF, compositeTrackerType );
						isInterface = true;
						methodVisitor.visitJumpInsn( Opcodes.IFEQ, compositeTrackerFalseLabel );
					}

					// Load the tracker on which we will call $$_hibernate_setOwner
					methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, compositeTrackerType );
					methodVisitor.visitLdcInsn( propertyNames[index] );
					// Load the owner and cast it to the owner class, as we know it implements CompositeOwner
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
					methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, internalClazzName );
					// Invoke the method to set the owner
					methodVisitor.visitMethodInsn(
							isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
							compositeTrackerType,
							"$$_hibernate_setOwner",
							constants.methodDescriptor_SetOwner,
							isInterface
					);

					// Skip the cleanup
					final var compositeTrackerEndLabel = new Label();
					methodVisitor.visitJumpInsn( Opcodes.GOTO, compositeTrackerEndLabel );

					// Here is the cleanup section for the false branch
					methodVisitor.visitLabel( compositeTrackerFalseLabel );
					// We still have the duplicated value on the stack
					implementationContext.getFrameGeneration().full(
							methodVisitor,
							List.of(
									constants.TypeObject
							),
							baseLocals
					);
					// Pop that duplicated property value from the stack
					methodVisitor.visitInsn( Opcodes.POP );

					// Clean stack after the if block
					methodVisitor.visitLabel( compositeTrackerEndLabel );
					implementationContext.getFrameGeneration()
							.same( methodVisitor, instrumentedMethod.getParameters().asTypeList() );
					alreadyHasFrame = true;
				}
				if ( persistentAttributeInterceptable ) {
					// Use the interceptor stored in local 3 at the top of the method
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 3 );
					final var skipLabel = new Label();
					methodVisitor.visitJumpInsn( Opcodes.IFNULL, skipLabel );
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 3 );
					methodVisitor.visitLdcInsn( propertyNames[index] );
					methodVisitor.visitMethodInsn(
							Opcodes.INVOKEINTERFACE,
							constants.internalName_BytecodeLazyAttributeInterceptor,
							"attributeInitialized",
							constants.methodDescriptor_attributeInitialized,
							true
					);
					methodVisitor.visitLabel( skipLabel );
					implementationContext.getFrameGeneration()
							.same( methodVisitor, instrumentedMethod.getParameters().asTypeList() );
					alreadyHasFrame = true;
				}

				if ( alreadyHasFrame ) {
					// Usually, the currentLabel is visited as well generating a frame,
					// but if a frame was already generated, only visit the label here,
					// otherwise two frames for the same bytecode index are generated,
					// which is wrong and will produce an error when the JDK ClassFile API is used
					methodVisitor.visitLabel( nextLabel );
					currentLabel = null;
				}
				else {
					currentLabel = nextLabel;
				}
				nextLabel = new Label();
			}
		}
		if ( currentLabel != null ) {
			methodVisitor.visitLabel( currentLabel );
			implementationContext.getFrameGeneration()
					.same( methodVisitor, instrumentedMethod.getParameters().asTypeList() );
		}
		methodVisitor.visitInsn( Opcodes.RETURN );
		return new Size( 4, instrumentedMethod.getStackSize() + ( persistentAttributeInterceptable ? 1 : 0 ) );
	}

	private static void visitSetter(MethodVisitor methodVisitor, Member setterMember, Class<?> type) {
		if ( setterMember instanceof Method setter ) {
			final var declaringClass = setter.getDeclaringClass();
			methodVisitor.visitMethodInsn(
					declaringClass.isInterface()
							? Opcodes.INVOKEINTERFACE
							: Opcodes.INVOKEVIRTUAL,
					Type.getInternalName( declaringClass ),
					setter.getName(),
					Type.getMethodDescriptor( setter ),
					declaringClass.isInterface()
			);
			final var returnType = setter.getReturnType();
			if ( returnType != void.class ) {
				// Setters could return something which we have to ignore
				switch ( returnType.getTypeName() ) {
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
		else if ( setterMember instanceof Field field ) {
			methodVisitor.visitFieldInsn(
					Opcodes.PUTFIELD,
					Type.getInternalName( field.getDeclaringClass() ),
					field.getName(),
					Type.getDescriptor( type )
			);
		}
		else if ( setterMember instanceof ForeignPackageMember foreignPackageMember ) {
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
	}

	private static @Nonnull Class<?> delegateMemberType(Member member) {
		if ( member instanceof Field field ) {
			return field.getType();
		}
		else if ( member instanceof Method method ) {
			return method.getParameterTypes()[0];
		}
		return Object.class;
	}

	private static @Nonnull Class<?> setterTypee(Member setterMember) {
		if ( setterMember instanceof Method setter ) {
			return setter.getParameterTypes()[0];
		}
		else if ( setterMember instanceof Field field ) {
			return field.getType();
		}
		else if ( setterMember instanceof ForeignPackageMember foreignPackageMember ) {
			final var underlyingMember = foreignPackageMember.getMember();
			if ( underlyingMember instanceof Method setter ) {
				return setter.getParameterTypes()[0];
			}
			else if ( underlyingMember instanceof Field field ) {
				return field.getType();
			}
			else {
				throw new AssertionError( "Unknown underlying member type" );
			}
		}
		else {
			throw new AssertionError( "Unknown setter member type" );
		}
	}
}
