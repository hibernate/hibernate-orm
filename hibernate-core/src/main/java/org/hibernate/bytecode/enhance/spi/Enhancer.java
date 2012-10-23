/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.bytecode.enhance.spi;

import javax.persistence.Transient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.IdentityHashMap;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.annotation.Annotation;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.EnhancementException;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.ManagedComposite;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.internal.CoreMessageLogger;

/**
 * @author Steve Ebersole
 * @author Jason Greene
 */
public class Enhancer  {
	private static final CoreMessageLogger log = Logger.getMessageLogger( CoreMessageLogger.class, Enhancer.class.getName() );

	private static final String PERSISTENT_FIELD_READER_PREFIX = "$hibernate_read_";
	private static final String PERSISTENT_FIELD_WRITER_PREFIX = "$hibernate_write_";

	public static final String ENTITY_INSTANCE_GETTER_NAME = "hibernate_getEntityInstance";

	public static final String ENTITY_ENTRY_FIELD_NAME = "$hibernate_entityEntryHolder";
	public static final String ENTITY_ENTRY_GETTER_NAME = "hibernate_getEntityEntry";
	public static final String ENTITY_ENTRY_SETTER_NAME = "hibernate_setEntityEntry";

	public static final String PREVIOUS_FIELD_NAME = "$hibernate_previousManagedEntity";
	public static final String PREVIOUS_GETTER_NAME = "hibernate_getPreviousManagedEntity";
	public static final String PREVIOUS_SETTER_NAME = "hibernate_setPreviousManagedEntity";

	public static final String NEXT_FIELD_NAME = "$hibernate_nextManagedEntity";
	public static final String NEXT_GETTER_NAME = "hibernate_getNextManagedEntity";
	public static final String NEXT_SETTER_NAME = "hibernate_setNextManagedEntity";

	private final EnhancementContext enhancementContext;

	private final ClassPool classPool;
	private final CtClass managedEntityCtClass;
	private final CtClass managedCompositeCtClass;
	private final CtClass entityEntryCtClass;
	private final CtClass objectCtClass;

	public Enhancer(EnhancementContext enhancementContext) {
		this.enhancementContext = enhancementContext;

		this.classPool = new ClassPool( false );
		try {
			// add ManagedEntity contract
			this.managedEntityCtClass = classPool.makeClass(
					ManagedEntity.class.getClassLoader().getResourceAsStream(
							ManagedEntity.class.getName().replace( '.', '/' ) + ".class"
					)
			);

			// add ManagedEntity contract
			this.managedCompositeCtClass = classPool.makeClass(
					ManagedComposite.class.getClassLoader().getResourceAsStream(
							ManagedComposite.class.getName().replace( '.', '/' ) + ".class"
					)
			);

			// "add" EntityEntry
			this.entityEntryCtClass = classPool.makeClass( EntityEntry.class.getName() );

			this.objectCtClass = classPool.makeClass( Object.class.getName() );
		}
		catch (IOException e) {
			throw new EnhancementException( "Could not prepare Javassist ClassPool", e );
		}
	}

	/**
	 * Performs the enhancement.
	 *
	 * @param className The name of the class whose bytecode is being enhanced.
	 * @param originalBytes The class's original (pre-enhancement) byte code
	 *
	 * @return The enhanced bytecode.  Could be the same as the original bytecode if the original was
	 * already enhanced or we could not enhance it for some reason.
	 *
	 * @throws EnhancementException
	 */
	public byte[] enhance(String className, byte[] originalBytes) throws EnhancementException {
		final CtClass managedCtClass;
		try {
			managedCtClass = classPool.makeClassIfNew( new ByteArrayInputStream( originalBytes ) );
		}
		catch (IOException e) {
			log.unableToBuildEnhancementMetamodel( className );
			return originalBytes;
		}

		enhance( managedCtClass );

		DataOutputStream out = null;
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			out = new DataOutputStream( byteStream );
			managedCtClass.toBytecode( out );
			return byteStream.toByteArray();
		}
		catch (Exception e) {
			log.unableToTransformClass( e.getMessage() );
			throw new HibernateException( "Unable to transform class: " + e.getMessage() );
		}
		finally {
			try {
				if ( out != null ) {
					out.close();
				}
			}
			catch (IOException e) {
				//swallow
			}
		}
	}

	private void enhance(CtClass managedCtClass) {
		final String className = managedCtClass.getName();
		log.debugf( "Enhancing %s", className );

		// can't effectively enhance interfaces
		if ( managedCtClass.isInterface() ) {
			log.debug( "skipping enhancement : interface" );
			return;
		}

		// skip already enhanced classes

		final String[] interfaceNames = managedCtClass.getClassFile2().getInterfaces();
		for ( String interfaceName : interfaceNames ) {
			if ( Managed.class.getName().equals( interfaceName ) ) {
				log.debug( "skipping enhancement : already enhanced" );
				return;
			}
		}

		if ( enhancementContext.isEntityClass( className ) ) {
			enhanceAsEntity( managedCtClass );
		}
		else if ( enhancementContext.isCompositeClass( className ) ) {
			enhanceAsComposite( managedCtClass );
		}
		else {
			log.debug( "skipping enhancement : not entity or composite" );
		}
	}

	private void enhanceAsEntity(CtClass managedCtClass) {
		final ConstPool constPool = managedCtClass.getClassFile().getConstPool();

		// add the ManagedEntity interface
		managedCtClass.addInterface( managedEntityCtClass );

//		enhancePersistentAttributes( managedCtClass );

		addEntityInstanceHandling( managedCtClass );
		addEntityEntryHandling( managedCtClass );
		addLinkedPreviousHandling( managedCtClass );
		addLinkedNextHandling( managedCtClass );
	}

	private void enhanceAsComposite(CtClass classFile) {
	}

	private void addEntityInstanceHandling(CtClass managedCtClass) {
		// add the ManagedEntity#hibernate_getEntityInstance method
		try {
			managedCtClass.addMethod(
					CtNewMethod.make(
							objectCtClass,
							ENTITY_INSTANCE_GETTER_NAME,
							new CtClass[0],
							new CtClass[0],
							"{ return this; }",
							managedCtClass
					)
			);
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add EntityEntry getter",
							managedCtClass.getName()
					),
					e
			);
		}

		// essentially add `return this;`
	}

	private void addEntityEntryHandling(CtClass managedCtClass) {
		final ConstPool constPool = managedCtClass.getClassFile().getConstPool();

		// add field to hold EntityEntry
		final CtField entityEntryField;
		try {
			entityEntryField = new CtField( entityEntryCtClass, ENTITY_ENTRY_FIELD_NAME, managedCtClass );
			managedCtClass.addField( entityEntryField );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add field for holding EntityEntry",
							managedCtClass.getName()
					),
					e
			);
		}

		// make that new field transient and @Transient
		entityEntryField.setModifiers( entityEntryField.getModifiers() | Modifier.TRANSIENT );
		entityEntryField.setModifiers( Modifier.setPrivate( entityEntryField.getModifiers() ) );
		AnnotationsAttribute annotationsAttribute = getVisibleAnnotations( entityEntryField.getFieldInfo() );
		annotationsAttribute.addAnnotation( new Annotation( Transient.class.getName(), constPool ) );

		// add the ManagedEntity#hibernate_getEntityEntry method
		try {
			managedCtClass.addMethod(
					CtNewMethod.getter( ENTITY_ENTRY_GETTER_NAME, entityEntryField )
			);
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add EntityEntry getter",
							managedCtClass.getName()
					),
					e
			);
		}

		// add the ManagedEntity#hibernate_setEntityEntry method
		try {
			managedCtClass.addMethod(
					CtNewMethod.setter( ENTITY_ENTRY_SETTER_NAME, entityEntryField )
			);
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add EntityEntry setter",
							managedCtClass.getName()
					),
					e
			);
		}
	}

	private void addLinkedPreviousHandling(CtClass managedCtClass) {
		final ConstPool constPool = managedCtClass.getClassFile().getConstPool();

		// add field to hold "previous" ManagedEntity
		final CtField previousField;
		try {
			previousField = new CtField( managedEntityCtClass, PREVIOUS_FIELD_NAME, managedCtClass );
			managedCtClass.addField( previousField );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add field for holding previous ManagedEntity",
							managedCtClass.getName()
					),
					e
			);
		}

		// make that new field transient and @Transient
		previousField.setModifiers( previousField.getModifiers() | Modifier.TRANSIENT );
		previousField.setModifiers( Modifier.setPrivate( previousField.getModifiers() ) );
		AnnotationsAttribute annotationsAttribute = getVisibleAnnotations( previousField.getFieldInfo() );
		annotationsAttribute.addAnnotation( new Annotation( Transient.class.getName(), constPool ) );

		// add the "getter"
		try {
			managedCtClass.addMethod( CtNewMethod.getter( PREVIOUS_GETTER_NAME, previousField ) );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add previous ManagedEntity getter",
							managedCtClass.getName()
					),
					e
			);
		}

		// add the "setter"
		try {
			managedCtClass.addMethod( CtNewMethod.setter( PREVIOUS_SETTER_NAME, previousField ) );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add previous ManagedEntity setter",
							managedCtClass.getName()
					),
					e
			);
		}
	}

	private void addLinkedNextHandling(CtClass managedCtClass) {
		final ConstPool constPool = managedCtClass.getClassFile().getConstPool();

		// add field to hold "next" ManagedEntity
		final CtField nextField;
		try {
			nextField = new CtField( managedEntityCtClass, NEXT_FIELD_NAME, managedCtClass );
			managedCtClass.addField( nextField );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add field for holding next ManagedEntity",
							managedCtClass.getName()
					),
					e
			);
		}

		// make that new field (1) private, (2) transient and (3) @Transient
		nextField.setModifiers( nextField.getModifiers() | Modifier.TRANSIENT );
		nextField.setModifiers( Modifier.setPrivate( nextField.getModifiers() ) );
		AnnotationsAttribute annotationsAttribute = getVisibleAnnotations( nextField.getFieldInfo() );
		annotationsAttribute.addAnnotation( new Annotation( Transient.class.getName(), constPool ) );

		// add the "getter"
		try {
			managedCtClass.addMethod( CtNewMethod.getter( NEXT_GETTER_NAME, nextField ) );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add next ManagedEntity getter",
							managedCtClass.getName()
					),
					e
			);
		}

		// add the "setter"
		try {
			managedCtClass.addMethod( CtNewMethod.setter( NEXT_SETTER_NAME, nextField ) );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add next ManagedEntity setter",
							managedCtClass.getName()
					),
					e
			);
		}
	}

	private AnnotationsAttribute getVisibleAnnotations(FieldInfo fieldInfo) {
		AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) fieldInfo.getAttribute( AnnotationsAttribute.visibleTag );
		if ( annotationsAttribute == null ) {
			annotationsAttribute = new AnnotationsAttribute( fieldInfo.getConstPool(), AnnotationsAttribute.visibleTag );
			fieldInfo.addAttribute( annotationsAttribute );
		}
		return annotationsAttribute;
	}

	private void enhancePersistentAttributes(CtClass managedCtClass) {
		final IdentityHashMap<CtField,FieldVirtualReadWritePair> fieldToMethodsXref = new IdentityHashMap<CtField, FieldVirtualReadWritePair>();

		for ( CtField ctField : managedCtClass.getFields() ) {
			if ( ! enhancementContext.isPersistentField( ctField ) ) {
				continue;
			}

			final FieldVirtualReadWritePair methodPair = addReadAndWriteMethod( managedCtClass, ctField );
			fieldToMethodsXref.put( ctField, methodPair );
		}

		transformFieldAccessesIntoReadsAndWrites( managedCtClass, fieldToMethodsXref );
	}

	private FieldVirtualReadWritePair addReadAndWriteMethod(CtClass managedCtClass, CtField persistentField) {
		// add the "reader"
		final CtMethod reader = generateFieldReader( managedCtClass, persistentField );

		// add the "writer"
		final CtMethod writer = generateFieldWriter( managedCtClass, persistentField );

		return new FieldVirtualReadWritePair( reader, writer );
	}

	private CtMethod generateFieldReader(CtClass managedCtClass, CtField persistentField) {
		// todo : temporary; still need to add hooks into lazy-loading
		try {
			final String name = PERSISTENT_FIELD_READER_PREFIX + persistentField.getName();
			CtMethod reader = CtNewMethod.getter( name, persistentField );
			managedCtClass.addMethod( reader );
			return reader;
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add virtual reader method for field [%s]",
							managedCtClass.getName(),
							persistentField.getName()
					),
					e
			);
		}
	}

	private CtMethod generateFieldWriter(CtClass managedCtClass, CtField persistentField) {
		// todo : temporary; still need to add hooks into lazy-loading and dirtying
		try {
			final String name = PERSISTENT_FIELD_WRITER_PREFIX + persistentField.getName();
			CtMethod writer = CtNewMethod.setter( name, persistentField );
			managedCtClass.addMethod( writer );
			return writer;
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add virtual writer method for field [%s]",
							managedCtClass.getName(),
							persistentField.getName()
					),
					e
			);
		}
	}

	private void transformFieldAccessesIntoReadsAndWrites(
			CtClass managedCtClass,
			IdentityHashMap<CtField, FieldVirtualReadWritePair> fieldToMethodsXref) {
	}

	private static class FieldVirtualReadWritePair {
		private final CtMethod readMethod;
		private final CtMethod writeMethod;

		private FieldVirtualReadWritePair(CtMethod readMethod, CtMethod writeMethod) {
			this.readMethod = readMethod;
			this.writeMethod = writeMethod;
		}
	}

}
