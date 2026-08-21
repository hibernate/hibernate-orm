/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

/**
 * Emits a constructor that calls {@code super()} and then stores each element
 * of the typed array parameter into the corresponding {@code delegate$N}
 * instance field. The array is already typed (e.g. {@code HibernateAccessorValueReader[]})
 * so no per-element casts are needed.
 */
public class DelegateConstructorAppender implements ByteCodeAppender {

	private final int delegateCount;
	private final String fieldDescriptor;

	public DelegateConstructorAppender(int delegateCount, Class<?> fieldType) {
		this.delegateCount = delegateCount;
		this.fieldDescriptor = Type.getDescriptor( fieldType );
	}

	@Override
	public Size apply(
			MethodVisitor methodVisitor,
			Implementation.Context implementationContext,
			MethodDescription instrumentedMethod) {
		final String instrumentedType = implementationContext.getInstrumentedType().getInternalName();
		final String superType = implementationContext.getInstrumentedType()
				.getSuperClass().asErasure().getInternalName();

		// super()
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL, superType, "<init>", "()V", false );

		// this.delegate$N = args[N]
		for ( int i = 0; i < delegateCount; i++ ) {
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			methodVisitor.visitLdcInsn( i );
			methodVisitor.visitInsn( Opcodes.AALOAD );
			methodVisitor.visitFieldInsn(
					Opcodes.PUTFIELD, instrumentedType, "delegate$" + i, fieldDescriptor
			);
		}

		methodVisitor.visitInsn( Opcodes.RETURN );
		return new Size( 3, instrumentedMethod.getStackSize() );
	}
}
