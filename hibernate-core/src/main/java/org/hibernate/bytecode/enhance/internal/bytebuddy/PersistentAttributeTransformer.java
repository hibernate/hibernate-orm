package org.hibernate.bytecode.enhance.internal.bytebuddy;

import static java.util.Collections.emptyList;
import static net.bytebuddy.matcher.ElementMatchers.anyOf;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.hibernate.bytecode.enhance.internal.BytecodeEnhancementLogging.ENHANCEMENT_LOGGER;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;

import org.hibernate.annotations.Immutable;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.ModifierAdjustment;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;

final class PersistentAttributeTransformer implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

	private static final Junction<MethodDescription> NOT_HIBERNATE_GENERATED = not( nameStartsWith( "$$_hibernate_" ) );
	private static final String IMMUTABLE_DESCRIPTOR = Type.getDescriptor( Immutable.class );
	private static final Set<String> IMMUTABLE_BASIC_TYPES =
			Set.of( String.class.getName(), BigDecimal.class.getName(), BigInteger.class.getName() );
	private static final ModifierContributor.ForType REMOVE_FINAL_TYPE_MODIFIER = new ModifierContributor.ForType() {
		@Override
		public int getMask() {
			return EMPTY_MASK; // Do not add any modifier
		}

		@Override
		public int getRange() {
			return Opcodes.ACC_FINAL; // Remove the "final" modifier
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	};
	private static final ModifierContributor.ForMethod REMOVE_FINAL_METHOD_MODIFIER = new ModifierContributor.ForMethod() {
		@Override
		public int getMask() {
			return EMPTY_MASK; // Do not add any modifier
		}

		@Override
		public int getRange() {
			return Opcodes.ACC_FINAL; // Remove the "final" modifier
		}

		@Override
		public boolean isDefault() {
			return false;
		}
	};
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

	private final EnhancerImplConstants constants;

	private PersistentAttributeTransformer(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			TypePool classPool,
			AnnotatedFieldDescription[] enhancedFields,
			EnhancerImplConstants constants) {
		this.managedCtClass = managedCtClass;
		this.enhancementContext = enhancementContext;
		this.classPool = classPool;
		this.enhancedFields = enhancedFields;
		this.constants = constants;
	}

	public AnnotatedFieldDescription[] getEnhancedFields() {
		return enhancedFields;
	}

	public static PersistentAttributeTransformer collectPersistentFields(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			TypePool classPool,
			EnhancerImplConstants constants) {
		final List<AnnotatedFieldDescription> persistentFieldList = new ArrayList<>();
		// HHH-10646 Add fields inherited from @MappedSuperclass
		// HHH-10981 There is no need to do it for @MappedSuperclass
		// HHH-15505 This needs to be done first so that fields with the same name in the mappedsuperclass and entity are handled correctly
		if ( !enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			persistentFieldList.addAll( collectInheritPersistentFields( managedCtClass, enhancementContext ) );
		}
		for ( var ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields and skip fields added by enhancement and  outer reference in inner classes
			if ( !ctField.getName().startsWith( "$$_hibernate_" )
				&& !"this$0".equals( ctField.getName() ) ) {
				final var annotatedField = new AnnotatedFieldDescription( enhancementContext, ctField );
				if ( !ctField.isStatic() && enhancementContext.isPersistentField( annotatedField ) ) {
					persistentFieldList.add( annotatedField );
				}
			}
		}

		final var orderedFields =
				enhancementContext.order( persistentFieldList.toArray( new AnnotatedFieldDescription[0] ) );
		if ( ENHANCEMENT_LOGGER.isTraceEnabled() ) {
			ENHANCEMENT_LOGGER.persistentFieldsForEntity(
					managedCtClass.getName(),
					Arrays.toString( orderedFields )
			);
		}
		return new PersistentAttributeTransformer( managedCtClass, enhancementContext, classPool, orderedFields, constants );
	}

	private static Collection<AnnotatedFieldDescription> collectInheritPersistentFields(
			TypeDefinition managedCtClass,
			ByteBuddyEnhancementContext enhancementContext) {
		if ( managedCtClass == null || managedCtClass.represents( Object.class ) ) {
			return emptyList();
		}
		final var managedCtSuperclass = managedCtClass.getSuperClass();
		// If managedCtSuperclass is null, managedCtClass can be either interface or module-info.
		// Interfaces are already filtered-out, and module-info does not have any fields to enhance
		// so we can safely return empty list.
		if ( managedCtSuperclass == null || enhancementContext.isEntityClass( managedCtSuperclass.asErasure() ) ) {
			return emptyList();
		}
		else if ( !enhancementContext.isMappedSuperclassClass( managedCtSuperclass.asErasure() ) ) {
			return collectInheritPersistentFields( managedCtSuperclass, enhancementContext );
		}

		ENHANCEMENT_LOGGER.foundMappedSuperclass( String.valueOf( managedCtSuperclass ) );

		final List<AnnotatedFieldDescription> persistentFieldList = new ArrayList<>();

		for ( var ctField : managedCtSuperclass.getDeclaredFields() ) {
			if ( !ctField.getName().startsWith( "$$_hibernate_" )
				&& !"this$0".equals( ctField.getName() ) ) {
				final var annotatedField = new AnnotatedFieldDescription( enhancementContext, ctField );
				if ( !ctField.isStatic() && enhancementContext.isPersistentField( annotatedField ) ) {
					persistentFieldList.add( annotatedField );
				}
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
		if ( instrumentedMethod.isConstructor() ) {
			return wrapMethodWithPreSuperFieldAccess( methodVisitor );
		}
		return wrapMethodWithoutPreSuperFieldAccess( methodVisitor );
	}

	private MethodVisitor wrapMethodWithoutPreSuperFieldAccess(MethodVisitor methodVisitor) {
		return new MethodVisitor( OpenedClassReader.ASM_API, methodVisitor ) {
			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				if ( !emitFieldAccessReplacement( methodVisitor, opcode, owner, name, desc ) ) {
					super.visitFieldInsn( opcode, owner, name, desc );
				}
			}
		};
	}

	private boolean emitFieldAccessReplacement(
			MethodVisitor mv, int opcode, String owner, String name, String desc) {
		final var enhancedField = getEnhancedField( owner, name, desc );
		if ( enhancedField != null ) {
			switch ( opcode ) {
				case Opcodes.GETFIELD:
					mv.visitMethodInsn(
							Opcodes.INVOKEVIRTUAL,
							owner,
							PERSISTENT_FIELD_READER_PREFIX + name,
							"()" + desc,
							false
					);
					return true;
				case Opcodes.PUTFIELD:
					// Final fields will only be written to from the constructor,
					// so there's no point trying to replace final field writes with a method call.
					if ( !enhancedField.getFieldDescription().isFinal() ) {
						mv.visitMethodInsn(
								Opcodes.INVOKEVIRTUAL,
								owner,
								PERSISTENT_FIELD_WRITER_PREFIX + name,
								"(" + desc + ")V",
								false
						);
						return true;
					}
					break;
			}
		}
		return false;
	}

	/**
	 * Handles constructors that may use Java 25+ flexible constructors (JEP 513),
	 * where field writes occur before {@code super()}.
	 * <p>
	 * Before {@code super()}, {@code this} is {@code uninitializedThis} -- {@code INVOKEVIRTUAL}
	 * is illegal on it, so we cannot replace {@code PUTFIELD}/{@code GETFIELD} with the
	 * {@code $$_hibernate_write/read_<field>} calls. Field accesses on other (fully initialized)
	 * objects are still replaced normally.
	 * <p>
	 * A {@link UninitializedThisTracker} determines at transformation time whether each
	 * {@code PUTFIELD}/{@code GETFIELD} targets {@code this} or another object.
	 * <p>
	 * For field writes on {@code this}, we keep the original {@code PUTFIELD} and replay the
	 * dirty-tracking side effects after {@code super()}. Boolean locals track which fields were
	 * written with a value that differs from the JVM default ({@code null}/{@code 0}). These
	 * booleans are checked after {@code super()} to emit deferred dirty tracking and composite
	 * owner setup.
	 */
	private MethodVisitor wrapMethodWithPreSuperFieldAccess(MethodVisitor methodVisitor) {
		final var trackerHolder = new UninitializedThisTracker[1];
		var recorder = new RecordingMethodVisitor( OpenedClassReader.ASM_API, methodVisitor ) {
			private final List<AnnotatedFieldDescription> enhancedFieldsWrittenBeforeSuper =
					new ArrayList<>();

			private AnnotatedFieldDescription getDirtyTrackableField(
					String owner, String name, String desc) {
				if ( !enhancementContext.doDirtyCheckingInline() ) {
					return null;
				}
				for ( var field : enhancedFields ) {
					if ( field.getName().equals( name )
							&& field.getDescriptor().equals( desc )
							&& field.getDeclaringType().asErasure()
									.getInternalName().equals( owner )
							&& !field.getFieldDescription().isFinal()
							&& InlineDirtyCheckingHandler.isDirtyTrackable(
									enhancementContext, managedCtClass, field ) ) {
						return field;
					}
				}
				return null;
			}

			@Override
			public void visitCode() {
				record( mv -> {
					mv.visitCode();
					for ( int i = 0; i < enhancedFieldsWrittenBeforeSuper.size(); i++ ) {
						mv.visitInsn( Opcodes.ICONST_0 );
						mv.visitVarInsn( Opcodes.ISTORE, getMaxLocals() + i );
					}
				} );
			}

			@Override
			public void visitFieldInsn(
					int opcode, String owner, String name, String desc) {
				var tracker = trackerHolder[0];
				if ( !tracker.isBeforeSuperCall() ) {
					record( mv -> {
						if ( !emitFieldAccessReplacement(
								mv, opcode, owner, name, desc ) ) {
							mv.visitFieldInsn( opcode, owner, name, desc );
						}
					} );
					return;
				}
				if ( tracker.isTrackingLost()
						&& ( opcode == Opcodes.PUTFIELD
								|| opcode == Opcodes.GETFIELD ) ) {
					if ( !enhancementContext.doLenientConstructorTracking() ) {
						throw new EnhancementException(
								"Failed to analyze constructor bytecode"
										+ " before super() call in "
										+ managedCtClass.getName() + "."
										+ " Set lenientConstructorTracking = true"
										+ " in your enhancement configuration"
										+ " to fall back to skipping enhancement"
										+ " for this constructor"
										+ " (Gradle/Ant: lenientConstructorTracking = true,"
										+ " Maven: <lenientConstructorTracking>true"
										+ "</lenientConstructorTracking>)."
						);
					}
					record( mv -> mv.visitFieldInsn(
							opcode, owner, name, desc ) );
					return;
				}
				if ( tracker.lastFieldTargetedThis() ) {
					if ( opcode == Opcodes.PUTFIELD ) {
						var field = getDirtyTrackableField(
								owner, name, desc );
						if ( field != null ) {
							var idx = enhancedFieldsWrittenBeforeSuper
									.indexOf( field );
							if ( idx < 0 ) {
								idx = enhancedFieldsWrittenBeforeSuper
										.size();
								enhancedFieldsWrittenBeforeSuper.add(
										field );
							}
							final var boolIdx = idx;
							record( mv -> emitDefaultValueComparison(
									mv, desc, boolIdx ) );
						}
					}
					record( mv -> mv.visitFieldInsn(
							opcode, owner, name, desc ) );
				}
				else {
					record( mv -> {
						if ( !emitFieldAccessReplacement(
								mv, opcode, owner, name, desc ) ) {
							mv.visitFieldInsn(
									opcode, owner, name, desc );
						}
					} );
				}
			}

			@Override
			public void visitMethodInsn(
					int opcode, String owner, String name,
					String descriptor, boolean isInterface) {
				var tracker = trackerHolder[0];
				if ( tracker.lastMethodWasSuperCall() ) {
					record( mv -> {
						mv.visitMethodInsn(
								opcode, owner, name,
								descriptor, isInterface );
						emitConditionalDirtyTracking( mv );
					} );
					return;
				}
				super.visitMethodInsn(
						opcode, owner, name, descriptor, isInterface );
			}

			private void emitDefaultValueComparison(
					MethodVisitor mv, String desc, int boolIdx) {
				var skipLabel = new Label();
				if ( desc.startsWith( "L" ) || desc.startsWith( "[" ) ) {
					mv.visitInsn( Opcodes.DUP );
					mv.visitJumpInsn( Opcodes.IFNULL, skipLabel );
				}
				else if ( "J".equals( desc ) ) {
					mv.visitInsn( Opcodes.DUP2 );
					mv.visitInsn( Opcodes.LCONST_0 );
					mv.visitInsn( Opcodes.LCMP );
					mv.visitJumpInsn( Opcodes.IFEQ, skipLabel );
				}
				else if ( "D".equals( desc ) ) {
					mv.visitInsn( Opcodes.DUP2 );
					mv.visitInsn( Opcodes.DCONST_0 );
					mv.visitInsn( Opcodes.DCMPL );
					mv.visitJumpInsn( Opcodes.IFEQ, skipLabel );
				}
				else if ( "F".equals( desc ) ) {
					mv.visitInsn( Opcodes.DUP );
					mv.visitInsn( Opcodes.FCONST_0 );
					mv.visitInsn( Opcodes.FCMPL );
					mv.visitJumpInsn( Opcodes.IFEQ, skipLabel );
				}
				else {
					mv.visitInsn( Opcodes.DUP );
					mv.visitJumpInsn( Opcodes.IFEQ, skipLabel );
				}
				mv.visitInsn( Opcodes.ICONST_1 );
				mv.visitVarInsn( Opcodes.ISTORE, getMaxLocals() + boolIdx );
				mv.visitLabel( skipLabel );
			}

			private void emitConditionalDirtyTracking(MethodVisitor mv) {
				if ( enhancedFieldsWrittenBeforeSuper.isEmpty() ) {
					return;
				}
				final var owner = managedCtClass.getInternalName();
				for ( int i = 0; i < enhancedFieldsWrittenBeforeSuper.size(); i++ ) {
					final var field = enhancedFieldsWrittenBeforeSuper.get( i );
					final var skipLabel = new Label();
					mv.visitVarInsn( Opcodes.ILOAD, getMaxLocals() + i );
					mv.visitJumpInsn( Opcodes.IFEQ, skipLabel );
					if ( InlineDirtyCheckingHandler.isCompositeOwnerField(
								enhancementContext, field ) ) {
						CodeTemplates.CompositeFieldDirtyCheckingHandler
								.emitCompositeOwnerSetup(
										mv, owner, field.getName(),
										field.getDescriptor() );
					}
					InlineDirtyCheckingHandler.emitTrackChange(
							mv, owner, field.getName() );
					mv.visitLabel( skipLabel );
				}
			}
		};
		trackerHolder[0] = new UninitializedThisTracker( OpenedClassReader.ASM_API, recorder );
		return trackerHolder[0];
	}

	private AnnotatedFieldDescription getEnhancedField(String owner, String name, String desc) {
		TypeDefinition declaringType = null;
		for ( var enhancedField : enhancedFields ) {
			if ( enhancedField.getName().equals( name )
					&& enhancedField.getDescriptor().equals( desc ) ) {
				if ( enhancedField.getDeclaringType().asErasure().getInternalName().equals( owner ) ) {
					return enhancedField;
				}
				if ( declaringType == null ) {
					// The bytecode may name a subclass as the owner of an inherited field.
					// Resolve the declaration first, including any field hiding along the way.
					final var resolution = classPool.describe( owner.replace( '/', '.' ) );
					if ( !resolution.isResolved() ) {
						return null;
					}
					declaringType = FieldAccessEnhancer.findFields( resolution.resolve(), name, desc )
							.getOnly().getDeclaringType();
				}
				if ( enhancedField.getDeclaringType().asErasure().equals( declaringType.asErasure() ) ) {
					return enhancedField;
				}
			}
		}
		return null;
	}

	DynamicType.Builder<?> applyTo(DynamicType.Builder<?> builder) {
		boolean compositeOwner = false;

		if ( enhancementContext.isEntityClass( managedCtClass )
				|| enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			builder = builder.visit(
					new ModifierAdjustment()
							.withTypeModifiers( REMOVE_FINAL_TYPE_MODIFIER )
							.withMethodModifiers( NOT_HIBERNATE_GENERATED, REMOVE_FINAL_METHOD_MODIFIER )
			);
		}

		// Remove the private modifier from the constructor, which allows to create a better InstantiationOptimizer
		builder = builder.visit(
				new ModifierAdjustment().withConstructorModifiers(
						ElementMatchers.isDefaultConstructor(),
						REMOVE_PRIVATE_MODIFIER
				)
		);

		builder = builder.visit( new AsmVisitorWrapper.ForDeclaredMethods()
				.invokable( NOT_HIBERNATE_GENERATED, this )
				.writerFlags( ClassWriter.COMPUTE_FRAMES )
				.readerFlags( ClassReader.EXPAND_FRAMES ) );
		// Remove the final modifier from all enhanced fields, because:
		// 1. We sometimes need to write to final fields when they are lazy.
		// 2. Those fields are already written to by Hibernate ORM through reflection anyway.
		// 3. The compiler already makes sure that final fields are not written to from the user's source code.
		final List<FieldDescription.InDefinedShape> enhancedFieldsAsDefined = new ArrayList<>();
		for ( var fieldDescription : enhancedFields ) {
			enhancedFieldsAsDefined.add( fieldDescription.asDefined() );
		}
		final List<FieldDescription.InDefinedShape> immutableFinalEnhancedFieldsAsDefined = new ArrayList<>();
		for ( var fieldDescription : enhancedFields ) {
			if ( fieldDescription.getFieldDescription().isFinal()
					&& isImmutableBasicType( fieldDescription.getFieldDescription().getType() ) ) {
				immutableFinalEnhancedFieldsAsDefined.add( fieldDescription.asDefined() );
			}
		}
		if ( !immutableFinalEnhancedFieldsAsDefined.isEmpty() ) {
			builder = builder.visit(
					new AsmVisitorWrapper.ForDeclaredFields()
							.field( anyOf( immutableFinalEnhancedFieldsAsDefined ),
									ImmutableAnnotationAppender.INSTANCE )
			);
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
		for ( var enhancedField : enhancedFields ) {
			builder = builder
					.defineMethod(
							PERSISTENT_FIELD_READER_PREFIX + enhancedField.getName(),
							enhancedField.asDefined().getType().asErasure(),
							constants.modifierPUBLIC
					)
					.intercept( fieldReader( enhancedField ) );
			// Final fields will only be written to from the constructor,
			// so there's no point trying to replace final field writes with a method call.
			if ( !enhancedField.getFieldDescription().isFinal() ) {
				builder = builder
						.defineMethod(
								PERSISTENT_FIELD_WRITER_PREFIX + enhancedField.getName(),
								constants.TypeVoid,
								constants.modifierPUBLIC
						)
						.withParameter( enhancedField.asDefined().getType().asErasure() )
						.intercept( fieldWriter( enhancedField ) );
			}

			if ( !compositeOwner
					&& !enhancementContext.isMappedSuperclassClass( managedCtClass )
					&& enhancementContext.isCompositeField( enhancedField )
					&& enhancementContext.doDirtyCheckingInline() ) {
				compositeOwner = true;
			}
		}

		if ( compositeOwner ) {
			builder = builder.implement( constants.INTERFACES_for_CompositeOwner );

			if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
				builder = builder.defineMethod( EnhancerConstants.TRACKER_CHANGER_NAME, void.class, constants.modifierPUBLIC )
						.withParameter( constants.TypeString )
						.intercept( Advice.to( CodeTemplates.CompositeOwnerDirtyCheckingHandler.class ).wrap( StubMethod.INSTANCE ) );
			}
		}

		if ( enhancementContext.doExtendedEnhancement() ) {
			builder = applyExtended( builder );
		}

		return builder;
	}

	private Implementation fieldReader(AnnotatedFieldDescription enhancedField) {
		if ( enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			return FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() );
		}
		else if ( !enhancementContext.hasLazyLoadableAttributes( managedCtClass ) || !enhancementContext.isLazyLoadable( enhancedField ) ) {
			if ( enhancedField.getDeclaringType().asErasure().equals( managedCtClass ) ) {
				return FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() );
			}
			else {
				return new Implementation.Simple( new FieldMethodReader( managedCtClass, enhancedField ) );
			}
		}
		else {
			return new Implementation.Simple( FieldReaderAppender.of( managedCtClass, enhancedField, constants ) );
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
		else if ( !enhancementContext.hasLazyLoadableAttributes( managedCtClass ) || !enhancementContext.isLazyLoadable( enhancedField ) ) {
			if ( enhancedField.getDeclaringType().asErasure().equals( managedCtClass ) ) {
				return FieldAccessor.ofField( enhancedField.getName() ).in( enhancedField.getDeclaringType().asErasure() );
			}
			else {
				return new Implementation.Simple( new FieldMethodWriter( managedCtClass, enhancedField ) );
			}
		}
		else {
			return new Implementation.Simple( FieldWriterAppender.of( managedCtClass, enhancedField, constants ) );
		}
	}

	DynamicType.Builder<?> applyExtended(DynamicType.Builder<?> builder) {
		AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper enhancer = new FieldAccessEnhancer( managedCtClass, enhancementContext, classPool );
		return builder.visit( new AsmVisitorWrapper.ForDeclaredMethods().invokable( NOT_HIBERNATE_GENERATED, enhancer ) );
	}

	private enum ImmutableAnnotationAppender implements AsmVisitorWrapper.ForDeclaredFields.FieldVisitorWrapper {
		INSTANCE;

		@Override
		public FieldVisitor wrap(
				TypeDescription instrumentedType,
				FieldDescription.InDefinedShape fieldDescription,
				FieldVisitor fieldVisitor) {
			return new FieldVisitor( OpenedClassReader.ASM_API, fieldVisitor ) {
				private boolean immutableAnnotationSeen;

				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					if ( IMMUTABLE_DESCRIPTOR.equals( descriptor ) ) {
						immutableAnnotationSeen = true;
					}
					return super.visitAnnotation( descriptor, visible );
				}

				@Override
				public void visitEnd() {
					if ( !immutableAnnotationSeen ) {
						final var immutable = super.visitAnnotation( IMMUTABLE_DESCRIPTOR, true );
						if ( immutable != null ) {
							immutable.visitEnd();
						}
					}
					super.visitEnd();
				}
			};
		}
	}

	private static boolean isImmutableBasicType(TypeDefinition type) {
		if ( type.isArray() ) {
			return false;
		}
		else {
			final var erasure = type.asErasure();
			return type.isPrimitive()
				|| type.isEnum()
				|| erasure.isPrimitiveWrapper()
				|| IMMUTABLE_BASIC_TYPES.contains( erasure.getName() )
				|| erasure.getName().startsWith( "java.time." );
		}
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
			final String descriptor = persistentField.asDefined().getType().asErasure().getDescriptor();
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					managedCtClass.getSuperClass().asErasure().getInternalName(),
					PERSISTENT_FIELD_READER_PREFIX + persistentField.getName(),
					Type.getMethodDescriptor( Type.getType( descriptor ) ),
					false
			);
			methodVisitor.visitInsn( Type.getType( descriptor ).getOpcode( Opcodes.IRETURN ) );
			return new Size( persistentField.getType().getStackSize().getSize(),
					instrumentedMethod.getStackSize() );
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
			final String descriptor = persistentField.asDefined().getType().asErasure().getDescriptor();
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitVarInsn( Type.getType( descriptor ).getOpcode( Opcodes.ILOAD ), 1 );
			methodVisitor.visitMethodInsn(
					Opcodes.INVOKESPECIAL,
					managedCtClass.getSuperClass().asErasure().getInternalName(),
					PERSISTENT_FIELD_WRITER_PREFIX + persistentField.getName(),
					Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( descriptor ) ),
					false
			);
			methodVisitor.visitInsn( Opcodes.RETURN );
			return new Size( 1 + persistentField.getType().getStackSize().getSize(),
					instrumentedMethod.getStackSize() );
		}
	}

	@Override
	public boolean equals(final Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof PersistentAttributeTransformer that) ) {
			return false;
		}
		else {
			return Objects.equals( managedCtClass, that.managedCtClass );
		}
	}

	@Override
	public int hashCode() {
		return managedCtClass.hashCode();
	}
}
