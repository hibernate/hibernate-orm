/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

abstract class FieldWriterAppender implements ByteCodeAppender {

	protected final TypeDescription managedCtClass;

	protected final FieldDescription.InDefinedShape persistentField;

	private FieldWriterAppender(TypeDescription managedCtClass, FieldDescription.InDefinedShape persistentField) {
		this.managedCtClass = managedCtClass;
		this.persistentField = persistentField;
	}

	static ByteCodeAppender of(TypeDescription managedCtClass, FieldDescription persistentField) {
		if ( !persistentField.isVisibleTo( managedCtClass ) ) {
			return new MethodDispatching( managedCtClass, persistentField.asDefined() );
		}
		else {
			return new FieldWriting( managedCtClass, persistentField.asDefined() );
		}
	}

	@Override
	public Size apply(
			MethodVisitor methodVisitor,
			Implementation.Context implementationContext,
			MethodDescription instrumentedMethod) {
		TypeDescription dispatcherType = persistentField.getType().isPrimitive()
				? persistentField.getType().asErasure()
				: TypeDescription.OBJECT;
		// if ( this.$$_hibernate_getInterceptor() != null )
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitMethodInsn(
				Opcodes.INVOKEVIRTUAL,
				managedCtClass.getInternalName(),
				EnhancerConstants.INTERCEPTOR_GETTER_NAME,
				Type.getMethodDescriptor( Type.getType( PersistentAttributeInterceptor.class ) ),
				false
		);
		Label noInterceptor = new Label();
		methodVisitor.visitJumpInsn( Opcodes.IFNULL, noInterceptor );
		// this (for field write)
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		// this.$$_hibernate_getInterceptor();
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitMethodInsn(
				Opcodes.INVOKEVIRTUAL,
				managedCtClass.getInternalName(),
				EnhancerConstants.INTERCEPTOR_GETTER_NAME,
				Type.getMethodDescriptor( Type.getType( PersistentAttributeInterceptor.class ) ),
				false
		);
		// .writeXXX( self, fieldName, field, arg1 );
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitLdcInsn( persistentField.getName() );
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		fieldRead( methodVisitor );
		methodVisitor.visitVarInsn( Type.getType( dispatcherType.getDescriptor() ).getOpcode( Opcodes.ILOAD ), 1 );
		methodVisitor.visitMethodInsn(
				Opcodes.INVOKEINTERFACE,
				Type.getInternalName( PersistentAttributeInterceptor.class ),
				"write" + EnhancerImpl.capitalize( dispatcherType.getSimpleName() ),
				Type.getMethodDescriptor(
						Type.getType( dispatcherType.getDescriptor() ),
						Type.getType( Object.class ),
						Type.getType( String.class ),
						Type.getType( dispatcherType.getDescriptor() ),
						Type.getType( dispatcherType.getDescriptor() )
				),
				true
		);
		// arg1 = (cast) XXX
		if ( !dispatcherType.isPrimitive() ) {
			methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, persistentField.getType().asErasure().getInternalName() );
		}
		fieldWrite( methodVisitor );
		// return
		methodVisitor.visitInsn( Opcodes.RETURN );
		// else
		methodVisitor.visitLabel( noInterceptor );
		if ( implementationContext.getClassFileVersion().isAtLeast( ClassFileVersion.JAVA_V6 ) ) {
			methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
		}
		// this (for field write)
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		// arg1 = (cast) XXX
		methodVisitor.visitVarInsn( Type.getType( dispatcherType.getDescriptor() ).getOpcode( Opcodes.ILOAD ), 1 );
		if ( !dispatcherType.isPrimitive() ) {
			methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, persistentField.getType().asErasure().getInternalName() );
		}
		fieldWrite( methodVisitor );
		// return
		methodVisitor.visitInsn( Opcodes.RETURN );
		return new Size( 4 + 2 * persistentField.getType().getStackSize().getSize(), instrumentedMethod.getStackSize() );
	}

	protected abstract void fieldRead(MethodVisitor methodVisitor);

	protected abstract void fieldWrite(MethodVisitor methodVisitor);

	private static class FieldWriting extends FieldWriterAppender {

		private FieldWriting(TypeDescription managedCtClass, FieldDescription.InDefinedShape fieldDescription) {
			super( managedCtClass, fieldDescription );
		}

		@Override
		protected void fieldRead(MethodVisitor methodVisitor) {
			methodVisitor.visitFieldInsn(
					Opcodes.GETFIELD,
					persistentField.getDeclaringType().asErasure().getInternalName(),
					persistentField.getInternalName(),
					persistentField.getDescriptor()
			);
		}

		@Override
		protected void fieldWrite(MethodVisitor methodVisitor) {
			methodVisitor.visitFieldInsn(
					Opcodes.PUTFIELD,
					persistentField.getDeclaringType().asErasure().getInternalName(),
					persistentField.getInternalName(),
					persistentField.getDescriptor()
			);
		}
	}

	private static class MethodDispatching extends FieldWriterAppender {

		private MethodDispatching(TypeDescription managedCtClass, FieldDescription.InDefinedShape fieldDescription) {
			super( managedCtClass, fieldDescription );
		}

		@Override
		protected void fieldRead(MethodVisitor methodVisitor) {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					managedCtClass.getSuperClass().asErasure().getInternalName(),
					EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + persistentField.getName(),
					Type.getMethodDescriptor( Type.getType( persistentField.getType().asErasure().getDescriptor() ) ),
					false
			);
		}

		@Override
		protected void fieldWrite(MethodVisitor methodVisitor) {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					managedCtClass.getSuperClass().asErasure().getInternalName(),
					EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + persistentField.getName(),
					Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( persistentField.getType().asErasure().getDescriptor() ) ),
					false
			);
		}
	}
}
