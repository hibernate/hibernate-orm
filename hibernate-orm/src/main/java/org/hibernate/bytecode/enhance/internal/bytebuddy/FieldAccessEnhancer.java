/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import javax.persistence.Id;

import net.bytebuddy.description.method.MethodList;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.pool.TypePool;

import static net.bytebuddy.matcher.ElementMatchers.hasDescriptor;
import static net.bytebuddy.matcher.ElementMatchers.named;

class FieldAccessEnhancer implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

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
		return new MethodVisitor( Opcodes.ASM5, methodVisitor ) {
			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				if ( opcode != Opcodes.GETFIELD && opcode != Opcodes.PUTFIELD ) {
					super.visitFieldInsn( opcode, owner, name, desc );
					return;
				}

				FieldDescription field = findField( owner, name, desc );

				if ( ( enhancementContext.isEntityClass( field.getDeclaringType().asErasure() )
						|| enhancementContext.isCompositeClass( field.getDeclaringType().asErasure() ) )
						&& !field.getType().asErasure().equals( managedCtClass )
						&& enhancementContext.isPersistentField( field )
						&& !EnhancerImpl.isAnnotationPresent( field, Id.class )
						&& !field.getName().equals( "this$0" ) ) {

					log.debugf(
							"Extended enhancement: Transforming access to field [%s.%s] from method [%s#%s]",
							field.getType().asErasure(),
							field.getName(),
							field.getName(),
							name
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
				else {
					super.visitFieldInsn( opcode, owner, name, desc );
				}
			}
		};
	}

	private FieldDescription findField(String owner, String name, String desc) {
		TypePool.Resolution resolution = classPool.describe( owner.replace( '/', '.' ) );
		if ( !resolution.isResolved() ) {
			final String msg = String.format(
					"Unable to perform extended enhancement - Unable to locate [%s]",
					owner.replace( '/', '.' )
			);
			throw new EnhancementException( msg );
		}
		FieldList<?> fields = resolution.resolve().getDeclaredFields().filter( named( name ).and( hasDescriptor( desc ) ) );
		if ( fields.size() != 1 ) {
			final String msg = String.format(
					"Unable to perform extended enhancement - No unique field [%s] defined by [%s]",
					name,
					owner.replace( '/', '.' )
			);
			throw new EnhancementException( msg );
		}
		return fields.getOnly();
	}
}
