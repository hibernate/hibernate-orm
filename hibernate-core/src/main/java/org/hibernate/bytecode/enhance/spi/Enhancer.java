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

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.annotation.Annotation;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.EnhancementException;
import org.hibernate.bytecode.internal.javassist.FieldHandled;
import org.hibernate.engine.ManagedComposite;
import org.hibernate.engine.ManagedEntity;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.internal.CoreMessageLogger;

/**
 * @author Steve Ebersole
 * @author Jason Greene
 */
public class Enhancer  {
	private static final CoreMessageLogger log = Logger.getMessageLogger( CoreMessageLogger.class, Enhancer.class.getName() );

	public static final String ENTITY_ENTRY_FIELD_NAME = "$hibernate_entityEntryHolder";
	public static final String ENTITY_ENTRY_GETTER_NAME = "hibernate_getEntityEntry";
	public static final String ENTITY_ENTRY_SETTER_NAME = "hibernate_setEntityEntry";

	private final EnhancementContext enhancementContext;

	private final ClassPool classPool;
	private final CtClass managedEntityCtClass;
	private final CtClass managedCompositeCtClass;
	private final CtClass entityEntryCtClass;

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
			if ( FieldHandled.class.getName().equals( interfaceName ) ) {
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

		// make that new field @Transient
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

	private AnnotationsAttribute getVisibleAnnotations(FieldInfo fieldInfo) {
		AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) fieldInfo.getAttribute( AnnotationsAttribute.visibleTag );
		if ( annotationsAttribute == null ) {
			annotationsAttribute = new AnnotationsAttribute( fieldInfo.getConstPool(), AnnotationsAttribute.visibleTag );
			fieldInfo.addAttribute( annotationsAttribute );
		}
		return annotationsAttribute;
	}

	private void enhanceAsComposite(CtClass classFile) {
	}


}
