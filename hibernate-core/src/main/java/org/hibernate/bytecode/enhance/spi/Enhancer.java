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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

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
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.StackMapTable;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.stackmap.MapMaker;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.EnhancementException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedComposite;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Class responsible for performing enhancement.
 *
 * @author Steve Ebersole
 * @author Jason Greene
 */
public class Enhancer {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( Enhancer.class );

	private final EnhancementContext enhancementContext;

	private final ClassPool classPool;
	private final CtClass managedEntityCtClass;
	private final CtClass managedCompositeCtClass;
	private final CtClass attributeInterceptorCtClass;
	private final CtClass attributeInterceptableCtClass;
	private final CtClass entityEntryCtClass;
	private final CtClass objectCtClass;
	private boolean isComposite;

	/**
	 * Constructs the Enhancer, using the given context.
	 *
	 * @param enhancementContext Describes the context in which enhancement will occur so as to give access
	 * to contextual/environmental information.
	 */
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
		final ClassPool classPool = new ClassPool( false );
		final ClassLoader loadingClassLoader = enhancementContext.getLoadingClassLoader();
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
	 *         already enhanced or we could not enhance it for some reason.
	 *
	 * @throws EnhancementException Indicates a problem performing the enhancement
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

		enhance( managedCtClass, false );

		return getByteCode( managedCtClass );
	}

	public byte[] enhanceComposite(String className, byte[] originalBytes) throws EnhancementException {
		final CtClass managedCtClass;
		try {
			managedCtClass = classPool.makeClassIfNew( new ByteArrayInputStream( originalBytes ) );
		}
		catch (IOException e) {
			log.unableToBuildEnhancementMetamodel( className );
			return originalBytes;
		}

		enhance( managedCtClass, true );

		return getByteCode( managedCtClass );
	}

	private byte[] getByteCode(CtClass managedCtClass) {
		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		final DataOutputStream out;
		try {
			out = new DataOutputStream( byteStream );
			try {
				managedCtClass.toBytecode( out );
				return byteStream.toByteArray();
			}
			finally {
				try {
					out.close();
				}
				catch (IOException e) {
					//swallow
				}
			}
		}
		catch (Exception e) {
			log.unableToTransformClass( e.getMessage() );
			throw new HibernateException( "Unable to transform class: " + e.getMessage() );
		}
	}

	private void enhance(CtClass managedCtClass, boolean isComposite) {
		this.isComposite = isComposite;
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

		if ( !isComposite && enhancementContext.isEntityClass( managedCtClass ) ) {
			enhanceAsEntity( managedCtClass );
		}
		else if ( isComposite || enhancementContext.isCompositeClass( managedCtClass ) ) {
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
							EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME,
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
				EnhancerConstants.ENTITY_ENTRY_FIELD_NAME,
				EnhancerConstants.ENTITY_ENTRY_GETTER_NAME,
				EnhancerConstants.ENTITY_ENTRY_SETTER_NAME
		);
	}

	private void addLinkedPreviousHandling(CtClass managedCtClass) {
		addFieldWithGetterAndSetter(
				managedCtClass,
				managedEntityCtClass,
				EnhancerConstants.PREVIOUS_FIELD_NAME,
				EnhancerConstants.PREVIOUS_GETTER_NAME,
				EnhancerConstants.PREVIOUS_SETTER_NAME
		);
	}

	private void addLinkedNextHandling(CtClass managedCtClass) {
		addFieldWithGetterAndSetter(
				managedCtClass,
				managedEntityCtClass,
				EnhancerConstants.NEXT_FIELD_NAME,
				EnhancerConstants.NEXT_GETTER_NAME,
				EnhancerConstants.NEXT_SETTER_NAME
		);
	}

	private AnnotationsAttribute getVisibleAnnotations(FieldInfo fieldInfo) {
		AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) fieldInfo.getAttribute( AnnotationsAttribute.visibleTag );
		if ( annotationsAttribute == null ) {
			annotationsAttribute = new AnnotationsAttribute(
					fieldInfo.getConstPool(),
					AnnotationsAttribute.visibleTag
			);
			fieldInfo.addAttribute( annotationsAttribute );
		}
		return annotationsAttribute;
	}

	private void enhancePersistentAttributes(CtClass managedCtClass) {
		addInterceptorHandling( managedCtClass );
		if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
			addInLineDirtyHandling( managedCtClass );
		}

		final IdentityHashMap<String, PersistentAttributeDescriptor> attrDescriptorMap
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

		return enhancementContext.order( persistentFieldList.toArray( new CtField[persistentFieldList.size()] ) );
	}

	private List<CtField> collectCollectionFields(CtClass managedCtClass) {

		final List<CtField> collectionList = new ArrayList<CtField>();
		try {
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
					for ( CtClass ctClass : ctField.getType().getInterfaces() ) {
						if ( ctClass.getName().equals( "java.util.Collection" ) ) {
							collectionList.add( ctField );
							break;
						}
					}
				}
			}
		}
		catch (NotFoundException ignored) {
		}

		return collectionList;
	}

	private void addInterceptorHandling(CtClass managedCtClass) {
		// interceptor handling is only needed if either:
		//		a) in-line dirty checking has *not* been requested
		//		b) class has lazy-loadable attributes
		if ( enhancementContext.doDirtyCheckingInline( managedCtClass )
				&& !enhancementContext.hasLazyLoadableAttributes( managedCtClass ) ) {
			return;
		}

		log.debug( "Weaving in PersistentAttributeInterceptable implementation" );


		// add in the PersistentAttributeInterceptable contract
		managedCtClass.addInterface( attributeInterceptableCtClass );

		addFieldWithGetterAndSetter(
				managedCtClass,
				attributeInterceptorCtClass,
				EnhancerConstants.INTERCEPTOR_FIELD_NAME,
				EnhancerConstants.INTERCEPTOR_GETTER_NAME,
				EnhancerConstants.INTERCEPTOR_SETTER_NAME
		);
	}

	private boolean isClassAlreadyTrackingDirtyStatus(CtClass managedCtClass) {
		try {
			for ( CtClass ctInterface : managedCtClass.getInterfaces() ) {
				if ( ctInterface.getName().equals( SelfDirtinessTracker.class.getName() ) ) {
					return true;
				}
			}
		}
		catch (NotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void addInLineDirtyHandling(CtClass managedCtClass) {
		try {

			//create composite methods
			if ( isComposite ) {
				managedCtClass.addInterface( classPool.get( "org.hibernate.engine.spi.CompositeTracker" ) );
				CtClass compositeCtType = classPool.get( "org.hibernate.bytecode.enhance.spi.CompositeOwnerTracker" );
				addField( managedCtClass, compositeCtType, EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME, true );
				createCompositeTrackerMethod( managedCtClass );
			}
			// "normal" entity
			else {
				managedCtClass.addInterface( classPool.get( "org.hibernate.engine.spi.SelfDirtinessTracker" ) );
				CtClass trackerCtType = classPool.get( "java.util.Set" );
				addField( managedCtClass, trackerCtType, EnhancerConstants.TRACKER_FIELD_NAME, true );

				CtClass collectionTrackerCtType = classPool.get( "org.hibernate.bytecode.enhance.spi.CollectionTracker" );
				addField( managedCtClass, collectionTrackerCtType, EnhancerConstants.TRACKER_COLLECTION_NAME, true );

				createDirtyTrackerMethods( managedCtClass );
			}


		}
		catch (NotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create all dirty tracker methods
	 */
	private void createDirtyTrackerMethods(CtClass managedCtClass) {
		try {
			String trackerChangeMethod =
					"public void " + EnhancerConstants.TRACKER_CHANGER_NAME + "(String name) {" +
							"  if(" + EnhancerConstants.TRACKER_FIELD_NAME + " == null) {" +
							"    " + EnhancerConstants.TRACKER_FIELD_NAME + " = new java.util.HashSet();" +
							"  }" +
							"  if(!" + EnhancerConstants.TRACKER_FIELD_NAME + ".contains(name)) {" +
							"    " + EnhancerConstants.TRACKER_FIELD_NAME + ".add(name);" +
							"  }" +
							"}";
			managedCtClass.addMethod( CtNewMethod.make( trackerChangeMethod, managedCtClass ) );

			createCollectionDirtyCheckMethod( managedCtClass );
			createCollectionDirtyCheckGetFieldsMethod( managedCtClass );
			//createCompositeFieldsDirtyCheckMethod(managedCtClass);
			//createGetCompositeDirtyFieldsMethod(managedCtClass);

			createHasDirtyAttributesMethod( managedCtClass );

			createClearDirtyCollectionMethod( managedCtClass );
			createClearDirtyMethod( managedCtClass );

			String trackerGetMethod =
					"public java.util.List " + EnhancerConstants.TRACKER_GET_NAME + "() { " +
							"if(" + EnhancerConstants.TRACKER_FIELD_NAME + " == null) " +
							EnhancerConstants.TRACKER_FIELD_NAME + " = new java.util.HashSet();" +
							EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME + "(" +
							EnhancerConstants.TRACKER_FIELD_NAME + ");" +
							"return " + EnhancerConstants.TRACKER_FIELD_NAME + "; }";
			CtMethod getMethod = CtNewMethod.make( trackerGetMethod, managedCtClass );

			MethodInfo methodInfo = getMethod.getMethodInfo();
			SignatureAttribute signatureAttribute =
					new SignatureAttribute( methodInfo.getConstPool(), "()Ljava/util/Set<Ljava/lang/String;>;" );
			methodInfo.addAttribute( signatureAttribute );
			managedCtClass.addMethod( getMethod );

		}
		catch (CannotCompileException e) {
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void createTrackChangeCompositeMethod(CtClass managedCtClass) {
		StringBuilder builder = new StringBuilder();
		builder.append( "public void " )
				.append( EnhancerConstants.TRACKER_CHANGER_NAME )
				.append( "(String name) {" )
				.append( "if (" )
				.append( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME )
				.append( " != null) " )
				.append( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME )
				.append( ".callOwner(\".\"+name); }" );

		System.out.println( "COMPOSITE METHOD: " + builder.toString() );

		try {
			managedCtClass.addMethod( CtNewMethod.make( builder.toString(), managedCtClass ) );
		}
		catch (CannotCompileException e) {
			// swallow
		}
	}

	private void createCompositeTrackerMethod(CtClass managedCtClass) {
		try {
			StringBuilder builder = new StringBuilder();
			builder.append( "public void " )
					.append( EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER )
					.append( "(String name, org.hibernate.engine.spi.CompositeOwner tracker) {" )
					.append( "if(" )
					.append( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME )
					.append( " == null) " )
					.append( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME )
					.append( " = new org.hibernate.bytecode.enhance.spi.CompositeOwnerTracker();" )
					.append( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME )
					.append( ".add(name, tracker); }" );

			managedCtClass.addMethod( CtNewMethod.make( builder.toString(), managedCtClass ) );

			builder = new StringBuilder();
			builder.append( "public void " )
					.append( EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER )
					.append( "(String name) {" )
					.append( " if(" )
					.append( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME )
					.append( " != null)" )
					.append( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME )
					.append( ".removeOwner(name);}" );

			managedCtClass.addMethod( CtNewMethod.make( builder.toString(), managedCtClass ) );
		}
		catch (CannotCompileException e) {
			e.printStackTrace();
		}
	}

	private void createHasDirtyAttributesMethod(CtClass managedCtClass) throws CannotCompileException {
		String trackerHasChangedMethod =
				"public boolean " + EnhancerConstants.TRACKER_HAS_CHANGED_NAME + "() { return (" +
						EnhancerConstants.TRACKER_FIELD_NAME + " != null && !" +
						EnhancerConstants.TRACKER_FIELD_NAME + ".isEmpty()) || " +
						EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME + "(); } ";

		managedCtClass.addMethod( CtNewMethod.make( trackerHasChangedMethod, managedCtClass ) );
	}

	/**
	 * Creates _clearDirtyAttributes
	 */
	private void createClearDirtyMethod(CtClass managedCtClass) throws CannotCompileException, ClassNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append( "public void " )
				.append( EnhancerConstants.TRACKER_CLEAR_NAME )
				.append( "() {" )
				.append( "if (" )
				.append( EnhancerConstants.TRACKER_FIELD_NAME )
				.append( " != null) " )
				.append( EnhancerConstants.TRACKER_FIELD_NAME )
				.append( ".clear(); " )
				.append( EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME )
				.append( "(); }" );

		managedCtClass.addMethod( CtNewMethod.make( builder.toString(), managedCtClass ) );
	}

	private void createClearDirtyCollectionMethod(CtClass managedCtClass) throws CannotCompileException {
		StringBuilder builder = new StringBuilder();
		builder.append( "private void " )
				.append( EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME )
				.append( "() { if(" )
				.append( EnhancerConstants.TRACKER_COLLECTION_NAME )
				.append( " == null)" )
				.append( EnhancerConstants.TRACKER_COLLECTION_NAME )
				.append( " = new org.hibernate.bytecode.enhance.spi.CollectionTracker();" );

		for ( CtField ctField : collectCollectionFields( managedCtClass ) ) {
			if ( !enhancementContext.isMappedCollection( ctField ) ) {
				builder.append( "if(" )
						.append( ctField.getName() )
						.append( " != null) " )
						.append( EnhancerConstants.TRACKER_COLLECTION_NAME )
						.append( ".add(\"" )
						.append( ctField.getName() )
						.append( "\", " )
						.append( ctField.getName() )
						.append( ".size());" );
			}
		}

		builder.append( "}" );

		managedCtClass.addMethod( CtNewMethod.make( builder.toString(), managedCtClass ) );
	}

	/**
	 * create _areCollectionFieldsDirty
	 */
	private void createCollectionDirtyCheckMethod(CtClass managedCtClass) throws CannotCompileException {
		StringBuilder builder = new StringBuilder( "private boolean " )
				.append( EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME )
				.append( "() { if ($$_hibernate_getInterceptor() == null || " )
				.append( EnhancerConstants.TRACKER_COLLECTION_NAME )
				.append( " == null) return false; " );

		for ( CtField ctField : collectCollectionFields( managedCtClass ) ) {
			if ( !enhancementContext.isMappedCollection( ctField ) ) {
				builder.append( "if(" )
						.append( EnhancerConstants.TRACKER_COLLECTION_NAME )
						.append( ".getSize(\"" )
						.append( ctField.getName() )
						.append( "\") != " )
						.append( ctField.getName() )
						.append( ".size()) return true;" );
			}
		}

		builder.append( "return false; }" );

		managedCtClass.addMethod( CtNewMethod.make( builder.toString(), managedCtClass ) );
	}

	/**
	 * create _getCollectionFieldDirtyNames
	 */
	private void createCollectionDirtyCheckGetFieldsMethod(CtClass managedCtClass) throws CannotCompileException {
		StringBuilder collectionFieldDirtyFieldMethod = new StringBuilder( "private void " )
				.append( EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME )
				.append( "(java.util.Set trackerSet) { if(" )
				.append( EnhancerConstants.TRACKER_COLLECTION_NAME )
				.append( " == null) return; else {" );

		for ( CtField ctField : collectCollectionFields( managedCtClass ) ) {
			if ( !ctField.getName().startsWith( "$$_hibernate" )
					&& !enhancementContext.isMappedCollection( ctField ) ) {
				collectionFieldDirtyFieldMethod
						.append( "if(" )
						.append( EnhancerConstants.TRACKER_COLLECTION_NAME )
						.append( ".getSize(\"" )
						.append( ctField.getName() )
						.append( "\") != " )
						.append( ctField.getName() )
						.append( ".size()) trackerSet.add(\"" )
						.append( ctField.getName() )
						.append( "\");" );
			}
		}

		collectionFieldDirtyFieldMethod.append( "}}" );

		managedCtClass.addMethod( CtNewMethod.make( collectionFieldDirtyFieldMethod.toString(), managedCtClass ) );
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

		final AnnotationsAttribute annotationsAttribute = getVisibleAnnotations( theField.getFieldInfo() );
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
		final String readerName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + fieldName;

		// read attempts only have to deal lazy-loading support, not dirty checking; so if the field
		// is not enabled as lazy-loadable return a plain simple getter as the reader
		if ( !enhancementContext.isLazyLoadable( persistentField ) ) {
			// not lazy-loadable...
			// EARLY RETURN!!!
			try {
				final CtMethod reader = CtNewMethod.getter( readerName, persistentField );
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
		final String methodBody = typeDescriptor.buildReadInterceptionBodyFragment( fieldName )
				+ " return this." + fieldName + ";";

		try {
			final CtMethod reader = CtNewMethod.make(
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
		final String writerName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + fieldName;

		final CtMethod writer;

		try {
			if ( !enhancementContext.isLazyLoadable( persistentField ) ) {
				// not lazy-loadable...
				writer = CtNewMethod.setter( writerName, persistentField );
			}
			else {
				final String methodBody = typeDescriptor.buildWriteInterceptionBodyFragment( fieldName );
				writer = CtNewMethod.make(
						Modifier.PRIVATE,
						CtClass.voidType,
						writerName,
						new CtClass[] {persistentField.getType()},
						null,
						"{" + methodBody + "}",
						managedCtClass
				);
			}

			if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) && !isComposite ) {
				writer.insertBefore( typeDescriptor.buildInLineDirtyCheckingBodyFragment( persistentField ) );
			}

			if ( isComposite ) {
				StringBuilder builder = new StringBuilder();
				builder.append( " if(  " )
						.append( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME )
						.append( " != null) " )
						.append( EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME )
						.append( ".callOwner(\"." )
						.append( persistentField.getName() )
						.append( "\");" );

				writer.insertBefore( builder.toString() );
			}

			//composite types
			if ( persistentField.getAnnotation( Embedded.class ) != null ) {
				//make sure to add the CompositeOwner interface
				if ( !doClassInheritCompositeOwner( managedCtClass ) ) {
					managedCtClass.addInterface( classPool.get( "org.hibernate.engine.spi.CompositeOwner" ) );
				}
				//if a composite have a embedded field we need to implement the method as well
				if ( isComposite ) {
					createTrackChangeCompositeMethod( managedCtClass );
				}


				writer.insertBefore( cleanupPreviousOwner( persistentField ) );

				writer.insertAfter( compositeMethodBody( persistentField ) );
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

	private boolean doClassInheritCompositeOwner(CtClass managedCtClass) {
		try {
			for ( CtClass ctClass : managedCtClass.getInterfaces() ) {
				if ( ctClass.getName().equals( "org.hibernate.engine.spi.CompositeOwner" ) ) {
					return true;
				}
			}

			return false;
		}
		catch (NotFoundException e) {
			return false;
		}
	}

	private String cleanupPreviousOwner(CtField currentValue) {
		StringBuilder builder = new StringBuilder();
		builder.append( "if (" )
				.append( currentValue.getName() )
				.append( " != null) " )
				.append( "((org.hibernate.engine.spi.CompositeTracker)" )
				.append( currentValue.getName() )
				.append( ")." )
				.append( EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER )
				.append( "(\"" )
				.append( currentValue.getName() )
				.append( "\");" );

		return builder.toString();
	}

	private String compositeMethodBody(CtField currentValue) {
		StringBuilder builder = new StringBuilder();
		builder.append( "((org.hibernate.engine.spi.CompositeTracker) " )
				.append( currentValue.getName() )
				.append( ").$$_hibernate_setOwner(\"" )
				.append( currentValue.getName() )
				.append( "\",(org.hibernate.engine.spi.CompositeOwner) this);" )
				.append( EnhancerConstants.TRACKER_CHANGER_NAME + "(\"" ).append( currentValue.getName() ).append(
				"\");"
		);

		return builder.toString();
	}

	private void transformFieldAccessesIntoReadsAndWrites(
			CtClass managedCtClass,
			IdentityHashMap<String, PersistentAttributeDescriptor> attributeDescriptorMap) {

		final ConstPool constPool = managedCtClass.getClassFile().getConstPool();

		for ( Object oMethod : managedCtClass.getClassFile().getMethods() ) {
			final MethodInfo methodInfo = (MethodInfo) oMethod;
			final String methodName = methodInfo.getName();

			// skip methods added by enhancement
			if ( methodName.startsWith( EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX )
					|| methodName.startsWith( EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX )
					|| methodName.equals( EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME )
					|| methodName.equals( EnhancerConstants.ENTITY_ENTRY_GETTER_NAME )
					|| methodName.equals( EnhancerConstants.ENTITY_ENTRY_SETTER_NAME )
					|| methodName.equals( EnhancerConstants.PREVIOUS_GETTER_NAME )
					|| methodName.equals( EnhancerConstants.PREVIOUS_SETTER_NAME )
					|| methodName.equals( EnhancerConstants.NEXT_GETTER_NAME )
					|| methodName.equals( EnhancerConstants.NEXT_SETTER_NAME ) ) {
				continue;
			}

			final CodeAttribute codeAttr = methodInfo.getCodeAttribute();
			if ( codeAttr == null ) {
				// would indicate an abstract method, continue to next method
				continue;
			}

			try {
				final CodeIterator itr = codeAttr.iterator();
				while ( itr.hasNext() ) {
					final int index = itr.next();
					final int op = itr.byteAt( index );
					if ( op != Opcode.PUTFIELD && op != Opcode.GETFIELD ) {
						continue;
					}

					final int constIndex = itr.u16bitAt( index + 1 );

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
						final int readMethodIndex = constPool.addMethodrefInfo(
								constPool.getThisClassInfo(),
								attributeDescriptor.getReader().getName(),
								attributeDescriptor.getReader().getSignature()
						);
						itr.writeByte( Opcode.INVOKESPECIAL, index );
						itr.write16bit( readMethodIndex, index + 1 );
					}
					else {
						final int writeMethodIndex = constPool.addMethodrefInfo(
								constPool.getThisClassInfo(),
								attributeDescriptor.getWriter().getName(),
								attributeDescriptor.getWriter().getSignature()
						);
						itr.writeByte( Opcode.INVOKESPECIAL, index );
						itr.write16bit( writeMethodIndex, index + 1 );
					}
				}

				final StackMapTable smt = MapMaker.make( classPool, methodInfo );
				methodInfo.getCodeAttribute().setAttribute( smt );
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

		@SuppressWarnings("UnusedDeclaration")
		public AttributeTypeDescriptor getTypeDescriptor() {
			return typeDescriptor;
		}
	}

	private static interface AttributeTypeDescriptor {
		public String buildReadInterceptionBodyFragment(String fieldName);

		public String buildWriteInterceptionBodyFragment(String fieldName);

		public String buildInLineDirtyCheckingBodyFragment(CtField currentField);
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

	private abstract static class AbstractAttributeTypeDescriptor implements AttributeTypeDescriptor {
		@Override
		public String buildInLineDirtyCheckingBodyFragment(CtField currentValue) {
			StringBuilder builder = new StringBuilder();
			try {
				//should ignore primary keys
				for ( Object o : currentValue.getType().getAnnotations() ) {
					if ( o instanceof Id ) {
						return "";
					}
				}

				builder.append( entityMethodBody( currentValue ) );


			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			catch (NotFoundException e) {
				e.printStackTrace();
			}
			return builder.toString();
		}

		private String entityMethodBody(CtField currentValue) {
			StringBuilder inlineBuilder = new StringBuilder();
			try {
				inlineBuilder.append( "if ( $$_hibernate_getInterceptor() != null " );
				//primitives || enums
				if ( currentValue.getType().isPrimitive() || currentValue.getType().isEnum() ) {
					inlineBuilder.append( "&& " ).append( currentValue.getName() ).append( " != $1)" );
				}
				//simple data types
				else if ( currentValue.getType().getName().startsWith( "java.lang" )
						|| currentValue.getType().getName().startsWith( "java.math.Big" )
						|| currentValue.getType().getName().startsWith( "java.sql.Time" )
						|| currentValue.getType().getName().startsWith( "java.sql.Date" )
						|| currentValue.getType().getName().startsWith( "java.util.Date" )
						|| currentValue.getType().getName().startsWith( "java.util.Calendar" ) ) {
					inlineBuilder.append( "&& ((" )
							.append( currentValue.getName() )
							.append( " == null) || (!" )
							.append( currentValue.getName() )
							.append( ".equals( $1))))" );
				}
				//all other objects
				else {
					//if the field is a collection we return since we handle that in a separate method
					for ( CtClass ctClass : currentValue.getType().getInterfaces() ) {
						if ( ctClass.getName().equals( "java.util.Collection" ) ) {

							//if the collection is not managed we should write it to the tracker
							//todo: should use EnhancementContext.isMappedCollection here instead
							if ( currentValue.getAnnotation( OneToMany.class ) != null ||
									currentValue.getAnnotation( ManyToMany.class ) != null ||
									currentValue.getAnnotation( ElementCollection.class ) != null ) {
								return "";
							}
						}
					}

					//todo: for now just call equals, should probably do something else here
					inlineBuilder.append( "&& ((" )
							.append( currentValue.getName() )
							.append( " == null) || (!" )
							.append( currentValue.getName() )
							.append( ".equals( $1))))" );
				}

				inlineBuilder.append( EnhancerConstants.TRACKER_CHANGER_NAME + "(\"" )
						.append( currentValue.getName() )
						.append( "\");" );
			}
			catch (NotFoundException e) {
				e.printStackTrace();
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return inlineBuilder.toString();
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
