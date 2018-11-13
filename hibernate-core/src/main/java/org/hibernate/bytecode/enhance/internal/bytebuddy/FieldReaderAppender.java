/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Objects;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
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

abstract class FieldReaderAppender implements ByteCodeAppender {

	protected final TypeDescription managedCtClass;

	protected final AnnotatedFieldDescription persistentField;

	protected final FieldDescription.InDefinedShape persistentFieldAsDefined;

	private FieldReaderAppender(TypeDescription managedCtClass, AnnotatedFieldDescription persistentField) {
		this.managedCtClass = managedCtClass;
		this.persistentField = persistentField;
		this.persistentFieldAsDefined = persistentField.asDefined();
	}

	static ByteCodeAppender of(TypeDescription managedCtClass, AnnotatedFieldDescription persistentField) {
		if ( !persistentField.isVisibleTo( managedCtClass ) ) {
			return new MethodDispatching( managedCtClass, persistentField );
		}
		else {
			return new FieldWriting( managedCtClass, persistentField );
		}
	}

	@Override
	public Size apply(
			MethodVisitor methodVisitor,
			Implementation.Context implementationContext,
			MethodDescription instrumentedMethod) {
		TypeDescription dispatcherType = persistentFieldAsDefined.getType().isPrimitive()
				? persistentFieldAsDefined.getType().asErasure()
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
		Label skip = new Label();
		methodVisitor.visitJumpInsn( Opcodes.IFNULL, skip );
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
		// .readXXX( self, fieldName, field );
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitLdcInsn( persistentFieldAsDefined.getName() );
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		fieldRead( methodVisitor );
		methodVisitor.visitMethodInsn(
				Opcodes.INVOKEINTERFACE,
				Type.getInternalName( PersistentAttributeInterceptor.class ),
				"read" + EnhancerImpl.capitalize( dispatcherType.getSimpleName() ),
				Type.getMethodDescriptor(
						Type.getType( dispatcherType.getDescriptor() ),
						Type.getType( Object.class ),
						Type.getType( String.class ),
						Type.getType( dispatcherType.getDescriptor() )
				),
				true
		);
		// field = (cast) XXX
		if ( !dispatcherType.isPrimitive() ) {
			methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, persistentFieldAsDefined.getType().asErasure().getInternalName() );
		}
		fieldWrite( methodVisitor );
		// end if
		methodVisitor.visitLabel( skip );
		if ( implementationContext.getClassFileVersion().isAtLeast( ClassFileVersion.JAVA_V6 ) ) {
			methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
		}
		// return field
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		fieldRead( methodVisitor );
		if ( !persistentField.getType().isPrimitive()
				&& !persistentField.getType().asErasure().getInternalName().equals( persistentFieldAsDefined.getType().asErasure().getInternalName() ) ) {
			methodVisitor.visitTypeInsn( Opcodes.CHECKCAST, persistentField.getType().asErasure().getInternalName() );
		}
		methodVisitor.visitInsn( Type.getType( persistentFieldAsDefined.getType().asErasure().getDescriptor() ).getOpcode( Opcodes.IRETURN ) );
		return new Size( 4 + persistentFieldAsDefined.getType().getStackSize().getSize(), instrumentedMethod.getStackSize() );
	}

	protected abstract void fieldRead(MethodVisitor methodVisitor);

	protected abstract void fieldWrite(MethodVisitor methodVisitor);

	private static class FieldWriting extends FieldReaderAppender {

		private FieldWriting(TypeDescription managedCtClass, AnnotatedFieldDescription persistentField) {
			super( managedCtClass, persistentField );
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

		private MethodDispatching(TypeDescription managedCtClass, AnnotatedFieldDescription persistentField) {
			super( managedCtClass, persistentField );
		}

		@Override
		protected void fieldRead(MethodVisitor methodVisitor) {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					managedCtClass.getSuperClass().asErasure().getInternalName(),
					EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + persistentFieldAsDefined.getName(),
					Type.getMethodDescriptor( Type.getType( persistentFieldAsDefined.getType().asErasure().getDescriptor() ) ),
					false
			);
		}

		@Override
		protected void fieldWrite(MethodVisitor methodVisitor) {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					managedCtClass.getSuperClass().asErasure().getInternalName(),
					EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + persistentFieldAsDefined.getName(),
					Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( persistentFieldAsDefined.getType().asErasure().getDescriptor() ) ),
					false
			);
		}
	}

	@Override
	public boolean equals(final Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final FieldReaderAppender that = (FieldReaderAppender) o;
		return Objects.equals( managedCtClass, that.managedCtClass ) &&
			Objects.equals( persistentField, that.persistentField ) &&
			Objects.equals( persistentFieldAsDefined, that.persistentFieldAsDefined );
	}

	@Override
	public int hashCode() {
		return Objects.hash( managedCtClass, persistentField, persistentFieldAsDefined );
	}

}
