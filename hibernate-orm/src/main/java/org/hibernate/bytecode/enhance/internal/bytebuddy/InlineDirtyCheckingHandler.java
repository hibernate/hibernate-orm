/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Collection;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.internal.util.compare.EqualsHelper;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

class InlineDirtyCheckingHandler implements Implementation, ByteCodeAppender {

	private final Implementation delegate;

	private final TypeDescription managedCtClass;

	private final FieldDescription.InDefinedShape persistentField;

	private InlineDirtyCheckingHandler(Implementation delegate, TypeDescription managedCtClass, FieldDescription.InDefinedShape persistentField) {
		this.delegate = delegate;
		this.managedCtClass = managedCtClass;
		this.persistentField = persistentField;
	}

	static Implementation wrap(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			FieldDescription persistentField,
			Implementation implementation) {
		if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {

			if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
				implementation = Advice.to( CodeTemplates.CompositeDirtyCheckingHandler.class ).wrap( implementation );
			}
			else if ( !EnhancerImpl.isAnnotationPresent( persistentField, Id.class )
					&& !EnhancerImpl.isAnnotationPresent( persistentField, EmbeddedId.class )
					&& !( persistentField.getType().asErasure().isAssignableTo( Collection.class )
					&& enhancementContext.isMappedCollection( persistentField ) ) ) {
				implementation = new InlineDirtyCheckingHandler( implementation, managedCtClass, persistentField.asDefined() );
			}

			if ( enhancementContext.isCompositeClass( persistentField.getType().asErasure() )
					&& EnhancerImpl.isAnnotationPresent( persistentField, Embedded.class ) ) {

				implementation = Advice.withCustomMapping()
						.bind( CodeTemplates.FieldValue.class, persistentField )
						.bind( CodeTemplates.FieldName.class, persistentField.getName() )
						.to( CodeTemplates.CompositeFieldDirtyCheckingHandler.class )
						.wrap( implementation );
			}
		}
		return implementation;
	}

	@Override
	public ByteCodeAppender appender(Target implementationTarget) {
		return new ByteCodeAppender.Compound( this, delegate.appender( implementationTarget ) );
	}

	@Override
	public InstrumentedType prepare(InstrumentedType instrumentedType) {
		return delegate.prepare( instrumentedType );
	}

	@Override
	public Size apply(
			MethodVisitor methodVisitor,
			Context implementationContext,
			MethodDescription instrumentedMethod) {
		// if (arg != field) {
		methodVisitor.visitVarInsn( Type.getType( persistentField.getType().asErasure().getDescriptor() ).getOpcode( Opcodes.ILOAD ), 1 );
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		if ( persistentField.getDeclaringType().asErasure().equals( managedCtClass ) ) {
			methodVisitor.visitFieldInsn(
					Opcodes.GETFIELD,
					persistentField.getDeclaringType().asErasure().getInternalName(),
					persistentField.getName(),
					persistentField.getDescriptor()
			);
		}
		else {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKEVIRTUAL,
					persistentField.getDeclaringType().asErasure().getInternalName(),
					EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + persistentField.getName(),
					Type.getMethodDescriptor( Type.getType( persistentField.getDescriptor() ) ),
					false
			);
		}
		int branchCode;
		if ( persistentField.getType().isPrimitive() ) {
			if ( persistentField.getType().represents( long.class ) ) {
				methodVisitor.visitInsn( Opcodes.LCMP );
			}
			else if ( persistentField.getType().represents( float.class ) ) {
				methodVisitor.visitInsn( Opcodes.FCMPL );
			}
			else if ( persistentField.getType().represents( double.class ) ) {
				methodVisitor.visitInsn( Opcodes.DCMPL );
			}
			else {
				methodVisitor.visitInsn( Opcodes.ISUB );
			}
			branchCode = Opcodes.IFEQ;
		}
		else {
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESTATIC,
					Type.getInternalName( EqualsHelper.class ),
					"areEqual",
					Type.getMethodDescriptor( Type.getType( boolean.class ), Type.getType( Object.class ), Type.getType( Object.class ) ),
					false
			);
			branchCode = Opcodes.IFNE;
		}
		Label skip = new Label();
		methodVisitor.visitJumpInsn( branchCode, skip );
		// this.$$_hibernate_trackChange(fieldName)
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitLdcInsn( persistentField.getName() );
		methodVisitor.visitMethodInsn(
				Opcodes.INVOKEVIRTUAL,
				managedCtClass.getInternalName(),
				EnhancerConstants.TRACKER_CHANGER_NAME,
				Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( String.class ) ),
				false
		);
		// }
		methodVisitor.visitLabel( skip );
		if ( implementationContext.getClassFileVersion().isAtLeast( ClassFileVersion.JAVA_V6 ) ) {
			methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
		}
		return new Size( 1 + 2 * persistentField.getType().asErasure().getStackSize().getSize(), instrumentedMethod.getStackSize() );
	}
}
