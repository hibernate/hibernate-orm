/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import static net.bytebuddy.matcher.ElementMatchers.hasDescriptor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hibernate.bytecode.enhance.internal.BytecodeEnhancementLogging.ENHANCEMENT_LOGGER;

import jakarta.persistence.Id;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.utility.OpenedClassReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.pool.TypePool;

import java.util.Objects;

final class FieldAccessEnhancer implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

	private final TypeDescription managedCtClass;

	private final ByteBuddyEnhancementContext enhancementContext;

	private final TypePool classPool;

	FieldAccessEnhancer(TypeDescription managedCtClass, ByteBuddyEnhancementContext enhancementContext, TypePool classPool) {
		this.managedCtClass = managedCtClass;
		this.enhancementContext = enhancementContext;
		this.classPool = classPool;
	}

	@Override
	public MethodVisitor wrap(
			TypeDescription instrumentedType,
			MethodDescription instrumentedMethod,
			MethodVisitor methodVisitor,
			Implementation.Context implementationContext,
			TypePool typePool,
			int writerFlags,
			int readerFlags) {
		return new MethodVisitor( OpenedClassReader.ASM_API, methodVisitor ) {
			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				if ( opcode != Opcodes.GETFIELD && opcode != Opcodes.PUTFIELD ) {
					super.visitFieldInsn( opcode, owner, name, desc );
				}
				else {
					final var declaredOwnerType = findDeclaredType( owner );
					final var field = findField( declaredOwnerType, name, desc );
					// try to discover composite types on the fly to support some testing scenarios
					enhancementContext.discoverCompositeTypes( declaredOwnerType, typePool );

					if ( (enhancementContext.isEntityClass( declaredOwnerType.asErasure() )
						|| enhancementContext.isCompositeClass( declaredOwnerType.asErasure() ))
							&& !field.getType().asErasure().equals( managedCtClass )
							&& enhancementContext.isPersistentField( field )
							&& !field.hasAnnotation( Id.class )
							&& !field.getName().equals( "this$0" ) ) {

						ENHANCEMENT_LOGGER.extendedTransformingFieldAccess(
								declaredOwnerType.getName(),
								field.getName(),
								instrumentedType.getName(),
								instrumentedMethod.getName()
						);

						switch ( opcode ) {
							case Opcodes.GETFIELD:
								methodVisitor.visitMethodInsn(
										Opcodes.INVOKEVIRTUAL,
										owner,
										EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + name,
										Type.getMethodDescriptor( Type.getType( desc ) ),
										false
								);
								return;
							case Opcodes.PUTFIELD:
								if ( field.getFieldDescription().isFinal() ) {
									// Final fields will only be written to from the constructor,
									// so there's no point trying to replace final field writes with a method call.
									break;
								}
								methodVisitor.visitMethodInsn(
										Opcodes.INVOKEVIRTUAL,
										owner,
										EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + name,
										Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( desc ) ),
										false
								);
								return;
							default:
								throw new EnhancementException( "Unexpected opcode: " + opcode );
						}
					}
					super.visitFieldInsn( opcode, owner, name, desc );
				}
			}
		};
	}

	private TypeDescription findDeclaredType(String name) {
		//Classpool#describe does not accept '/' in the description name as it expects a class name
		final String cleanedName = name.replace( '/', '.' );
		final var resolution = classPool.describe( cleanedName );
		if ( !resolution.isResolved() ) {
			throw new EnhancementException( String.format(
					"Unable to perform extended enhancement - Unable to locate [%s]",
					cleanedName
			) );
		}
		return resolution.resolve();
	}

	private AnnotatedFieldDescription findField(TypeDescription declaredOwnedType, String name, String desc) {
		final var fields = findFields( declaredOwnedType, name, desc );
		if ( fields.size() != 1 ) {
			throw new EnhancementException( String.format(
					"Unable to perform extended enhancement - No unique field [%s] defined by [%s]",
					name,
					declaredOwnedType.getName()
			) );
		}
		return new AnnotatedFieldDescription( enhancementContext, fields.getOnly() );
	}

	private static @NonNull FieldList<?> findFields(TypeDescription declaredOwnedType, String name, String desc) {
		TypeDefinition ownerType = declaredOwnedType;
		final var fieldFilter = named( name ).and( hasDescriptor( desc ) );
		FieldList<?> fields = ownerType.getDeclaredFields().filter( fieldFilter );
		// Look in the superclasses if necessary
		while ( fields.isEmpty() && ownerType.getSuperClass() != null ) {
			ownerType = ownerType.getSuperClass();
			fields = ownerType.getDeclaredFields().filter( fieldFilter );
		}
		return fields;
	}

	@Override
	public boolean equals(final Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof FieldAccessEnhancer that) ) {
			return false;
		}
		else {
			return Objects.equals( this.managedCtClass, that.managedCtClass );
		}
	}

	@Override
	public int hashCode() {
		return managedCtClass.hashCode();
	}

}
