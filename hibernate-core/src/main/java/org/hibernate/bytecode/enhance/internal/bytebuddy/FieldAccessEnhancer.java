/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import static net.bytebuddy.matcher.ElementMatchers.hasDescriptor;
import static net.bytebuddy.matcher.ElementMatchers.named;

import jakarta.persistence.Id;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.OpenedClassReader;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

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

	private static final CoreMessageLogger log = CoreLogging.messageLogger( FieldAccessEnhancer.class );

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
					return;
				}

				TypeDescription declaredOwnerType = findDeclaredType( owner );
				AnnotatedFieldDescription field = findField( declaredOwnerType, name, desc );
				// try to discover composite types on the fly to support some testing scenarios
				enhancementContext.discoverCompositeTypes( declaredOwnerType, typePool );

				if ( ( enhancementContext.isEntityClass( declaredOwnerType.asErasure() )
						|| enhancementContext.isCompositeClass( declaredOwnerType.asErasure() ) )
						&& !field.getType().asErasure().equals( managedCtClass )
						&& enhancementContext.isPersistentField( field )
						&& !field.hasAnnotation( Id.class )
						&& !field.getName().equals( "this$0" ) ) {

					log.debugf(
							"Extended enhancement: Transforming access to field [%s#%s] from method [%s#%s()]",
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
		};
	}

	private TypeDescription findDeclaredType(String name) {
		//Classpool#describe does not accept '/' in the description name as it expects a class name
		final String cleanedName = name.replace( '/', '.' );
		final TypePool.Resolution resolution = classPool.describe( cleanedName );
		if ( !resolution.isResolved() ) {
			final String msg = String.format(
					"Unable to perform extended enhancement - Unable to locate [%s]",
					cleanedName
			);
			throw new EnhancementException( msg );
		}
		return resolution.resolve();
	}

	private AnnotatedFieldDescription findField(TypeDescription declaredOwnedType, String name, String desc) {
		TypeDefinition ownerType = declaredOwnedType;
		ElementMatcher.Junction<NamedElement.WithDescriptor> fieldFilter = named( name ).and( hasDescriptor( desc ) );

		FieldList<?> fields = ownerType.getDeclaredFields().filter( fieldFilter );

		// Look in the superclasses if necessary
		while ( fields.isEmpty() && ownerType.getSuperClass() != null ) {
			ownerType = ownerType.getSuperClass();
			fields = ownerType.getDeclaredFields().filter( fieldFilter );
		}

		if ( fields.size() != 1 ) {
			final String msg = String.format(
					"Unable to perform extended enhancement - No unique field [%s] defined by [%s]",
					name,
					declaredOwnedType.getName()
			);
			throw new EnhancementException( msg );
		}
		return new AnnotatedFieldDescription( enhancementContext, fields.getOnly() );
	}

	@Override
	public boolean equals(final Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || FieldAccessEnhancer.class != o.getClass() ) {
			return false;
		}
		final FieldAccessEnhancer that = (FieldAccessEnhancer) o;
		return Objects.equals( managedCtClass, that.managedCtClass );
	}

	@Override
	public int hashCode() {
		return managedCtClass.hashCode();
	}

}
