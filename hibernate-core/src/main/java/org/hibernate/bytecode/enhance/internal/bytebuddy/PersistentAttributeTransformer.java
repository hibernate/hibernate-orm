/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import static net.bytebuddy.matcher.ElementMatchers.anyOf;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.ModifierAdjustment;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
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
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;

final class PersistentAttributeTransformer implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( PersistentAttributeTransformer.class );

	private static final Junction<MethodDescription> NOT_HIBERNATE_GENERATED = not( nameStartsWith( "$$_hibernate_" ) );
	private static final ModifierContributor.ForField REMOVE_PRIVATE_FINAL_MODIFIER = new ModifierContributor.ForField() {
		@Override
		public int getMask() {
			return EMPTY_MASK; // Do not add any modifier
		}

		@Override
		public int getRange() {
			return Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE; // Remove the "final" and "private" modifier
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	};
	private static final ModifierContributor.ForMethod REMOVE_PRIVATE_MODIFIER = new ModifierContributor.ForMethod() {
		@Override
		public int getMask() {
			return EMPTY_MASK; // Do not add any modifier
		}

		@Override
		public int getRange() {
			return Opcodes.ACC_PRIVATE; // Remove the "private" modifier
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	};

	private final TypeDescription managedCtClass;

	private final ByteBuddyEnhancementContext enhancementContext;

	private final TypePool classPool;

	private final AnnotatedFieldDescription[] enhancedFields;

	private PersistentAttributeTransformer(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			TypePool classPool,
			AnnotatedFieldDescription[] enhancedFields) {
		this.managedCtClass = managedCtClass;
		this.enhancementContext = enhancementContext;
		this.classPool = classPool;
		this.enhancedFields = enhancedFields;
	}

	public AnnotatedFieldDescription[] getEnhancedFields() {
		return enhancedFields;
	}

	public static PersistentAttributeTransformer collectPersistentFields(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			TypePool classPool) {
		List<AnnotatedFieldDescription> persistentFieldList = new ArrayList<>();
		// HHH-10646 Add fields inherited from @MappedSuperclass
		// HHH-10981 There is no need to do it for @MappedSuperclass
		// HHH-15505 This needs to be done first so that fields with the same name in the mappedsuperclass and entity are handled correctly
		if ( !enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			persistentFieldList.addAll( collectInheritPersistentFields( managedCtClass, enhancementContext ) );
		}
		for ( FieldDescription ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields and skip fields added by enhancement and  outer reference in inner classes
			if ( ctField.getName().startsWith( "$$_hibernate_" ) || "this$0".equals( ctField.getName() ) ) {
				continue;
			}
			AnnotatedFieldDescription annotatedField = new AnnotatedFieldDescription( enhancementContext, ctField );
			if ( !ctField.isStatic() && enhancementContext.isPersistentField( annotatedField ) ) {
				persistentFieldList.add( annotatedField );
			}
		}

		AnnotatedFieldDescription[] orderedFields = enhancementContext.order( persistentFieldList.toArray( new AnnotatedFieldDescription[0] ) );
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Persistent fields for entity %s: %s",
					managedCtClass.getName(),
					Arrays.toString( orderedFields )
			);
		}
		return new PersistentAttributeTransformer( managedCtClass, enhancementContext, classPool, orderedFields );
	}

	private static Collection<AnnotatedFieldDescription> collectInheritPersistentFields(
			TypeDefinition managedCtClass,
			ByteBuddyEnhancementContext enhancementContext) {
		if ( managedCtClass == null || managedCtClass.represents( Object.class ) ) {
			return Collections.emptyList();
		}
		TypeDefinition managedCtSuperclass = managedCtClass.getSuperClass();

		// If managedCtSuperclass is null, managedCtClass can be either interface or module-info.
		// Interfaces are already filtered-out, and module-info does not have any fields to enhance
		// so we can safely return empty list.
		if ( managedCtSuperclass == null || enhancementContext.isEntityClass( managedCtSuperclass.asErasure() ) ) {
			return Collections.emptyList();
		}
		else if ( !enhancementContext.isMappedSuperclassClass( managedCtSuperclass.asErasure() ) ) {
			return collectInheritPersistentFields( managedCtSuperclass, enhancementContext );
		}

		log.debugf( "Found @MappedSuperclass %s to collectPersistenceFields", managedCtSuperclass );

		List<AnnotatedFieldDescription> persistentFieldList = new ArrayList<>();

		for ( FieldDescription ctField : managedCtSuperclass.getDeclaredFields() ) {
			if ( ctField.getName().startsWith( "$$_hibernate_" ) || "this$0".equals( ctField.getName() ) ) {
				continue;
			}
			AnnotatedFieldDescription annotatedField = new AnnotatedFieldDescription( enhancementContext, ctField );
			if ( !ctField.isStatic() && enhancementContext.isPersistentField( annotatedField ) ) {
				persistentFieldList.add( annotatedField );
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
		return new MethodVisitor( OpenedClassReader.ASM_API, methodVisitor ) {
			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				AnnotatedFieldDescription enhancedField = getEnhancedField( owner, name, desc );
				if ( enhancedField != null ) {
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
							if ( enhancedField.getFieldDescription().isFinal() ) {
								// Final fields will only be written to from the constructor,
								// so there's no point trying to replace final field writes with a method call.
								break;
							}
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

	private AnnotatedFieldDescription getEnhancedField(String owner, String name, String desc) {
		for ( AnnotatedFieldDescription enhancedField : enhancedFields ) {
			if ( enhancedField.getName().equals( name )
					&& enhancedField.getDescriptor().equals( desc )
					&& enhancedField.getDeclaringType().asErasure().getInternalName().equals( owner ) ) {
				return enhancedField;
			}
		}
		return null;
	}

	DynamicType.Builder<?> applyTo(DynamicType.Builder<?> builder) {
		boolean compositeOwner = false;

		// Remove the private modifier from the constructor, which allows to create a better InstantiationOptimizer
		builder = builder.visit(
				new ModifierAdjustment().withConstructorModifiers(
						ElementMatchers.isDefaultConstructor(),
						REMOVE_PRIVATE_MODIFIER
				)
		);

		builder = builder.visit( new AsmVisitorWrapper.ForDeclaredMethods().invokable( NOT_HIBERNATE_GENERATED, this ) );
		// Remove the final modifier from all enhanced fields, because:
		// 1. We sometimes need to write to final fields when they are lazy.
		// 2. Those fields are already written to by Hibernate ORM through reflection anyway.
		// 3. The compiler already makes sure that final fields are not written to from the user's source code.
		List<FieldDescription.InDefinedShape> enhancedFieldsAsDefined = new ArrayList<>();
		for ( AnnotatedFieldDescription f : enhancedFields ) {
			enhancedFieldsAsDefined.add( f.asDefined() );
		}
		// Replace the private modifier with package-private for all enhanced fields,
		// because our AccessOptimizer needs to get/set fields and can't use field reader/writer,
		// as reader/writer methods have different semantics
		builder = builder.visit(
				new ModifierAdjustment().withFieldModifiers(
						anyOf( enhancedFieldsAsDefined ),
						REMOVE_PRIVATE_FINAL_MODIFIER
				)
		);
		for ( AnnotatedFieldDescription enhancedField : enhancedFields ) {
			builder = builder
					.defineMethod(
							EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + enhancedField.getName(),
							enhancedField.asDefined().getType().asErasure(),
							Visibility.PUBLIC
					)
					.intercept( fieldReader( enhancedField ) );
			// Final fields will only be written to from the constructor,
			// so there's no point trying to replace final field writes with a method call.
			if ( !enhancedField.getFieldDescription().isFinal() ) {
				builder = builder
						.defineMethod(
								EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + enhancedField.getName(),
								TypeDescription.VOID,
								Visibility.PUBLIC
						)
						.withParameters( enhancedField.asDefined().getType().asErasure() )
						.intercept( fieldWriter( enhancedField ) );
			}

			if ( !compositeOwner
					&& !enhancementContext.isMappedSuperclassClass( managedCtClass )
					&& enhancementContext.isCompositeField( enhancedField )
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

	private Implementation fieldReader(AnnotatedFieldDescription enhancedField) {
		if ( enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			return FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() );
		}
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

	private Implementation fieldWriter(AnnotatedFieldDescription enhancedField) {
		Implementation implementation = fieldWriterImplementation( enhancedField );
		if ( !enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			implementation = InlineDirtyCheckingHandler.wrap( managedCtClass, enhancementContext, enhancedField, implementation );
			implementation = BiDirectionalAssociationHandler.wrap( managedCtClass, enhancementContext, enhancedField, implementation );
		}
		return implementation;
	}

	private Implementation fieldWriterImplementation(AnnotatedFieldDescription enhancedField) {
		if ( enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			return FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() );
		}
		if ( !enhancementContext.hasLazyLoadableAttributes( managedCtClass ) || !enhancementContext.isLazyLoadable( enhancedField ) ) {
			if ( enhancedField.getDeclaringType().asErasure().equals( managedCtClass ) ) {
				return FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() );
			}
			else {
				return new Implementation.Simple( new FieldMethodWriter( managedCtClass, enhancedField ) );
			}
		}
		else {
			return new Implementation.Simple( FieldWriterAppender.of( managedCtClass, enhancedField ) );
		}
	}

	DynamicType.Builder<?> applyExtended(DynamicType.Builder<?> builder) {
		AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper enhancer = new FieldAccessEnhancer( managedCtClass, enhancementContext, classPool );
		return builder.visit( new AsmVisitorWrapper.ForDeclaredMethods().invokable( NOT_HIBERNATE_GENERATED, enhancer ) );
	}

	private static class FieldMethodReader implements ByteCodeAppender {

		private final TypeDescription managedCtClass;

		private final AnnotatedFieldDescription persistentField;

		private FieldMethodReader(TypeDescription managedCtClass, AnnotatedFieldDescription persistentField) {
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
					Type.getMethodDescriptor( Type.getType( persistentField.asDefined().getType().asErasure().getDescriptor() ) ),
					false
			);
			methodVisitor.visitInsn( Type.getType( persistentField.asDefined().getType().asErasure().getDescriptor() ).getOpcode( Opcodes.IRETURN ) );
			return new Size( persistentField.getType().getStackSize().getSize(), instrumentedMethod.getStackSize() );
		}
	}

	private static class FieldMethodWriter implements ByteCodeAppender {

		private final TypeDescription managedCtClass;

		private final AnnotatedFieldDescription persistentField;

		private FieldMethodWriter(TypeDescription managedCtClass, AnnotatedFieldDescription persistentField) {
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
			methodVisitor.visitVarInsn( Type.getType( persistentField.asDefined().getType().asErasure().getDescriptor() ).getOpcode( Opcodes.ILOAD ), 1 );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					managedCtClass.getSuperClass().asErasure().getInternalName(),
					EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + persistentField.getName(),
					Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( persistentField.asDefined().getType().asErasure().getDescriptor() ) ),
					false
			);
			methodVisitor.visitInsn( Opcodes.RETURN );
			return new Size( 1 + persistentField.getType().getStackSize().getSize(), instrumentedMethod.getStackSize() );
		}
	}

	@Override
	public boolean equals(final Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || PersistentAttributeTransformer.class != o.getClass() ) {
			return false;
		}
		final PersistentAttributeTransformer that = (PersistentAttributeTransformer) o;
		return Objects.equals( managedCtClass, that.managedCtClass );
	}

	@Override
	public int hashCode() {
		return managedCtClass.hashCode();
	}
}
