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

public class GetPropertyNames implements ByteCodeAppender {

	private final String[] propertyNames;
	private final EnhancerImplConstants constants;

	public GetPropertyNames(String[] propertyNames, EnhancerImplConstants constants) {
		this.propertyNames = propertyNames;
		this.constants = constants;
	}

	@Override
	public Size apply(
			MethodVisitor methodVisitor,
			Implementation.Context implementationContext,
			MethodDescription instrumentedMethod) {
		methodVisitor.visitLdcInsn( propertyNames.length );
		methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, constants.internalName_String );
		for ( int i = 0; i < propertyNames.length; i++ ) {
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitLdcInsn( i );
			methodVisitor.visitLdcInsn( propertyNames[i] );
			methodVisitor.visitInsn( Opcodes.AASTORE );
		}
		methodVisitor.visitInsn( Opcodes.ARETURN );
		return new Size( 4, instrumentedMethod.getStackSize() + 1 );
	}
}
