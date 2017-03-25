/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Collection;
import java.util.Map;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

class BiDirectionalAssociationHandler implements Implementation {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( BiDirectionalAssociationHandler.class );

	static Implementation wrap(
			TypeDescription managedCtClass,
			ByteBuddyEnhancementContext enhancementContext,
			FieldDescription persistentField,
			Implementation implementation) {
		if ( !enhancementContext.doBiDirectionalAssociationManagement( persistentField ) ) {
			return implementation;
		}

		TypeDescription targetEntity = getTargetEntityClass( managedCtClass, persistentField );
		if ( targetEntity == null ) {
			return implementation;
		}
		String mappedBy = getMappedBy( persistentField, targetEntity, enhancementContext );
		if ( mappedBy == null || mappedBy.isEmpty() ) {
			log.infof(
					"Could not find bi-directional association for field [%s#%s]",
					managedCtClass.getName(),
					persistentField.getName()
			);
			return implementation;
		}

		TypeDescription targetType = FieldLocator.ForClassHierarchy.Factory.INSTANCE.make( targetEntity )
				.locate( mappedBy )
				.getField()
				.getType()
				.asErasure();

		if ( EnhancerImpl.isAnnotationPresent( persistentField, OneToOne.class ) ) {
			implementation = Advice.withCustomMapping()
					.bind( CodeTemplates.FieldValue.class, persistentField )
					.bind( CodeTemplates.MappedBy.class, mappedBy )
					.to( CodeTemplates.OneToOneHandler.class )
					.wrap( implementation );
		}

		if ( EnhancerImpl.isAnnotationPresent( persistentField, OneToMany.class ) ) {
			implementation = Advice.withCustomMapping()
					.bind( CodeTemplates.FieldValue.class, persistentField )
					.bind( CodeTemplates.MappedBy.class, mappedBy )
					.to( persistentField.getType().asErasure().isAssignableTo( Map.class )
								? CodeTemplates.OneToManyOnMapHandler.class
								: CodeTemplates.OneToManyOnCollectionHandler.class )
					.wrap( implementation );
		}

		if ( EnhancerImpl.isAnnotationPresent( persistentField, ManyToOne.class ) ) {
			implementation = Advice.withCustomMapping()
					.bind( CodeTemplates.FieldValue.class, persistentField )
					.bind( CodeTemplates.MappedBy.class, mappedBy )
					.to( CodeTemplates.ManyToOneHandler.class )
					.wrap( implementation );
		}

		if ( EnhancerImpl.isAnnotationPresent( persistentField, ManyToMany.class ) ) {

			if ( persistentField.getType().asErasure().isAssignableTo( Map.class ) || targetType.isAssignableTo( Map.class ) ) {
				log.infof(
						"Bi-directional association for field [%s#%s] not managed: @ManyToMany in java.util.Map attribute not supported ",
						managedCtClass.getName(),
						persistentField.getName()
				);
				return implementation;
			}

			implementation = Advice.withCustomMapping()
					.bind( CodeTemplates.FieldValue.class, persistentField )
					.bind( CodeTemplates.MappedBy.class, mappedBy )
					.to( CodeTemplates.ManyToManyHandler.class )
					.wrap( implementation );
		}

		return new BiDirectionalAssociationHandler( implementation, targetEntity, targetType, mappedBy );
	}

	public static TypeDescription getTargetEntityClass(TypeDescription managedCtClass, FieldDescription persistentField) {
		try {
			AnnotationDescription.Loadable<OneToOne> oto = EnhancerImpl.getAnnotation( persistentField, OneToOne.class );
			AnnotationDescription.Loadable<OneToMany> otm = EnhancerImpl.getAnnotation( persistentField, OneToMany.class );
			AnnotationDescription.Loadable<ManyToOne> mto = EnhancerImpl.getAnnotation( persistentField, ManyToOne.class );
			AnnotationDescription.Loadable<ManyToMany> mtm = EnhancerImpl.getAnnotation( persistentField, ManyToMany.class );

			if ( oto == null && otm == null && mto == null && mtm == null ) {
				return null;
			}

			AnnotationValue<?, ?> targetClass = null;
			if ( oto != null ) {
				targetClass = oto.getValue( new MethodDescription.ForLoadedMethod( OneToOne.class.getDeclaredMethod( "targetEntity" ) ) );
			}
			if ( otm != null ) {
				targetClass = otm.getValue( new MethodDescription.ForLoadedMethod( OneToMany.class.getDeclaredMethod( "targetEntity" ) ) );
			}
			if ( mto != null ) {
				targetClass = mto.getValue( new MethodDescription.ForLoadedMethod( ManyToOne.class.getDeclaredMethod( "targetEntity" ) ) );
			}
			if ( mtm != null ) {
				targetClass = mtm.getValue( new MethodDescription.ForLoadedMethod( ManyToMany.class.getDeclaredMethod( "targetEntity" ) ) );
			}

			if ( targetClass == null ) {
				log.infof(
						"Could not find type of bi-directional association for field [%s#%s]",
						managedCtClass.getName(),
						persistentField.getName()
				);
				return null;
			}
			else if ( !targetClass.resolve( TypeDescription.class ).represents( void.class ) ) {
				return targetClass.resolve( TypeDescription.class );
			}
		}
		catch (NoSuchMethodException ignored) {
		}

		return entityType( target( persistentField ) );
	}

	private static TypeDescription.Generic target(FieldDescription persistentField) {
		AnnotationDescription.Loadable<Access> access = persistentField.getDeclaringType().asErasure().getDeclaredAnnotations().ofType( Access.class );
		if ( access != null && access.loadSilent().value() == AccessType.FIELD ) {
			return persistentField.getType();
		}
		else {
			MethodDescription getter = EnhancerImpl.getterOf( persistentField );
			if ( getter == null ) {
				return persistentField.getType();
			}
			else {
				return getter.getReturnType();
			}
		}
	}

	private static String getMappedBy(FieldDescription target, TypeDescription targetEntity, ByteBuddyEnhancementContext context) {
		String mappedBy = getMappedByNotManyToMany( target );
		if ( mappedBy == null || mappedBy.isEmpty() ) {
			return getMappedByManyToMany( target, targetEntity, context );
		}
		else {
			return mappedBy;
		}
	}

	private static String getMappedByNotManyToMany(FieldDescription target) {
		try {
			AnnotationDescription.Loadable<OneToOne> oto = EnhancerImpl.getAnnotation( target, OneToOne.class );
			if ( oto != null ) {
				return oto.getValue( new MethodDescription.ForLoadedMethod( OneToOne.class.getDeclaredMethod( "mappedBy" ) ) ).resolve( String.class );
			}

			AnnotationDescription.Loadable<OneToMany> otm = EnhancerImpl.getAnnotation( target, OneToMany.class );
			if ( otm != null ) {
				return otm.getValue( new MethodDescription.ForLoadedMethod( OneToMany.class.getDeclaredMethod( "mappedBy" ) ) ).resolve( String.class );
			}

			AnnotationDescription.Loadable<ManyToMany> mtm = EnhancerImpl.getAnnotation( target, ManyToMany.class );
			if ( mtm != null ) {
				return mtm.getValue( new MethodDescription.ForLoadedMethod( ManyToMany.class.getDeclaredMethod( "mappedBy" ) ) ).resolve( String.class );
			}
		}
		catch (NoSuchMethodException ignored) {
		}

		return null;
	}

	private static String getMappedByManyToMany(FieldDescription target, TypeDescription targetEntity, ByteBuddyEnhancementContext context) {
		for ( FieldDescription f : targetEntity.getDeclaredFields() ) {
			if ( context.isPersistentField( f )
					&& target.getName().equals( getMappedByNotManyToMany( f ) )
					&& target.getDeclaringType().asErasure().isAssignableTo( entityType( f.getType() ) ) ) {
				log.debugf(
						"mappedBy association for field [%s#%s] is [%s#%s]",
						target.getDeclaringType().asErasure().getName(),
						target.getName(),
						targetEntity.getName(),
						f.getName()
				);
				return f.getName();
			}
		}
		return null;
	}

	private static TypeDescription entityType(TypeDescription.Generic type) {
		if ( type.getSort().isParameterized() ) {
			if ( type.asErasure().isAssignableTo( Collection.class ) ) {
				return type.getTypeArguments().get( 0 ).asErasure();
			}
			if ( type.asErasure().isAssignableTo( Map.class ) ) {
				return type.getTypeArguments().get( 1 ).asErasure();
			}
		}

		return type.asErasure();
	}

	private final Implementation delegate;

	private final TypeDescription targetEntity;

	private final TypeDescription targetType;

	private final String mappedBy;

	private BiDirectionalAssociationHandler(
			Implementation delegate,
			TypeDescription targetEntity,
			TypeDescription targetType,
			String mappedBy) {
		this.delegate = delegate;
		this.targetEntity = targetEntity;
		this.targetType = targetType;
		this.mappedBy = mappedBy;
	}

	@Override
	public ByteCodeAppender appender(Target implementationTarget) {
		return new WrappingAppender( delegate.appender( implementationTarget ) );
	}

	@Override
	public InstrumentedType prepare(InstrumentedType instrumentedType) {
		return delegate.prepare( instrumentedType );
	}

	private class WrappingAppender implements ByteCodeAppender {

		private final ByteCodeAppender delegate;

		private WrappingAppender(ByteCodeAppender delegate) {
			this.delegate = delegate;
		}

		@Override
		public Size apply(
				MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
			return delegate.apply( new MethodVisitor( Opcodes.ASM5, methodVisitor ) {

				@Override
				public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
					if ( owner.startsWith( Type.getInternalName( CodeTemplates.class ) ) ) {
						if ( name.equals( "getter" ) ) {
							super.visitTypeInsn( Opcodes.CHECKCAST, targetEntity.getInternalName() );
							super.visitMethodInsn(
									Opcodes.INVOKEVIRTUAL,
									targetEntity.getInternalName(),
									EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + mappedBy,
									Type.getMethodDescriptor( Type.getType( targetType.getDescriptor() ) ),
									false
							);
						}
						else if ( name.equals( "setterSelf" ) ) {
							super.visitInsn( Opcodes.POP );
							super.visitTypeInsn( Opcodes.CHECKCAST, targetEntity.getInternalName() );
							super.visitVarInsn( Opcodes.ALOAD, 0 );
							super.visitMethodInsn(
									Opcodes.INVOKEVIRTUAL,
									targetEntity.getInternalName(),
									EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + mappedBy,
									Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( targetType.getDescriptor() ) ),
									false
							);
						}
						else if ( name.equals( "setterNull" ) ) {
							super.visitInsn( Opcodes.POP );
							super.visitTypeInsn( Opcodes.CHECKCAST, targetEntity.getInternalName() );
							super.visitInsn( Opcodes.ACONST_NULL );
							super.visitMethodInsn(
									Opcodes.INVOKEVIRTUAL,
									targetEntity.getInternalName(),
									EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + mappedBy,
									Type.getMethodDescriptor( Type.getType( void.class ), Type.getType( targetType.getDescriptor() ) ),
									false
							);
						}
						else {
							throw new EnhancementException( "Unknown template method: " + name );
						}
					}
					else {
						super.visitMethodInsn( opcode, owner, name, desc, itf );
					}
				}
			}, implementationContext, instrumentedMethod );
		}
	}
}
