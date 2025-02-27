/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Collection;
import java.util.Objects;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

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

final class InlineDirtyCheckingHandler implements Implementation, ByteCodeAppender {

	private static final String HELPER_TYPE_NAME = Type.getInternalName( InlineDirtyCheckerEqualsHelper.class );
	private static final Type PE_INTERCEPTABLE_TYPE = Type.getType( PersistentAttributeInterceptable.class );
	private static final Type OBJECT_TYPE = Type.getType( Object.class );
	private static final Type STRING_TYPE = Type.getType( String.class );

	private final Implementation delegate;

	private final TypeDescription managedCtClass;

	private final FieldDescription.InDefinedShape persistentField;
	private final boolean applyLazyCheck;

	private InlineDirtyCheckingHandler(
			Implementation delegate,
			TypeDescription managedCtClass,
			FieldDescription.InDefinedShape persistentField,
			boolean applyLazyCheck) {
		this.delegate = delegate;
		this.managedCtClass = managedCtClass;
		this.persistentField = persistentField;
		this.applyLazyCheck = applyLazyCheck;
	}

	static Implementation wrap(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			AnnotatedFieldDescription persistentField,
			Implementation implementation) {
		if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {

			if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
				implementation = Advice.to( CodeTemplates.CompositeDirtyCheckingHandler.class ).wrap( implementation );
			}
			else if ( !persistentField.hasAnnotation( Id.class )
					&& !persistentField.hasAnnotation( EmbeddedId.class )
					&& !( persistentField.getType().asErasure().isAssignableTo( Collection.class )
					&& enhancementContext.isMappedCollection( persistentField ) ) ) {
				implementation = new InlineDirtyCheckingHandler(
						implementation,
						managedCtClass,
						persistentField.asDefined(),
						enhancementContext.hasLazyLoadableAttributes( managedCtClass )
				);
			}

			if ( enhancementContext.isCompositeField( persistentField )
				&& !persistentField.hasAnnotation( EmbeddedId.class )
				// Don't do composite owner tracking for records
				&& !persistentField.getType().isRecord() ) {

				// HHH-13759 - Call getter on superclass if field is not visible
				// An embedded field won't be visible if declared private in a superclass
				// annotated with @MappedSuperclass
				Advice.WithCustomMapping advice = Advice.withCustomMapping();
				advice = persistentField.isVisibleTo( managedCtClass )
						? advice.bind( CodeTemplates.FieldValue.class, persistentField.getFieldDescription() )
						: advice.bind( CodeTemplates.FieldValue.class, new CodeTemplates.GetterMapping( persistentField.getFieldDescription(), persistentField.getGetter().get().getReturnType() ) );

				implementation = advice
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

		if ( applyLazyCheck ) {
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitLdcInsn( persistentField.getName() );
		}
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
		if ( applyLazyCheck ) {
			if ( persistentField.getType().isPrimitive() ) {
				final Type fieldType = Type.getType( persistentField.getDescriptor() );
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKESTATIC,
						HELPER_TYPE_NAME,
						"areEquals",
						Type.getMethodDescriptor(
								Type.BOOLEAN_TYPE,
								PE_INTERCEPTABLE_TYPE,
								STRING_TYPE,
								fieldType,
								fieldType
						),
						false
				);
			}
			else {
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKESTATIC,
						HELPER_TYPE_NAME,
						"areEquals",
						Type.getMethodDescriptor(
								Type.BOOLEAN_TYPE,
								PE_INTERCEPTABLE_TYPE,
								STRING_TYPE,
								OBJECT_TYPE,
								OBJECT_TYPE
						),
						false
				);
			}
			branchCode = Opcodes.IFNE;
		}
		else {
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
						Type.getInternalName( Objects.class ),
						"deepEquals",
						Type.getMethodDescriptor(
								Type.BOOLEAN_TYPE,
								OBJECT_TYPE,
								OBJECT_TYPE
						),
						false
				);
				branchCode = Opcodes.IFNE;
			}
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
				Type.getMethodDescriptor( Type.VOID_TYPE, STRING_TYPE ),
				false
		);
		// }
		methodVisitor.visitLabel( skip );
		if ( implementationContext.getClassFileVersion().isAtLeast( ClassFileVersion.JAVA_V6 ) ) {
			methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
		}
		return new Size( 3 + 2 * persistentField.getType().asErasure().getStackSize().getSize(), instrumentedMethod.getStackSize() );
	}

	@Override
	public boolean equals(final Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || InlineDirtyCheckingHandler.class != o.getClass() ) {
			return false;
		}
		final InlineDirtyCheckingHandler that = (InlineDirtyCheckingHandler) o;
		return Objects.equals( delegate, that.delegate ) &&
			Objects.equals( managedCtClass, that.managedCtClass ) &&
			Objects.equals( persistentField, that.persistentField );
	}

	@Override
	public int hashCode() {
		return Objects.hash( delegate, managedCtClass, persistentField );
	}
}
