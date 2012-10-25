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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.StackMapTable;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.stackmap.MapMaker;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.EnhancementException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedComposite;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Steve Ebersole
 * @author Jason Greene
 */
public class Enhancer  {
	private static final CoreMessageLogger log = Logger.getMessageLogger( CoreMessageLogger.class, Enhancer.class.getName() );

	public static final String PERSISTENT_FIELD_READER_PREFIX = "$$_hibernate_read_";
	public static final String PERSISTENT_FIELD_WRITER_PREFIX = "$$_hibernate_write_";

	public static final String ENTITY_INSTANCE_GETTER_NAME = "$$_hibernate_getEntityInstance";

	public static final String ENTITY_ENTRY_FIELD_NAME = "$$_hibernate_entityEntryHolder";
	public static final String ENTITY_ENTRY_GETTER_NAME = "$$_hibernate_getEntityEntry";
	public static final String ENTITY_ENTRY_SETTER_NAME = "$$_hibernate_setEntityEntry";

	public static final String PREVIOUS_FIELD_NAME = "$$_hibernate_previousManagedEntity";
	public static final String PREVIOUS_GETTER_NAME = "$$_hibernate_getPreviousManagedEntity";
	public static final String PREVIOUS_SETTER_NAME = "$$_hibernate_setPreviousManagedEntity";

	public static final String NEXT_FIELD_NAME = "$$_hibernate_nextManagedEntity";
	public static final String NEXT_GETTER_NAME = "$$_hibernate_getNextManagedEntity";
	public static final String NEXT_SETTER_NAME = "$$_hibernate_setNextManagedEntity";

	public static final String INTERCEPTOR_FIELD_NAME = "$$_hibernate_attributeInterceptor";
	public static final String INTERCEPTOR_GETTER_NAME = "$$_hibernate_getInterceptor";
	public static final String INTERCEPTOR_SETTER_NAME = "$$_hibernate_setInterceptor";

	private final EnhancementContext enhancementContext;

	private final ClassPool classPool;
	private final CtClass managedEntityCtClass;
	private final CtClass managedCompositeCtClass;
	private final CtClass attributeInterceptorCtClass;
	private final CtClass attributeInterceptableCtClass;
	private final CtClass entityEntryCtClass;
	private final CtClass objectCtClass;

	public Enhancer(EnhancementContext enhancementContext) {
		this.enhancementContext = enhancementContext;
		this.classPool = buildClassPool( enhancementContext );

		try {
			// add ManagedEntity contract
			this.managedEntityCtClass = classPool.makeClass(
					ManagedEntity.class.getClassLoader().getResourceAsStream(
							ManagedEntity.class.getName().replace( '.', '/' ) + ".class"
					)
			);

			// add ManagedComposite contract
			this.managedCompositeCtClass = classPool.makeClass(
					ManagedComposite.class.getClassLoader().getResourceAsStream(
							ManagedComposite.class.getName().replace( '.', '/' ) + ".class"
					)
			);

			// add PersistentAttributeInterceptable contract
			this.attributeInterceptableCtClass = classPool.makeClass(
					PersistentAttributeInterceptable.class.getClassLoader().getResourceAsStream(
							PersistentAttributeInterceptable.class.getName().replace( '.', '/' ) + ".class"
					)
			);

			// add PersistentAttributeInterceptor contract
			this.attributeInterceptorCtClass = classPool.makeClass(
					PersistentAttributeInterceptor.class.getClassLoader().getResourceAsStream(
							PersistentAttributeInterceptor.class.getName().replace( '.', '/' ) + ".class"
					)
			);

			// "add" EntityEntry
			this.entityEntryCtClass = classPool.makeClass( EntityEntry.class.getName() );
		}
		catch (IOException e) {
			throw new EnhancementException( "Could not prepare Javassist ClassPool", e );
		}

		try {
			this.objectCtClass = classPool.getCtClass( Object.class.getName() );
		}
		catch (NotFoundException e) {
			throw new EnhancementException( "Could not prepare Javassist ClassPool", e );
		}
	}

	private ClassPool buildClassPool(EnhancementContext enhancementContext) {
		ClassPool classPool = new ClassPool( false );
		ClassLoader loadingClassLoader = enhancementContext.getLoadingClassLoader();
		if ( loadingClassLoader != null ) {
			classPool.appendClassPath( new LoaderClassPath( loadingClassLoader ) );
		}
		return classPool;
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
			if ( ManagedEntity.class.getName().equals( interfaceName )
					|| ManagedComposite.class.getName().equals( interfaceName ) ) {
				log.debug( "skipping enhancement : already enhanced" );
				return;
			}
		}

		if ( enhancementContext.isEntityClass( managedCtClass ) ) {
			enhanceAsEntity( managedCtClass );
		}
		else if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
			enhanceAsComposite( managedCtClass );
		}
		else {
			log.debug( "skipping enhancement : not entity or composite" );
		}
	}

	private void enhanceAsEntity(CtClass managedCtClass) {
		// add the ManagedEntity interface
		managedCtClass.addInterface( managedEntityCtClass );

		enhancePersistentAttributes( managedCtClass );

		addEntityInstanceHandling( managedCtClass );
		addEntityEntryHandling( managedCtClass );
		addLinkedPreviousHandling( managedCtClass );
		addLinkedNextHandling( managedCtClass );
	}

	private void enhanceAsComposite(CtClass managedCtClass) {
		enhancePersistentAttributes( managedCtClass );
	}

	private void addEntityInstanceHandling(CtClass managedCtClass) {
		// add the ManagedEntity#$$_hibernate_getEntityInstance method
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
	}

	private void addEntityEntryHandling(CtClass managedCtClass) {
		addFieldWithGetterAndSetter(
				managedCtClass,
				entityEntryCtClass,
				ENTITY_ENTRY_FIELD_NAME,
				ENTITY_ENTRY_GETTER_NAME,
				ENTITY_ENTRY_SETTER_NAME
		);
	}

	private void addLinkedPreviousHandling(CtClass managedCtClass) {
		addFieldWithGetterAndSetter(
				managedCtClass,
				managedEntityCtClass,
				PREVIOUS_FIELD_NAME,
				PREVIOUS_GETTER_NAME,
				PREVIOUS_SETTER_NAME
		);
	}

	private void addLinkedNextHandling(CtClass managedCtClass) {
		addFieldWithGetterAndSetter(
				managedCtClass,
				managedEntityCtClass,
				NEXT_FIELD_NAME,
				NEXT_GETTER_NAME,
				NEXT_SETTER_NAME
		);
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
		addInterceptorHandling( managedCtClass );
		if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
			addInLineDirtyHandling( managedCtClass );
		}

		final IdentityHashMap<String,PersistentAttributeDescriptor> attrDescriptorMap
				= new IdentityHashMap<String, PersistentAttributeDescriptor>();

		for ( CtField persistentField : collectPersistentFields( managedCtClass ) ) {
			attrDescriptorMap.put(
					persistentField.getName(),
					enhancePersistentAttribute( managedCtClass, persistentField )
			);
		}

		// lastly, find all references to the transformed fields and replace with calls to the added reader/writer
		transformFieldAccessesIntoReadsAndWrites( managedCtClass, attrDescriptorMap );
	}

	private PersistentAttributeDescriptor enhancePersistentAttribute(CtClass managedCtClass, CtField persistentField) {
		try {
			final AttributeTypeDescriptor typeDescriptor = resolveAttributeTypeDescriptor( persistentField );
			return new PersistentAttributeDescriptor(
					persistentField,
					generateFieldReader( managedCtClass, persistentField, typeDescriptor ),
					generateFieldWriter( managedCtClass, persistentField, typeDescriptor ),
					typeDescriptor
			);
		}
		catch (Exception e) {
			throw new EnhancementException(
					String.format(
							"Unable to enhance persistent attribute [%s:%s]",
							managedCtClass.getName(),
							persistentField.getName()
					),
					e
			);
		}
	}

	private CtField[] collectPersistentFields(CtClass managedCtClass) {
		// todo : drive this from the Hibernate metamodel instance...

		final List<CtField> persistentFieldList = new ArrayList<CtField>();
		for ( CtField ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields
			if ( Modifier.isStatic( ctField.getModifiers() ) ) {
				continue;
			}
			// skip fields added by enhancement
			if ( ctField.getName().startsWith( "$" ) ) {
				continue;
			}
			if ( enhancementContext.isPersistentField( ctField ) ) {
				persistentFieldList.add( ctField );
			}
		}

		return enhancementContext.order( persistentFieldList.toArray( new CtField[persistentFieldList.size()]) );
	}

	private void addInterceptorHandling(CtClass managedCtClass) {
		// interceptor handling is only needed if either:
		//		a) in-line dirty checking has *not* been requested
		//		b) class has lazy-loadable attributes
		if ( enhancementContext.doDirtyCheckingInline( managedCtClass )
				&& ! enhancementContext.hasLazyLoadableAttributes( managedCtClass ) ) {
			return;
		}

		log.debug( "Weaving in PersistentAttributeInterceptable implementation" );


		// add in the PersistentAttributeInterceptable contract
		managedCtClass.addInterface( attributeInterceptableCtClass );

		addFieldWithGetterAndSetter(
				managedCtClass,
				attributeInterceptorCtClass,
				INTERCEPTOR_FIELD_NAME,
				INTERCEPTOR_GETTER_NAME,
				INTERCEPTOR_SETTER_NAME
		);
	}

	private void addInLineDirtyHandling(CtClass managedCtClass) {
		// todo : implement
	}

	private void addFieldWithGetterAndSetter(
			CtClass targetClass,
			CtClass fieldType,
			String fieldName,
			String getterName,
			String setterName) {
		final CtField theField = addField( targetClass, fieldType, fieldName, true );
		addGetter( targetClass, theField, getterName );
		addSetter( targetClass, theField, setterName );
	}

	private CtField addField(CtClass targetClass, CtClass fieldType, String fieldName, boolean makeTransient) {
		final ConstPool constPool = targetClass.getClassFile().getConstPool();

		final CtField theField;
		try {
			theField = new CtField( fieldType, fieldName, targetClass );
			targetClass.addField( theField );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance class [%s] to add field [%s]",
							targetClass.getName(),
							fieldName
					),
					e
			);
		}

		// make that new field (1) private, (2) transient and (3) @Transient
		if ( makeTransient ) {
			theField.setModifiers( theField.getModifiers() | Modifier.TRANSIENT );
		}
		theField.setModifiers( Modifier.setPrivate( theField.getModifiers() ) );
		AnnotationsAttribute annotationsAttribute = getVisibleAnnotations( theField.getFieldInfo() );
		annotationsAttribute.addAnnotation( new Annotation( Transient.class.getName(), constPool ) );
		return theField;
	}

	private void addGetter(CtClass targetClass, CtField theField, String getterName) {
		try {
			targetClass.addMethod( CtNewMethod.getter( getterName, theField ) );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add getter method [%s]",
							targetClass.getName(),
							getterName
					),
					e
			);
		}
	}

	private void addSetter(CtClass targetClass, CtField theField, String setterName) {
		try {
			targetClass.addMethod( CtNewMethod.setter( setterName, theField ) );
		}
		catch (CannotCompileException e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add setter method [%s]",
							targetClass.getName(),
							setterName
					),
					e
			);
		}
	}

	private CtMethod generateFieldReader(
			CtClass managedCtClass,
			CtField persistentField,
			AttributeTypeDescriptor typeDescriptor)
			throws BadBytecode, CannotCompileException {

		final FieldInfo fieldInfo = persistentField.getFieldInfo();
		final String fieldName = fieldInfo.getName();
		final String readerName = PERSISTENT_FIELD_READER_PREFIX + fieldName;

		// read attempts only have to deal lazy-loading support, not dirty checking; so if the field
		// is not enabled as lazy-loadable return a plain simple getter as the reader
		if ( ! enhancementContext.isLazyLoadable( persistentField ) ) {
			// not lazy-loadable...
			// EARLY RETURN!!!
			try {
				CtMethod reader = CtNewMethod.getter( readerName, persistentField );
				managedCtClass.addMethod( reader );
				return reader;
			}
			catch (CannotCompileException e) {
				throw new EnhancementException(
						String.format(
								"Could not enhance entity class [%s] to add field reader method [%s]",
								managedCtClass.getName(),
								readerName
						),
						e
				);
			}
		}

		// temporary solution...
		String methodBody = typeDescriptor.buildReadInterceptionBodyFragment( fieldName )
				+ " return this." + fieldName + ";";

		try {
			CtMethod reader = CtNewMethod.make(
					Modifier.PRIVATE,
					persistentField.getType(),
					readerName,
					null,
					null,
					"{" + methodBody + "}",
					managedCtClass
			);
			managedCtClass.addMethod( reader );
			return reader;
		}
		catch (Exception e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add field reader method [%s]",
							managedCtClass.getName(),
							readerName
					),
					e
			);
		}
	}

	private CtMethod generateFieldWriter(
			CtClass managedCtClass,
			CtField persistentField,
			AttributeTypeDescriptor typeDescriptor) {

		final FieldInfo fieldInfo = persistentField.getFieldInfo();
		final String fieldName = fieldInfo.getName();
		final String writerName = PERSISTENT_FIELD_WRITER_PREFIX + fieldName;

		final CtMethod writer;

		try {
			if ( ! enhancementContext.isLazyLoadable( persistentField ) ) {
				// not lazy-loadable...
				writer = CtNewMethod.setter( writerName, persistentField );
			}
			else {
				String methodBody = typeDescriptor.buildWriteInterceptionBodyFragment( fieldName );
				writer = CtNewMethod.make(
						Modifier.PRIVATE,
						CtClass.voidType,
						writerName,
						new CtClass[] { persistentField.getType() },
						null,
						"{" + methodBody + "}",
						managedCtClass
				);
			}

			if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				writer.insertBefore( typeDescriptor.buildInLineDirtyCheckingBodyFragment( fieldName ) );
			}

			managedCtClass.addMethod( writer );
			return writer;
		}
		catch (Exception e) {
			throw new EnhancementException(
					String.format(
							"Could not enhance entity class [%s] to add field writer method [%s]",
							managedCtClass.getName(),
							writerName
					),
					e
			);
		}
	}

	private void transformFieldAccessesIntoReadsAndWrites(
			CtClass managedCtClass,
			IdentityHashMap<String, PersistentAttributeDescriptor> attributeDescriptorMap) {

		final ConstPool constPool = managedCtClass.getClassFile().getConstPool();

		for ( Object oMethod : managedCtClass.getClassFile().getMethods() ) {
			final MethodInfo methodInfo = (MethodInfo) oMethod;
			final String methodName = methodInfo.getName();

			// skip methods added by enhancement
			if ( methodName.startsWith( PERSISTENT_FIELD_READER_PREFIX )
					|| methodName.startsWith( PERSISTENT_FIELD_WRITER_PREFIX )
					|| methodName.equals( ENTITY_INSTANCE_GETTER_NAME )
					|| methodName.equals( ENTITY_ENTRY_GETTER_NAME )
					|| methodName.equals( ENTITY_ENTRY_SETTER_NAME )
					|| methodName.equals( PREVIOUS_GETTER_NAME )
					|| methodName.equals( PREVIOUS_SETTER_NAME )
					|| methodName.equals( NEXT_GETTER_NAME )
					|| methodName.equals( NEXT_SETTER_NAME ) ) {
				continue;
			}

			final CodeAttribute codeAttr = methodInfo.getCodeAttribute();
			if ( codeAttr == null ) {
				// would indicate an abstract method, continue to next method
				continue;
			}

			try {
				CodeIterator itr = codeAttr.iterator();
				while ( itr.hasNext() ) {
					int index = itr.next();
					int op = itr.byteAt( index );
					if ( op != Opcode.PUTFIELD && op != Opcode.GETFIELD ) {
						continue;
					}

					int constIndex = itr.u16bitAt( index+1 );

					final String fieldName = constPool.getFieldrefName( constIndex );
					final PersistentAttributeDescriptor attributeDescriptor = attributeDescriptorMap.get( fieldName );

					if ( attributeDescriptor == null ) {
						// its not a field we have enhanced for interception, so skip it
						continue;
					}

					log.tracef(
							"Transforming access to field [%s] from method [%s]",
							fieldName,
							methodName
					);

					if ( op == Opcode.GETFIELD ) {
						int read_method_index = constPool.addMethodrefInfo(
								constPool.getThisClassInfo(),
								attributeDescriptor.getReader().getName(),
								attributeDescriptor.getReader().getSignature()
						);
						itr.writeByte( Opcode.INVOKESPECIAL, index );
						itr.write16bit( read_method_index, index+1 );
					}
					else {
						int write_method_index = constPool.addMethodrefInfo(
								constPool.getThisClassInfo(),
								attributeDescriptor.getWriter().getName(),
								attributeDescriptor.getWriter().getSignature()
						);
						itr.writeByte( Opcode.INVOKESPECIAL, index );
						itr.write16bit( write_method_index, index+1 );
					}
				}

				StackMapTable smt = MapMaker.make( classPool, methodInfo );
				methodInfo.getCodeAttribute().setAttribute(smt);
			}
			catch (BadBytecode e) {
				throw new EnhancementException(
						"Unable to perform field access transformation in method : " + methodName,
						e
				);
			}
		}
	}

	private static class PersistentAttributeDescriptor {
		private final CtField field;
		private final CtMethod reader;
		private final CtMethod writer;
		private final AttributeTypeDescriptor typeDescriptor;

		private PersistentAttributeDescriptor(
				CtField field,
				CtMethod reader,
				CtMethod writer,
				AttributeTypeDescriptor typeDescriptor) {
			this.field = field;
			this.reader = reader;
			this.writer = writer;
			this.typeDescriptor = typeDescriptor;
		}

		public CtField getField() {
			return field;
		}

		public CtMethod getReader() {
			return reader;
		}

		public CtMethod getWriter() {
			return writer;
		}

		public AttributeTypeDescriptor getTypeDescriptor() {
			return typeDescriptor;
		}
	}

	private static interface AttributeTypeDescriptor {
		public String buildReadInterceptionBodyFragment(String fieldName);
		public String buildWriteInterceptionBodyFragment(String fieldName);
		public String buildInLineDirtyCheckingBodyFragment(String fieldName);
	}

	private AttributeTypeDescriptor resolveAttributeTypeDescriptor(CtField persistentField) throws NotFoundException {
		// for now cheat... we know we only have Object fields
		if ( persistentField.getType() == CtClass.booleanType ) {
			return BOOLEAN_DESCRIPTOR;
		}
		else if ( persistentField.getType() == CtClass.byteType ) {
			return BYTE_DESCRIPTOR;
		}
		else if ( persistentField.getType() == CtClass.charType ) {
			return CHAR_DESCRIPTOR;
		}
		else if ( persistentField.getType() == CtClass.shortType ) {
			return SHORT_DESCRIPTOR;
		}
		else if ( persistentField.getType() == CtClass.intType ) {
			return INT_DESCRIPTOR;
		}
		else if ( persistentField.getType() == CtClass.longType ) {
			return LONG_DESCRIPTOR;
		}
		else if ( persistentField.getType() == CtClass.doubleType ) {
			return DOUBLE_DESCRIPTOR;
		}
		else if ( persistentField.getType() == CtClass.floatType ) {
			return FLOAT_DESCRIPTOR;
		}
		else {
			return new ObjectAttributeTypeDescriptor( persistentField.getType() );
		}
	}

	private static abstract class AbstractAttributeTypeDescriptor implements AttributeTypeDescriptor {
		@Override
		public String buildInLineDirtyCheckingBodyFragment(String fieldName) {
			// for now...
			// todo : hook-in in-lined dirty checking
			return String.format(
					"System.out.println( \"DIRTY CHECK (%1$s) : \" + this.%1$s + \" -> \" + $1 + \" (dirty=\" + (this.%1$s != $1) +\")\" );",
					fieldName
			);
		}
	}

	private static class ObjectAttributeTypeDescriptor extends AbstractAttributeTypeDescriptor {
		private final CtClass concreteType;

		private ObjectAttributeTypeDescriptor(CtClass concreteType) {
			this.concreteType = concreteType;
		}

		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format(
					"if ( $$_hibernate_getInterceptor() != null ) { " +
							"this.%1$s = (%2$s) $$_hibernate_getInterceptor().readObject(this, \"%1$s\", this.%1$s); " +
							"}",
					fieldName,
					concreteType.getName()
			);
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format(
					"%2$s localVar = $1;" +
							"if ( $$_hibernate_getInterceptor() != null ) {" +
							"localVar = (%2$s) $$_hibernate_getInterceptor().writeObject(this, \"%1$s\", this.%1$s, $1);" +
							"}" +
							"this.%1$s = localVar;",
					fieldName,
					concreteType.getName()
			);
		}
	}

	private static final AttributeTypeDescriptor BOOLEAN_DESCRIPTOR = new AbstractAttributeTypeDescriptor() {
		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format(
					"if ( $$_hibernate_getInterceptor() != null ) { " +
							"this.%1$s = $$_hibernate_getInterceptor().readBoolean(this, \"%1$s\", this.%1$s); " +
							"}",
					fieldName
			);
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format(
					"boolean localVar = $1;" +
							"if ( $$_hibernate_getInterceptor() != null ) {" +
							"localVar = $$_hibernate_getInterceptor().writeBoolean(this, \"%1$s\", this.%1$s, $1);" +
							"}" +
							"this.%1$s = localVar;",
					fieldName
			);
		}
	};

	private static final AttributeTypeDescriptor BYTE_DESCRIPTOR = new AbstractAttributeTypeDescriptor() {
		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format(
					"if ( $$_hibernate_getInterceptor() != null ) { " +
							"this.%1$s = $$_hibernate_getInterceptor().readByte(this, \"%1$s\", this.%1$s); " +
							"}",
					fieldName
			);
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format(
					"byte localVar = $1;" +
							"if ( $$_hibernate_getInterceptor() != null ) {" +
							"localVar = $$_hibernate_getInterceptor().writeByte(this, \"%1$s\", this.%1$s, $1);" +
							"}" +
							"this.%1$s = localVar;",
					fieldName
			);
		}
	};

	private static final AttributeTypeDescriptor CHAR_DESCRIPTOR = new AbstractAttributeTypeDescriptor() {
		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format(
					"if ( $$_hibernate_getInterceptor() != null ) { " +
						"this.%1$s = $$_hibernate_getInterceptor().readChar(this, \"%1$s\", this.%1$s); " +
					"}",
					fieldName
			);
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format(
					"char localVar = $1;" +
					"if ( $$_hibernate_getInterceptor() != null ) {" +
						"localVar = $$_hibernate_getInterceptor().writeChar(this, \"%1$s\", this.%1$s, $1);" +
					"}" +
					"this.%1$s = localVar;",
					fieldName
			);
		}
	};

	private static final AttributeTypeDescriptor SHORT_DESCRIPTOR = new AbstractAttributeTypeDescriptor() {
		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format(
					"if ( $$_hibernate_getInterceptor() != null ) { " +
						"this.%1$s = $$_hibernate_getInterceptor().readShort(this, \"%1$s\", this.%1$s); " +
					"}",
					fieldName
			);
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format(
					"short localVar = $1;" +
					"if ( $$_hibernate_getInterceptor() != null ) {" +
						"localVar = $$_hibernate_getInterceptor().writeShort(this, \"%1$s\", this.%1$s, $1);" +
					"}" +
					"this.%1$s = localVar;",
					fieldName
			);
		}
	};

	private static final AttributeTypeDescriptor INT_DESCRIPTOR = new AbstractAttributeTypeDescriptor() {
		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format(
					"if ( $$_hibernate_getInterceptor() != null ) { " +
						"this.%1$s = $$_hibernate_getInterceptor().readInt(this, \"%1$s\", this.%1$s); " +
					"}",
					fieldName
			);
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format(
					"int localVar = $1;" +
					"if ( $$_hibernate_getInterceptor() != null ) {" +
						"localVar = $$_hibernate_getInterceptor().writeInt(this, \"%1$s\", this.%1$s, $1);" +
					"}" +
					"this.%1$s = localVar;",
					fieldName
			);
		}
	};

	private static final AttributeTypeDescriptor LONG_DESCRIPTOR = new AbstractAttributeTypeDescriptor() {
		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format(
					"if ( $$_hibernate_getInterceptor() != null ) { " +
						"this.%1$s = $$_hibernate_getInterceptor().readLong(this, \"%1$s\", this.%1$s); " +
					"}",
					fieldName
			);
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format(
					"long localVar = $1;" +
					"if ( $$_hibernate_getInterceptor() != null ) {" +
						"localVar = $$_hibernate_getInterceptor().writeLong(this, \"%1$s\", this.%1$s, $1);" +
					"}" +
					"this.%1$s = localVar;",
					fieldName
			);
		}
	};

	private static final AttributeTypeDescriptor DOUBLE_DESCRIPTOR = new AbstractAttributeTypeDescriptor() {
		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format(
					"if ( $$_hibernate_getInterceptor() != null ) { " +
						"this.%1$s = $$_hibernate_getInterceptor().readDouble(this, \"%1$s\", this.%1$s); " +
					"}",
					fieldName
			);
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format(
					"double localVar = $1;" +
					"if ( $$_hibernate_getInterceptor() != null ) {" +
						"localVar = $$_hibernate_getInterceptor().writeDouble(this, \"%1$s\", this.%1$s, $1);" +
					"}" +
					"this.%1$s = localVar;",
					fieldName
			);
		}
	};

	private static final AttributeTypeDescriptor FLOAT_DESCRIPTOR = new AbstractAttributeTypeDescriptor() {
		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format(
					"if ( $$_hibernate_getInterceptor() != null ) { " +
						"this.%1$s = $$_hibernate_getInterceptor().readFloat(this, \"%1$s\", this.%1$s); " +
					"}",
					fieldName
			);
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format(
					"float localVar = $1;" +
					"if ( $$_hibernate_getInterceptor() != null ) {" +
						"localVar = $$_hibernate_getInterceptor().writeFloat(this, \"%1$s\", this.%1$s, $1);" +
					"}" +
					"this.%1$s = localVar;",
					fieldName
			);
		}
	};
}
