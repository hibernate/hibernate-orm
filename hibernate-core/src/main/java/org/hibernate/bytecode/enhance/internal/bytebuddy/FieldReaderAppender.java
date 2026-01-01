/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Objects;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

import static net.bytebuddy.ClassFileVersion.JAVA_V6;
import static org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.capitalize;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.INTERCEPTOR_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX;

abstract class FieldReaderAppender implements ByteCodeAppender {

	protected final TypeDescription managedCtClass;

	protected final AnnotatedFieldDescription persistentField;

	protected final FieldDescription.InDefinedShape persistentFieldAsDefined;

	protected final EnhancerImplConstants constants;

	private FieldReaderAppender(TypeDescription managedCtClass, AnnotatedFieldDescription persistentField, EnhancerImplConstants constants) {
		this.managedCtClass = managedCtClass;
		this.persistentField = persistentField;
		this.persistentFieldAsDefined = persistentField.asDefined();
		this.constants = constants;
	}

	static ByteCodeAppender of(TypeDescription managedCtClass, AnnotatedFieldDescription persistentField, EnhancerImplConstants constants) {
		return persistentField.isVisibleTo( managedCtClass )
				? new FieldWriting( managedCtClass, persistentField, constants )
				: new MethodDispatching( managedCtClass, persistentField, constants );
	}

	@Override
	public Size apply(
			MethodVisitor methodVisitor,
			Implementation.Context implementationContext,
			MethodDescription instrumentedMethod) {
		final var type = persistentFieldAsDefined.getType();
		final var erasure = type.asErasure();
		final var dispatcherType = type.isPrimitive() ? erasure : TypeDescription.OBJECT;
		// From `PersistentAttributeTransformer`:
		//     Final fields will only be written to from the constructor,
		//     so there's no point trying to replace final field writes with a method call.
		// as a result if a field is final, then there will be no write method, and we don't want to have this block:
		if ( !persistentField.asDefined().isFinal() ) {
			// if ( this.$$_hibernate_getInterceptor() != null )
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKEVIRTUAL,
					managedCtClass.getInternalName(),
					INTERCEPTOR_GETTER_NAME,
					constants.methodDescriptor_getInterceptor,
					false
			);
			final var skip = new Label();
			methodVisitor.visitJumpInsn( Opcodes.IFNULL, skip );
			// this (for field write)
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			// this.$$_hibernate_getInterceptor();
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKEVIRTUAL,
					managedCtClass.getInternalName(),
					INTERCEPTOR_GETTER_NAME,
					constants.methodDescriptor_getInterceptor,
					false
			);
			// .readXXX( self, fieldName, field );
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitLdcInsn( persistentFieldAsDefined.getName() );
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );

			fieldRead( methodVisitor );
			final String descriptor = dispatcherType.getDescriptor();
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKEINTERFACE,
					constants.internalName_PersistentAttributeInterceptor,
					"read" + capitalize( dispatcherType.getSimpleName() ),
					Type.getMethodDescriptor(
							Type.getType( descriptor ),
							Type.getType( Object.class ),
							Type.getType( String.class ),
							Type.getType( descriptor )
					),
					true
			);
			// field = (cast) XXX
			if ( !dispatcherType.isPrimitive() ) {
				methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, erasure.getInternalName() );
			}
			fieldWrite( methodVisitor );
			// end if
			methodVisitor.visitLabel( skip );
			if ( implementationContext.getClassFileVersion().isAtLeast( JAVA_V6 ) ) {
				methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
			}
		}

		// return field
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		fieldRead( methodVisitor );
		final var persistentFieldType = persistentField.getType();
		if ( !persistentFieldType.isPrimitive() ) {
			final String internalName = persistentFieldType.asErasure().getInternalName();
			if ( !internalName.equals( erasure.getInternalName() ) ) {
				methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, internalName );
			}
		}
		methodVisitor.visitInsn( Type.getType( erasure.getDescriptor() )
				.getOpcode( Opcodes.IRETURN ) );
		return new Size( 4 + type.getStackSize().getSize(),
				instrumentedMethod.getStackSize() );
	}

	protected abstract void fieldRead(MethodVisitor methodVisitor);

	protected abstract void fieldWrite(MethodVisitor methodVisitor);

	private static class FieldWriting extends FieldReaderAppender {

		private FieldWriting(
				TypeDescription managedCtClass,
				AnnotatedFieldDescription persistentField,
				EnhancerImplConstants constants) {
			super( managedCtClass, persistentField, constants );
		}

		@Override
		protected void fieldRead(MethodVisitor methodVisitor) {
			methodVisitor.visitFieldInsn(
					Opcodes.GETFIELD,
					persistentFieldAsDefined.getDeclaringType().asErasure().getInternalName(),
					persistentFieldAsDefined.getInternalName(),
					persistentFieldAsDefined.getDescriptor()
			);
		}

		@Override
		protected void fieldWrite(MethodVisitor methodVisitor) {
			methodVisitor.visitFieldInsn(
					Opcodes.PUTFIELD,
					persistentFieldAsDefined.getDeclaringType().asErasure().getInternalName(),
					persistentFieldAsDefined.getInternalName(),
					persistentFieldAsDefined.getDescriptor()
			);
		}
	}

	private static class MethodDispatching extends FieldReaderAppender {

		private final String internalName;
		private final String descriptor;
		private final String fieldName;

		private MethodDispatching(
				TypeDescription managedCtClass,
				AnnotatedFieldDescription persistentField,
				EnhancerImplConstants constants) {
			super( managedCtClass, persistentField, constants );
			internalName = managedCtClass.getSuperClass().asErasure().getInternalName();
			descriptor = persistentFieldAsDefined.getType().asErasure().getDescriptor();
			fieldName = persistentFieldAsDefined.getName();
		}

		@Override
		protected void fieldRead(MethodVisitor methodVisitor) {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					internalName,
					PERSISTENT_FIELD_READER_PREFIX + fieldName,
					Type.getMethodDescriptor( Type.getType( descriptor ) ),
					false
			);
		}

		@Override
		protected void fieldWrite(MethodVisitor methodVisitor) {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					internalName,
					PERSISTENT_FIELD_WRITER_PREFIX + fieldName,
					Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( descriptor ) ),
					false
			);
		}
	}

	@Override
	public boolean equals(final Object object) {
		if ( this == object ) {
			return true;
		}
		if ( !(object instanceof FieldReaderAppender that) ) {
			return false;
		}
		else {
			return Objects.equals( managedCtClass, that.managedCtClass )
				&& Objects.equals( persistentField, that.persistentField )
				&& Objects.equals( persistentFieldAsDefined, that.persistentFieldAsDefined );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( managedCtClass, persistentField, persistentFieldAsDefined );
	}

}
