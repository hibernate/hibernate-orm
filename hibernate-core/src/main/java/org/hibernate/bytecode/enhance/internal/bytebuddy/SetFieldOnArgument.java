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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class SetFieldOnArgument implements ByteCodeAppender {

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
		if ( setterMember instanceof Method setter ) {
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
