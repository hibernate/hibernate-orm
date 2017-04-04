/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.persistence.Embedded;

import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.pool.TypePool;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

class PersistentAttributeTransformer implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( PersistentAttributeTransformer.class );

	private final TypeDescription managedCtClass;

	private final ByteBuddyEnhancementContext enhancementContext;

	private final TypePool classPool;

	private final FieldDescription[] enhancedFields;

	private PersistentAttributeTransformer(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			TypePool classPool,
			FieldDescription[] enhancedFields) {
		this.managedCtClass = managedCtClass;
		this.enhancementContext = enhancementContext;
		this.classPool = classPool;
		this.enhancedFields = enhancedFields;
	}

	public static PersistentAttributeTransformer collectPersistentFields(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			TypePool classPool) {
		List<FieldDescription> persistentFieldList = new ArrayList<FieldDescription>();
		for ( FieldDescription ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields and skip fields added by enhancement and  outer reference in inner classes
			if ( ctField.getName().startsWith( "$$_hibernate_" ) || "this$0".equals( ctField.getName() ) ) {
				continue;
			}
			if ( !ctField.isStatic() && enhancementContext.isPersistentField( ctField ) ) {
				persistentFieldList.add( ctField );
			}
		}
		// HHH-10646 Add fields inherited from @MappedSuperclass
		// HHH-10981 There is no need to do it for @MappedSuperclass
		if ( !enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			persistentFieldList.addAll( collectInheritPersistentFields( managedCtClass, enhancementContext ) );
		}

		FieldDescription[] orderedFields = enhancementContext.order( persistentFieldList.toArray( new FieldDescription[0] ) );
		log.debugf( "Persistent fields for entity %s: %s", managedCtClass.getName(), Arrays.toString( orderedFields ) );
		return new PersistentAttributeTransformer( managedCtClass, enhancementContext, classPool, orderedFields );
	}

	private static Collection<FieldDescription> collectInheritPersistentFields(
			TypeDefinition managedCtClass,
			ByteBuddyEnhancementContext enhancementContext) {
		if ( managedCtClass == null || managedCtClass.represents( Object.class ) ) {
			return Collections.emptyList();
		}
		TypeDefinition managedCtSuperclass = managedCtClass.getSuperClass();

		if ( !enhancementContext.isMappedSuperclassClass( managedCtSuperclass.asErasure() ) ) {
			return collectInheritPersistentFields( managedCtSuperclass, enhancementContext );
		}
		log.debugf( "Found @MappedSuperclass %s to collectPersistenceFields", managedCtSuperclass );
		List<FieldDescription> persistentFieldList = new ArrayList<FieldDescription>();

		for ( FieldDescription ctField : managedCtSuperclass.getDeclaredFields() ) {
			if ( ctField.getName().startsWith( "$$_hibernate_" ) || "this$0".equals( ctField.getName() ) ) {
				continue;
			}
			if ( !ctField.isStatic() && enhancementContext.isPersistentField( ctField ) ) {
				persistentFieldList.add( ctField );
			}
		}
		persistentFieldList.addAll( collectInheritPersistentFields( managedCtSuperclass, enhancementContext ) );
		return persistentFieldList;
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
				if ( isEnhanced( owner, name, desc ) ) {
					switch ( opcode ) {
						case Opcodes.GETFIELD:
							methodVisitor.visitMethodInsn(
									Opcodes.INVOKEVIRTUAL,
									owner,
									EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + name,
									"()" + desc,
									false
							);
							return;
						case Opcodes.PUTFIELD:
							methodVisitor.visitMethodInsn(
									Opcodes.INVOKEVIRTUAL,
									owner,
									EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + name,
									"(" + desc + ")V",
									false
							);
							return;
					}
				}
				super.visitFieldInsn( opcode, owner, name, desc );
			}
		};
	}

	private boolean isEnhanced(String owner, String name, String desc) {
		for ( FieldDescription enhancedField : enhancedFields ) {
			if ( enhancedField.getName().equals( name )
					&& enhancedField.getDescriptor().equals( desc )
					&& enhancedField.getDeclaringType().asErasure().getInternalName().equals( owner ) ) {
				return true;
			}
		}
		return false;
	}

	DynamicType.Builder<?> applyTo(DynamicType.Builder<?> builder, boolean accessor) {
		boolean compositeOwner = false;

		builder = builder.visit( new AsmVisitorWrapper.ForDeclaredMethods().method( not( nameStartsWith( "$$_hibernate_" ) ), this ) );
		for ( FieldDescription enhancedField : enhancedFields ) {
			builder = builder
					.defineMethod(
							EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + enhancedField.getName(),
							enhancedField.getType().asErasure(),
							Visibility.PUBLIC
					)
					.intercept(
							accessor
									? FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() )
									: fieldReader( enhancedField )
					)
					.defineMethod(
							EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + enhancedField.getName(),
							TypeDescription.VOID,
							Visibility.PUBLIC
					)
					.withParameters( enhancedField.getType().asErasure() )
					.intercept( accessor
										? FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() )
										: fieldWriter( enhancedField ) );

			if ( !compositeOwner
					&& !accessor
					&& EnhancerImpl.isAnnotationPresent( enhancedField, Embedded.class )
					&& enhancementContext.isCompositeClass( enhancedField.getType().asErasure() )
					&& enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				compositeOwner = true;
			}
		}

		if ( compositeOwner ) {
			builder = builder.implement( CompositeOwner.class );

			if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
				builder = builder.defineMethod( EnhancerConstants.TRACKER_CHANGER_NAME, void.class, Visibility.PUBLIC )
						.withParameters( String.class )
						.intercept( Advice.to( CodeTemplates.CompositeOwnerDirtyCheckingHandler.class ).wrap( StubMethod.INSTANCE ) );
			}
		}

		if ( enhancementContext.doExtendedEnhancement( managedCtClass ) ) {
			builder = applyExtended( builder );
		}

		return builder;
	}

	private Implementation fieldReader(FieldDescription enhancedField) {
		if ( !enhancementContext.hasLazyLoadableAttributes( managedCtClass ) || !enhancementContext.isLazyLoadable( enhancedField ) ) {
			if ( enhancedField.getDeclaringType().asErasure().equals( managedCtClass ) ) {
				return FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() );
			}
			else {
				return new Implementation.Simple( new FieldMethodReader( managedCtClass, enhancedField ) );
			}
		}
		else {
			return new Implementation.Simple( FieldReaderAppender.of( managedCtClass, enhancedField ) );
		}
	}

	private Implementation fieldWriter(FieldDescription enhancedField) {
		Implementation implementation;
		if ( !enhancementContext.hasLazyLoadableAttributes( managedCtClass ) || !enhancementContext.isLazyLoadable( enhancedField ) ) {
			if ( enhancedField.getDeclaringType().asErasure().equals( managedCtClass ) ) {
				implementation = FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() );
			}
			else {
				implementation = new Implementation.Simple( new FieldMethodWriter( managedCtClass, enhancedField ) );
			}
		}
		else {
			implementation = new Implementation.Simple( FieldWriterAppender.of( managedCtClass, enhancedField ) );
		}
		implementation = InlineDirtyCheckingHandler.wrap( managedCtClass, enhancementContext, enhancedField, implementation );
		return BiDirectionalAssociationHandler.wrap( managedCtClass, enhancementContext, enhancedField, implementation );
	}

	DynamicType.Builder<?> applyExtended(DynamicType.Builder<?> builder) {
		AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper enhancer = new FieldAccessEnhancer( managedCtClass, enhancementContext, classPool );
		return builder.visit( new AsmVisitorWrapper.ForDeclaredMethods().method( not( nameStartsWith( "$$_hibernate_" ) ), enhancer ) );
	}

	private static class FieldMethodReader implements ByteCodeAppender {

		private final TypeDescription managedCtClass;

		private final FieldDescription persistentField;

		private FieldMethodReader(TypeDescription managedCtClass, FieldDescription persistentField) {
			this.managedCtClass = managedCtClass;
			this.persistentField = persistentField;
		}

		@Override
		public Size apply(
				MethodVisitor methodVisitor,
				Implementation.Context implementationContext,
				MethodDescription instrumentedMethod
		) {
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					managedCtClass.getSuperClass().asErasure().getInternalName(),
					EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + persistentField.getName(),
					Type.getMethodDescriptor( Type.getType( persistentField.getType().asErasure().getDescriptor() ) ),
					false
			);
			methodVisitor.visitInsn( Type.getType( persistentField.getType().asErasure().getDescriptor() ).getOpcode( Opcodes.IRETURN ) );
			return new Size( persistentField.getType().getStackSize().getSize(), instrumentedMethod.getStackSize() );
		}
	}

	private static class FieldMethodWriter implements ByteCodeAppender {

		private final TypeDescription managedCtClass;

		private final FieldDescription persistentField;

		private FieldMethodWriter(TypeDescription managedCtClass, FieldDescription persistentField) {
			this.managedCtClass = managedCtClass;
			this.persistentField = persistentField;
		}

		@Override
		public Size apply(
				MethodVisitor methodVisitor,
				Implementation.Context implementationContext,
				MethodDescription instrumentedMethod
		) {
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitVarInsn( Type.getType( persistentField.getType().asErasure().getDescriptor() ).getOpcode( Opcodes.ILOAD ), 1 );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					managedCtClass.getSuperClass().asErasure().getInternalName(),
					EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + persistentField.getName(),
					Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( persistentField.getType().asErasure().getDescriptor() ) ),
					false
			);
			methodVisitor.visitInsn( Opcodes.RETURN );
			return new Size( 1 + persistentField.getType().getStackSize().getSize(), instrumentedMethod.getStackSize() );
		}
	}
}
