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
import org.hibernate.AssertionFailure;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class GetFieldOnArgument implements ByteCodeAppender {

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
		final var declaringClass = getterMember.getDeclaringClass();
		final Class<?> type;
		if ( getterMember instanceof Method getter ) {
			type = getter.getReturnType();
			methodVisitor.visitMethodInsn(
					declaringClass.isInterface()
							? Opcodes.INVOKEINTERFACE
							: Opcodes.INVOKEVIRTUAL,
					Type.getInternalName( declaringClass ),
					getter.getName(),
					Type.getMethodDescriptor( getter ),
					declaringClass.isInterface()
			);
		}
		else if ( getterMember instanceof Field getter ) {
			type = getter.getType();
			methodVisitor.visitFieldInsn(
					Opcodes.GETFIELD,
					Type.getInternalName( declaringClass ),
					getter.getName(),
					Type.getDescriptor( type )
			);
		}
		else {
			throw new AssertionFailure( "Unknown getter member type" );
		}
		methodVisitor.visitInsn( getReturnOpCode( type ) );
		return new Size( 2, instrumentedMethod.getStackSize() );
	}

	private int getReturnOpCode(Class<?> type) {
		return type.isPrimitive()
				? switch ( type.getTypeName() ) {
					case "long" -> Opcodes.LRETURN;
					case "float" -> Opcodes.FRETURN;
					case "double" -> Opcodes.DRETURN;
					default -> Opcodes.IRETURN;
				}
				: Opcodes.ARETURN;
	}
}
