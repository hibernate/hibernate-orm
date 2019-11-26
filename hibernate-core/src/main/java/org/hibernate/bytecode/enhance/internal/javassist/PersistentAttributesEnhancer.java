/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.stackmap.MapMaker;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * enhancer for persistent attributes of any type of entity
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class PersistentAttributesEnhancer extends EnhancerImpl {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( PersistentAttributesEnhancer.class );

	public PersistentAttributesEnhancer(JavassistEnhancementContext context) {
		super( context );
	}

	public void enhance(CtClass managedCtClass) {
		final IdentityHashMap<String, PersistentAttributeAccessMethods> attrDescriptorMap = new IdentityHashMap<String, PersistentAttributeAccessMethods>();

		for ( CtField persistentField : collectPersistentFields( managedCtClass ) ) {
			attrDescriptorMap.put(
					persistentField.getName(), enhancePersistentAttribute(
							managedCtClass,
							persistentField
					)
			);
		}

		// find all references to the transformed fields and replace with calls to the added reader/writer methods
		enhanceAttributesAccess( managedCtClass, attrDescriptorMap );

		// same thing for direct access to fields of other entities
		if ( this.enhancementContext.doExtendedEnhancement( managedCtClass ) ) {
			extendedEnhancement( managedCtClass );
		}
	}

	private CtField[] collectPersistentFields(CtClass managedCtClass) {
		List<CtField> persistentFieldList = new ArrayList<CtField>();
		for ( CtField ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields and skip fields added by enhancement and  outer reference in inner classes
			if ( ctField.getName().startsWith( "$$_hibernate_" ) || "this$0".equals( ctField.getName() ) ) {
				continue;
			}
			if ( !Modifier.isStatic( ctField.getModifiers() ) && enhancementContext.isPersistentField( ctField ) ) {
				persistentFieldList.add( ctField );
			}
		}
		// HHH-10646 Add fields inherited from @MappedSuperclass
		// HHH-10981 There is no need to do it for @MappedSuperclass
		if ( !enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			persistentFieldList.addAll( collectInheritPersistentFields( managedCtClass ) );
		}

		CtField[] orderedFields = enhancementContext.order( persistentFieldList.toArray( new CtField[0] ) );
		log.debugf( "Persistent fields for entity %s: %s", managedCtClass.getName(), Arrays.toString( orderedFields ) );
		return orderedFields;
	}

	private Collection<CtField> collectInheritPersistentFields(CtClass managedCtClass) {
		if ( managedCtClass == null || Object.class.getName().equals( managedCtClass.getName() ) ) {
			return Collections.emptyList();
		}
		try {
			CtClass managedCtSuperclass = managedCtClass.getSuperclass();

			if ( enhancementContext.isEntityClass( managedCtSuperclass ) ) {
				return Collections.emptyList();
			}
			else if ( !enhancementContext.isMappedSuperclassClass( managedCtSuperclass ) ) {
				return collectInheritPersistentFields( managedCtSuperclass );
			}
			log.debugf( "Found @MappedSuperclass %s to collectPersistenceFields", managedCtSuperclass.getName() );
			List<CtField> persistentFieldList = new ArrayList<CtField>();

			for ( CtField ctField : managedCtSuperclass.getDeclaredFields() ) {
				if ( ctField.getName().startsWith( "$$_hibernate_" ) || "this$0".equals( ctField.getName() ) ) {
					continue;
				}
				if ( !Modifier.isStatic( ctField.getModifiers() ) && enhancementContext.isPersistentField( ctField ) ) {
					persistentFieldList.add( ctField );
				}
			}
			persistentFieldList.addAll( collectInheritPersistentFields( managedCtSuperclass ) );
			return persistentFieldList;
		}
		catch ( NotFoundException nfe ) {
			log.warnf( "Could not find the superclass of %s", managedCtClass );
			return Collections.emptyList();
		}
	}

	private PersistentAttributeAccessMethods enhancePersistentAttribute(CtClass managedCtClass, CtField persistentField) {
		try {
			AttributeTypeDescriptor typeDescriptor = AttributeTypeDescriptor.resolve( managedCtClass, persistentField );
			return new PersistentAttributeAccessMethods(
					generateFieldReader( managedCtClass, persistentField, typeDescriptor ),
					generateFieldWriter( managedCtClass, persistentField, typeDescriptor )
			);
		}
		catch (Exception e) {
			final String msg = String.format(
					"Unable to enhance persistent attribute [%s:%s]",
					managedCtClass.getName(),
					persistentField.getName()
			);
			throw new EnhancementException( msg, e );
		}
	}

	protected CtMethod generateFieldReader(
			CtClass managedCtClass,
			CtField persistentField,
			AttributeTypeDescriptor typeDescriptor) {
		String fieldName = persistentField.getName();
		String readerName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + fieldName;
		String writerName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + fieldName;
		CtMethod tmpSuperReader = null;
		CtMethod tmpSuperWriter = null;
		CtMethod reader = null;

		try {
			boolean declared = persistentField.getDeclaringClass().equals( managedCtClass );
			String declaredReadFragment = "this." + fieldName;
			String superReadFragment = "super." + readerName + "()";

			if ( !declared ) {
				// create a temporary getter on the supper entity to be able to compile our code
				try {
					persistentField.getDeclaringClass().getDeclaredMethod( readerName );
					persistentField.getDeclaringClass().getDeclaredMethod( writerName );
				}
				catch ( NotFoundException nfe ) {
					tmpSuperReader = MethodWriter.addGetter( persistentField.getDeclaringClass(), persistentField.getName(), readerName );
					tmpSuperWriter = MethodWriter.addSetter( persistentField.getDeclaringClass(), persistentField.getName(), writerName );
				}
			}

			// read attempts only have to deal lazy-loading support, not dirty checking;
			// so if the field is not enabled as lazy-loadable return a plain simple getter as the reader
			if ( !enhancementContext.hasLazyLoadableAttributes( managedCtClass ) || !enhancementContext.isLazyLoadable( persistentField ) ) {
				reader = MethodWriter.write(
						managedCtClass, "public %s %s() {  return %s;%n}",
						persistentField.getType().getName(),
						readerName,
						declared ? declaredReadFragment : superReadFragment
				);
			}
			else {
				reader = MethodWriter.write(
						managedCtClass, "public %s %s() {%n%s%n  return %s;%n}",
						persistentField.getType().getName(),
						readerName,
						typeDescriptor.buildReadInterceptionBodyFragment( fieldName ),
						declared ? declaredReadFragment : superReadFragment
				);
			}
			if ( tmpSuperReader != null ) {
				persistentField.getDeclaringClass().removeMethod( tmpSuperReader );
			}
			if ( tmpSuperWriter != null ) {
				persistentField.getDeclaringClass().removeMethod( tmpSuperWriter );
			}
			return reader;
		}
		catch (CannotCompileException cce) {
			final String msg = String.format(
					"Could not enhance entity class [%s] to add field reader method [%s]",
					managedCtClass.getName(),
					readerName
			);
			throw new EnhancementException( msg, cce );
		}
		catch (NotFoundException nfe) {
			final String msg = String.format(
					"Could not enhance entity class [%s] to add field reader method [%s]",
					managedCtClass.getName(),
					readerName
			);
			throw new EnhancementException( msg, nfe );
		}
	}

	protected CtMethod generateFieldWriter(
			CtClass managedCtClass,
			CtField persistentField,
			AttributeTypeDescriptor typeDescriptor) {
		String fieldName = persistentField.getName();
		String readerName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + fieldName;
		String writerName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + fieldName;
		CtMethod tmpSuperReader = null;
		CtMethod tmpSuperWriter = null;
		CtMethod writer;

		try {
			boolean declared = persistentField.getDeclaringClass().equals( managedCtClass );
			String declaredWriteFragment = "this." + fieldName + "=" + fieldName + ";";
			String superWriteFragment = "super." + writerName + "(" + fieldName + ");";

			if ( !declared ) {
				// create a temporary setter on the supper entity to be able to compile our code
				try {
					persistentField.getDeclaringClass().getDeclaredMethod( readerName );
					persistentField.getDeclaringClass().getDeclaredMethod( writerName );
				}
				catch ( NotFoundException nfe ) {
					tmpSuperReader = MethodWriter.addGetter( persistentField.getDeclaringClass(), persistentField.getName(), readerName );
					tmpSuperWriter = MethodWriter.addSetter( persistentField.getDeclaringClass(), persistentField.getName(), writerName );
				}
			}

			if ( !enhancementContext.hasLazyLoadableAttributes( managedCtClass ) || !enhancementContext.isLazyLoadable( persistentField ) ) {
				writer = MethodWriter.write(
						managedCtClass,
						"public void %s(%s %s) {%n  %s%n}",
						writerName,
						persistentField.getType().getName(),
						fieldName,
						declared ? declaredWriteFragment : superWriteFragment
				);
			}
			else {
				writer = MethodWriter.write(
						managedCtClass,
						"public void %s(%s %s) {%n%s%n}",
						writerName,
						persistentField.getType().getName(),
						fieldName,
						typeDescriptor.buildWriteInterceptionBodyFragment( fieldName )
				);
			}

			if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
					writer.insertBefore(
							String.format(
									"  if (%1$s != null) { %1$s.callOwner(\"\"); }%n",
									EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME
							)
					);
				}
				else {
					writer.insertBefore( typeDescriptor.buildInLineDirtyCheckingBodyFragment( enhancementContext, persistentField ) );
				}

				handleCompositeField( managedCtClass, persistentField, writer );
			}

			if ( enhancementContext.doBiDirectionalAssociationManagement( persistentField ) ) {
				handleBiDirectionalAssociation( managedCtClass, persistentField, writer );
			}

			if ( tmpSuperReader != null ) {
				persistentField.getDeclaringClass().removeMethod( tmpSuperReader );
			}
			if ( tmpSuperWriter != null ) {
				persistentField.getDeclaringClass().removeMethod( tmpSuperWriter );
			}
			return writer;
		}
		catch (CannotCompileException cce) {
			final String msg = String.format(
					"Could not enhance entity class [%s] to add field writer method [%s]",
					managedCtClass.getName(),
					writerName
			);
			throw new EnhancementException( msg, cce );
		}
		catch (NotFoundException nfe) {
			final String msg = String.format(
					"Could not enhance entity class [%s] to add field writer method [%s]",
					managedCtClass.getName(),
					writerName
			);
			throw new EnhancementException( msg, nfe );
		}
	}

	private void handleBiDirectionalAssociation(CtClass managedCtClass, CtField persistentField, CtMethod fieldWriter)
			throws NotFoundException, CannotCompileException {
		if ( !PersistentAttributesHelper.isPossibleBiDirectionalAssociation( persistentField ) ) {
			return;
		}
		final CtClass targetEntity = PersistentAttributesHelper.getTargetEntityClass( managedCtClass, persistentField );
		if ( targetEntity == null ) {
			log.infof(
					"Bi-directional association not managed for field [%s#%s]: Could not find target type",
					managedCtClass.getName(),
					persistentField.getName()
			);
			return;
		}
		final String mappedBy = PersistentAttributesHelper.getMappedBy( persistentField, targetEntity, enhancementContext );
		if ( mappedBy == null || mappedBy.isEmpty() ) {
			log.infof(
					"Bi-directional association not managed for field [%s#%s]: Could not find target field in [%s]",
					managedCtClass.getName(),
					persistentField.getName(),
					targetEntity.getName()
			);
			return;
		}

		// create a temporary getter and setter on the target entity to be able to compile our code
		final String mappedByGetterName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + mappedBy;
		final String mappedBySetterName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + mappedBy;
		CtMethod getter;
		CtMethod setter;
		boolean tmpTargetMethods = false;
		try {
			getter = targetEntity.getDeclaredMethod( mappedByGetterName );
			setter = targetEntity.getDeclaredMethod( mappedByGetterName );
		}
		catch ( NotFoundException nfe ) {
			getter = MethodWriter.addGetter( targetEntity, mappedBy, mappedByGetterName );
			setter = MethodWriter.addSetter( targetEntity, mappedBy, mappedBySetterName );
			tmpTargetMethods = true;
		}

		// code fragments to check loaded state. We don't want to trigger lazy loading in association management code
		String currentAssociationLoaded = String.format(
				"%s.isPropertyInitialized(this.%s, \"%s\")",
				Hibernate.class.getName(),
				persistentField.getName(),
				mappedBy
		);
		String targetElementLoaded = String.format(
				"%s.isPropertyInitialized(target, \"%s\")",
				Hibernate.class.getName(),
				mappedBy
		);
		String newAssociationLoaded = String.format(
				"%s.isPropertyInitialized($1, \"%s\")",
				Hibernate.class.getName(),
				mappedBy
		);

		if ( PersistentAttributesHelper.hasAnnotation( persistentField, OneToOne.class ) ) {
			// only unset when $1 != null to avoid recursion
			fieldWriter.insertBefore(
					String.format(
							"  if (this.%1$s != null && %2$s && $1 != null) { this.%1$s.%3$s(null); }%n",
							persistentField.getName(),
							currentAssociationLoaded,
							mappedBySetterName
					)
			);
			fieldWriter.insertAfter(
					String.format(
							"  if ($1 != null && %s && $1.%s() != this) { $1.%s(this); }%n",
							newAssociationLoaded,
							mappedByGetterName,
							mappedBySetterName
					)
			);
		}
		if ( PersistentAttributesHelper.hasAnnotation( persistentField, OneToMany.class ) ) {
			boolean isMap = PersistentAttributesHelper.isAssignable( persistentField.getType(), Map.class.getName() );
			String toArrayMethod = isMap ? "values().toArray()" : "toArray()";

			// only remove elements not in the new collection or else we would loose those elements
			// don't use iterator to avoid ConcurrentModException
			fieldWriter.insertBefore(
					String.format(
							"  if (this.%3$s != null && %1$s) {%n" +
									"    Object[] array = this.%3$s.%2$s;%n" +
									"    for (int i = 0; i < array.length; i++) {%n" +
									"      %4$s target = (%4$s) array[i];%n" +
									"      if ($1 == null || !$1.contains(target)) { target.%5$s(null); }%n" +
									"    }%n" +
									"  }%n",
							currentAssociationLoaded,
							toArrayMethod,
							persistentField.getName(),
							targetEntity.getName(),
							mappedBySetterName
					)
			);
			fieldWriter.insertAfter(
					String.format(
							"  if ($1 != null && %1$s) {%n" +
									"    Object[] array = $1.%2$s;%n" +
									"    for (int i = 0; i < array.length; i++) {%n" +
									"      %4$s target = (%4$s) array[i];%n" +
									"      if (%3$s && target.%5$s() != this) { target.%6$s(this); }%n" +
									"    }%n" +
									"  }%n",
							newAssociationLoaded,
							toArrayMethod,
							targetElementLoaded,
							targetEntity.getName(),
							mappedByGetterName,
							mappedBySetterName
					)
			);
		}
		if ( PersistentAttributesHelper.hasAnnotation( persistentField, ManyToOne.class ) ) {
			fieldWriter.insertBefore(
					String.format(
							"  if (this.%2$s != null && %1$s && this.%2$s.%3$s() != null) { this.%2$s.%3$s().remove(this); }%n",
							currentAssociationLoaded,
							persistentField.getName(),
							mappedByGetterName
					)
			);
			// check .contains(this) to avoid double inserts (but preventing duplicates)
			fieldWriter.insertAfter(
					String.format(
							"  if ($1 != null && %s) {%n" +
									"    java.util.Collection c = $1.%s();%n" +
									"    if (c != null && !c.contains(this)) { c.add(this); }%n" +
									"  }%n",
							newAssociationLoaded,
							mappedByGetterName
					)
			);
		}
		if ( PersistentAttributesHelper.hasAnnotation( persistentField, ManyToMany.class ) ) {
			if ( PersistentAttributesHelper.isAssignable( persistentField.getType(), Map.class.getName() ) ||
					PersistentAttributesHelper.isAssignable( targetEntity.getField( mappedBy ).getType(), Map.class.getName() ) ) {
				log.infof(
						"Bi-directional association not managed for field [%s#%s]: @ManyToMany in java.util.Map attribute not supported ",
						managedCtClass.getName(),
						persistentField.getName()
				);
				return;
			}
			fieldWriter.insertBefore(
					String.format(
							"  if (this.%2$s != null && %1$s) {%n" +
									"    Object[] array = this.%2$s.toArray();%n" +
									"    for (int i = 0; i < array.length; i++) {%n" +
									"      %3$s target = (%3$s) array[i];%n" +
									"      if ($1 == null || !$1.contains(target)) { target.%4$s().remove(this); }%n" +
									"    }%n" +
									"  }%n",
							currentAssociationLoaded,
							persistentField.getName(),
							targetEntity.getName(),
							mappedByGetterName
					)
			);
			fieldWriter.insertAfter(
					String.format(
							"  if ($1 != null && %s) {%n" +
									"    Object[] array = $1.toArray();%n" +
									"    for (int i = 0; i < array.length; i++) {%n" +
									"      %s target = (%s) array[i];%n" +
									"	   if (%s) {%n" +
									"        java.util.Collection c = target.%s();%n" +
									"        if (c != this && c != null) { c.add(this); }%n" +
									"      }%n" +
									"    }%n" +
									"  }%n",
							newAssociationLoaded,
							targetEntity.getName(),
							targetEntity.getName(),
							targetElementLoaded,
							mappedByGetterName
					)
			);
		}
		// implementation note: association management @OneToMany and @ManyToMay works for add() operations but for remove() a snapshot of the collection is needed so we know what associations to break.
		// another approach that could force that behavior would be to return Collections.unmodifiableCollection() ...

		if ( tmpTargetMethods ) {
			targetEntity.removeMethod( getter );
			targetEntity.removeMethod( setter );
		}
	}

	private void handleCompositeField(CtClass managedCtClass, CtField persistentField, CtMethod fieldWriter)
			throws NotFoundException, CannotCompileException {
		if ( !enhancementContext.isCompositeClass( persistentField.getType() ) ||
				!PersistentAttributesHelper.hasAnnotation( persistentField, Embedded.class ) ) {
			return;
		}

		// make sure to add the CompositeOwner interface
		addCompositeOwnerInterface( managedCtClass );

		String readFragment = persistentField.visibleFrom( managedCtClass ) ? persistentField.getName() : "super." + EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + persistentField.getName() + "()";

		// cleanup previous owner
		fieldWriter.insertBefore(
				String.format(
						"if (%1$s != null) { ((%2$s) %1$s).%3$s(\"%4$s\"); }%n",
						readFragment,
						CompositeTracker.class.getName(),
						EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER,
						persistentField.getName()
				)
		);

		// trigger track changes
		fieldWriter.insertAfter(
				String.format(
						"if (%1$s != null) { ((%2$s) %1$s).%4$s(\"%6$s\", (%3$s) this); }%n" +
								"%5$s(\"%6$s\");",
						readFragment,
						CompositeTracker.class.getName(),
						CompositeOwner.class.getName(),
						EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER,
						EnhancerConstants.TRACKER_CHANGER_NAME,
						persistentField.getName()
				)
		);
	}

	private void addCompositeOwnerInterface(CtClass managedCtClass) throws NotFoundException, CannotCompileException {
		CtClass compositeOwnerCtClass = managedCtClass.getClassPool().get( CompositeOwner.class.getName() );

		// HHH-10540 only add the interface once
		for ( CtClass i : managedCtClass.getInterfaces() ) {
			if ( i.subclassOf( compositeOwnerCtClass ) ) {
				return;
			}
		}

		managedCtClass.addInterface( compositeOwnerCtClass );

		if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
			// if a composite have a embedded field we need to implement the TRACKER_CHANGER_NAME method as well
			MethodWriter.write(
					managedCtClass,
					"public void %1$s(String name) {%n" +
							"  if (%2$s != null) { %2$s.callOwner(\".\" + name); }%n}",
					EnhancerConstants.TRACKER_CHANGER_NAME,
					EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME
			);
		}
	}

	protected void enhanceAttributesAccess(
			CtClass managedCtClass,
			IdentityHashMap<String, PersistentAttributeAccessMethods> attributeDescriptorMap) {
		final ConstPool constPool = managedCtClass.getClassFile().getConstPool();
		final ClassPool classPool = managedCtClass.getClassPool();

		for ( Object oMethod : managedCtClass.getClassFile().getMethods() ) {
			final MethodInfo methodInfo = (MethodInfo) oMethod;
			final String methodName = methodInfo.getName();

			// skip methods added by enhancement and abstract methods (methods without any code)
			if ( methodName.startsWith( "$$_hibernate_" ) || methodInfo.getCodeAttribute() == null ) {
				continue;
			}

			try {
				final CodeIterator itr = methodInfo.getCodeAttribute().iterator();
				while ( itr.hasNext() ) {
					final int index = itr.next();
					final int op = itr.byteAt( index );
					if ( op != Opcode.PUTFIELD && op != Opcode.GETFIELD ) {
						continue;
					}

					// only transform access to fields of the entity being enhanced
					if ( !managedCtClass.getName().equals( constPool.getFieldrefClassName( itr.u16bitAt( index + 1 ) ) ) ) {
						continue;
					}

					final String fieldName = constPool.getFieldrefName( itr.u16bitAt( index + 1 ) );
					final PersistentAttributeAccessMethods attributeMethods = attributeDescriptorMap.get( fieldName );

					// its not a field we have enhanced for interception, so skip it
					if ( attributeMethods == null ) {
						continue;
					}
					//System.out.printf( "Transforming access to field [%s] from method [%s]%n", fieldName, methodName );
					log.debugf( "Transforming access to field [%s] from method [%s]", fieldName, methodName );

					if ( op == Opcode.GETFIELD ) {
						final int methodIndex = MethodWriter.addMethod( constPool, attributeMethods.getReader() );
						itr.writeByte( Opcode.INVOKEVIRTUAL, index );
						itr.write16bit( methodIndex, index + 1 );
					}
					else {
						final int methodIndex = MethodWriter.addMethod( constPool, attributeMethods.getWriter() );
						itr.writeByte( Opcode.INVOKEVIRTUAL, index );
						itr.write16bit( methodIndex, index + 1 );
					}
				}
				methodInfo.getCodeAttribute().setAttribute( MapMaker.make( classPool, methodInfo ) );
			}
			catch (BadBytecode bb) {
				final String msg = String.format(
						"Unable to perform field access transformation in method [%s]",
						methodName
				);
				throw new EnhancementException( msg, bb );
			}
		}
	}

	private static class PersistentAttributeAccessMethods {
		private final CtMethod reader;
		private final CtMethod writer;

		private PersistentAttributeAccessMethods(CtMethod reader, CtMethod writer) {
			this.reader = reader;
			this.writer = writer;
		}

		private CtMethod getReader() {
			return reader;
		}

		private CtMethod getWriter() {
			return writer;
		}
	}

	// --- //

	/**
	 * Replace access to fields of entities (for example, entity.field) with a call to the enhanced getter / setter
	 * (in this example, entity.$$_hibernate_read_field()). It's assumed that the target entity is enhanced as well.
	 *
	 * @param aCtClass Class to enhance (not an entity class).
	 */
	public void extendedEnhancement(CtClass aCtClass) {
		final ConstPool constPool = aCtClass.getClassFile().getConstPool();
		final ClassPool classPool = aCtClass.getClassPool();

		for ( Object oMethod : aCtClass.getClassFile().getMethods() ) {
			final MethodInfo methodInfo = (MethodInfo) oMethod;
			final String methodName = methodInfo.getName();

			// skip methods added by enhancement and abstract methods (methods without any code)
			if ( methodName.startsWith( "$$_hibernate_" ) || methodInfo.getCodeAttribute() == null ) {
				continue;
			}

			try {
				final CodeIterator itr = methodInfo.getCodeAttribute().iterator();
				while ( itr.hasNext() ) {
					int index = itr.next();
					int op = itr.byteAt( index );
					if ( op != Opcode.PUTFIELD && op != Opcode.GETFIELD ) {
						continue;
					}
					String fieldName = constPool.getFieldrefName( itr.u16bitAt( index + 1 ) );
					String fieldClassName = constPool.getClassInfo( constPool.getFieldrefClass( itr.u16bitAt( index + 1 ) ) );
					CtClass targetCtClass = classPool.getCtClass( fieldClassName );

					if ( !enhancementContext.isEntityClass( targetCtClass ) && !enhancementContext.isCompositeClass( targetCtClass ) ) {
						continue;
					}
					if ( targetCtClass == aCtClass
							|| !enhancementContext.isPersistentField( targetCtClass.getField( fieldName ) )
							|| PersistentAttributesHelper.hasAnnotation( targetCtClass, fieldName, Id.class )
							|| "this$0".equals( fieldName ) ) {
						continue;
					}

					log.debugf(
							"Extended enhancement: Transforming access to field [%s.%s] from method [%s#%s]",
							fieldClassName,
							fieldName,
							aCtClass.getName(),
							methodName
					);

					if ( op == Opcode.GETFIELD ) {
						int fieldReaderMethodIndex = constPool.addMethodrefInfo(
								constPool.addClassInfo( fieldClassName ),
								EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + fieldName,
								"()" + constPool.getFieldrefType( itr.u16bitAt( index + 1 ) )
						);
						itr.writeByte( Opcode.INVOKEVIRTUAL, index );
						itr.write16bit( fieldReaderMethodIndex, index + 1 );
					}
					else {
						int fieldWriterMethodIndex = constPool.addMethodrefInfo(
								constPool.addClassInfo( fieldClassName ),
								EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + fieldName,
								"(" + constPool.getFieldrefType( itr.u16bitAt( index + 1 ) ) + ")V"
						);
						itr.writeByte( Opcode.INVOKEVIRTUAL, index );
						itr.write16bit( fieldWriterMethodIndex, index + 1 );
					}

				}
				methodInfo.getCodeAttribute().setAttribute( MapMaker.make( classPool, methodInfo ) );
			}
			catch (BadBytecode bb) {
				final String msg = String.format(
						"Unable to perform extended enhancement in method [%s]",
						methodName
				);
				throw new EnhancementException( msg, bb );
			}
			catch (NotFoundException nfe) {
				final String msg = String.format(
						"Unable to perform extended enhancement in method [%s]",
						methodName
				);
				throw new EnhancementException( msg, nfe );
			}
		}
	}

}
