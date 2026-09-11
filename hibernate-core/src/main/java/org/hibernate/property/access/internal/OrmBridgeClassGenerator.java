/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.accessor.spi.CrossClassLoaderLookupBridge;

import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;

/**
 * Generates the bridge class bytecode used by {@link CrossClassLoaderLookupBridge}
 * to obtain a full-privilege {@link java.lang.invoke.MethodHandles.Lookup} in a
 * foreign classloader. Uses ByteBuddy's shaded ASM.
 *
 * @see CrossClassLoaderLookupBridge
 */
final class OrmBridgeClassGenerator {

	private OrmBridgeClassGenerator() {
	}

	static byte[] generate(String className) {
		final String internalName = className.replace( '.', '/' );
		final ClassWriter cw = new ClassWriter( 0 );
		cw.visit( Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName,
				null, "java/lang/Object", null );

		final MethodVisitor mv = cw.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
				CrossClassLoaderLookupBridge.BRIDGE_METHOD_NAME,
				"()Ljava/lang/invoke/MethodHandles$Lookup;", null, null );
		mv.visitCode();
		mv.visitMethodInsn( Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles",
				"lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", false );
		mv.visitInsn( Opcodes.ARETURN );
		mv.visitMaxs( 1, 0 );
		mv.visitEnd();

		cw.visitEnd();
		return cw.toByteArray();
	}
}
