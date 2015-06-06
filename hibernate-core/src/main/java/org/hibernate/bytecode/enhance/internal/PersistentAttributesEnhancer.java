/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Embedded;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import javassist.CannotCompileException;
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
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.stackmap.MapMaker;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
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
public class PersistentAttributesEnhancer extends Enhancer {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( PersistentAttributesEnhancer.class );

	public PersistentAttributesEnhancer(EnhancementContext context) {
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

		// lastly, find all references to the transformed fields and replace with calls to the added reader/writer methods
		enhanceAttributesAccess( managedCtClass, attrDescriptorMap );
	}

	private CtField[] collectPersistentFields(CtClass managedCtClass) {
		final List<CtField> persistentFieldList = new LinkedList<CtField>();
		for ( CtField ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields and skip fields added by enhancement
			if ( Modifier.isStatic( ctField.getModifiers() ) || ctField.getName().startsWith( "$$_hibernate_" ) ) {
				continue;
			}
			if ( enhancementContext.isPersistentField( ctField ) ) {
				persistentFieldList.add( ctField );
			}
		}
		return enhancementContext.order( persistentFieldList.toArray( new CtField[persistentFieldList.size()] ) );
	}

	private PersistentAttributeAccessMethods enhancePersistentAttribute(
			CtClass managedCtClass,
			CtField persistentField) {
		try {
			final AttributeTypeDescriptor typeDescriptor = AttributeTypeDescriptor.resolve( persistentField );
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

	private CtMethod generateFieldReader(
			CtClass managedCtClass,
			CtField persistentField,
			AttributeTypeDescriptor typeDescriptor) {
		final String fieldName = persistentField.getName();
		final String readerName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + fieldName;

		// read attempts only have to deal lazy-loading support, not dirty checking;
		// so if the field is not enabled as lazy-loadable return a plain simple getter as the reader
		if ( !enhancementContext.isLazyLoadable( persistentField ) ) {
			return MethodWriter.addGetter( managedCtClass, fieldName, readerName );
		}

		// TODO: temporary solution...
		try {
			return MethodWriter.write(
					managedCtClass, "public %s %s() {%n  %s%n  return this.%s;%n}",
					persistentField.getType().getName(),
					readerName,
					typeDescriptor.buildReadInterceptionBodyFragment( fieldName ),
					fieldName
			);
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

	private CtMethod generateFieldWriter(
			CtClass managedCtClass,
			CtField persistentField,
			AttributeTypeDescriptor typeDescriptor) {
		final String fieldName = persistentField.getName();
		final String writerName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + fieldName;

		try {
			final CtMethod writer;

			if ( !enhancementContext.isLazyLoadable( persistentField ) ) {
				writer = MethodWriter.addSetter( managedCtClass, fieldName, writerName );
			}
			else {
				writer = MethodWriter.write(
						managedCtClass,
						"public void %s(%s %s) {%n  %s%n}",
						writerName,
						persistentField.getType().getName(),
						fieldName,
						typeDescriptor.buildWriteInterceptionBodyFragment( fieldName )
				);
			}

			if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
				writer.insertBefore(
						String.format(
								"if (%s != null) { %<s.callOwner(\".%s\"); }%n",
								EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME,
								fieldName
						)
				);
			}
			else if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				writer.insertBefore(
						typeDescriptor.buildInLineDirtyCheckingBodyFragment(
								enhancementContext,
								persistentField
						)
				);
			}

			handleCompositeField( managedCtClass, persistentField, writer );

			if ( enhancementContext.doBiDirectionalAssociationManagement( persistentField ) ) {
				handleBiDirectionalAssociation( managedCtClass, persistentField, writer );
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
		if ( !isPossibleBiDirectionalAssociation( persistentField ) ) {
			return;
		}
		final CtClass targetEntity = getTargetEntityClass( persistentField );
		if ( targetEntity == null ) {
			log.debugf(
					"Could not find type of bi-directional association for field [%s#%s]",
					managedCtClass.getName(),
					persistentField.getName()
			);
			return;
		}
		final String mappedBy = getMappedBy( persistentField, targetEntity );
		if ( mappedBy.isEmpty() ) {
			log.warnf(
					"Could not find bi-directional association for field [%s#%s]",
					managedCtClass.getName(),
					persistentField.getName()
			);
			return;
		}

		// create a temporary getter and setter on the target entity to be able to compile our code
		final String mappedByGetterName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + mappedBy;
		final String mappedBySetterName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + mappedBy;
		MethodWriter.addGetter( targetEntity, mappedBy, mappedByGetterName );
		MethodWriter.addSetter( targetEntity, mappedBy, mappedBySetterName );

		if ( persistentField.hasAnnotation( OneToOne.class ) ) {
			// only unset when $1 != null to avoid recursion
			fieldWriter.insertBefore(
					String.format(
							"if ($0.%s != null && $1 != null) $0.%<s.%s(null);%n",
							persistentField.getName(),
							mappedBySetterName
					)
			);
			fieldWriter.insertAfter(
					String.format(
							"if ($1 != null && $1.%s() != $0) $1.%s($0);%n",
							mappedByGetterName,
							mappedBySetterName
					)
			);
		}
		if ( persistentField.hasAnnotation( OneToMany.class ) ) {
			// only remove elements not in the new collection or else we would loose those elements
			// don't use iterator to avoid ConcurrentModException
			fieldWriter.insertBefore(
					String.format(
							"if ($0.%s != null) { Object[] array = $0.%<s.toArray(); for (int i = 0; i < array.length; i++) { %s target = (%<s) array[i]; if ($1 == null || !$1.contains(target)) target.%s(null); } }%n",
							persistentField.getName(),
							targetEntity.getName(),
							mappedBySetterName
					)
			);
			fieldWriter.insertAfter(
					String.format(
							"if ($1 != null) { Object[] array = $1.toArray(); for (int i = 0; i < array.length; i++) { %s target = (%<s) array[i]; if (target.%s() != $0) target.%s((%s)$0); } }%n",
							targetEntity.getName(),
							mappedByGetterName,
							mappedBySetterName,
							managedCtClass.getName()
					)
			);
		}
		if ( persistentField.hasAnnotation( ManyToOne.class ) ) {
			fieldWriter.insertBefore(
					String.format(
							"if ($0.%1$s != null && $0.%1$s.%2$s() != null) $0.%1$s.%2$s().remove($0);%n",
							persistentField.getName(),
							mappedByGetterName
					)
			);
			// check .contains($0) to avoid double inserts (but preventing duplicates)
			fieldWriter.insertAfter(
					String.format(
							"if ($1 != null) { java.util.Collection c = $1.%s(); if (c != null && !c.contains($0)) c.add($0); }%n",
							mappedByGetterName
					)
			);
		}
		if ( persistentField.hasAnnotation( ManyToMany.class ) ) {
			fieldWriter.insertBefore(
					String.format(
							"if ($0.%s != null) { Object[] array = $0.%<s.toArray(); for (int i = 0; i < array.length; i++) { %s target = (%<s) array[i]; if ($1 == null || !$1.contains(target)) target.%s().remove($0); } }%n",
							persistentField.getName(),
							targetEntity.getName(),
							mappedByGetterName
					)
			);
			fieldWriter.insertAfter(
					String.format(
							"if ($1 != null) { Object[] array = $1.toArray(); for (int i = 0; i < array.length; i++) { %s target = (%<s) array[i]; java.util.Collection c = target.%s(); if ( c != $0 && c != null) c.add($0); } }%n",
							targetEntity.getName(),
							mappedByGetterName
					)
			);
		}
		// implementation note: association management @OneToMany and @ManyToMay works for add() operations but for remove() a snapshot of the collection is needed so we know what associations to break.
		// another approach that could force that behavior would be to return Collections.unmodifiableCollection() ...
	}

	private boolean isPossibleBiDirectionalAssociation(CtField persistentField) {
		return persistentField.hasAnnotation( OneToOne.class ) ||
				persistentField.hasAnnotation( OneToMany.class ) ||
				persistentField.hasAnnotation( ManyToOne.class ) ||
				persistentField.hasAnnotation( ManyToMany.class );
	}

	private String getMappedBy(CtField persistentField, CtClass targetEntity) {
		final String local = getMappedByFromAnnotation( persistentField );
		return local.isEmpty() ? getMappedByFromTargetEntity( persistentField, targetEntity ) : local;
	}

	private String getMappedByFromAnnotation(CtField persistentField) {
		try {
			if ( persistentField.hasAnnotation( OneToOne.class ) ) {
				return ( (OneToOne) persistentField.getAnnotation( OneToOne.class ) ).mappedBy();
			}
			if ( persistentField.hasAnnotation( OneToMany.class ) ) {
				return ( (OneToMany) persistentField.getAnnotation( OneToMany.class ) ).mappedBy();
			}
			// For @ManyToOne associations, mappedBy must come from the @OneToMany side of the association
			if ( persistentField.hasAnnotation( ManyToMany.class ) ) {
				return ( (ManyToMany) persistentField.getAnnotation( ManyToMany.class ) ).mappedBy();
			}
		}
		catch (ClassNotFoundException ignore) {
		}
		return "";
	}

	private String getMappedByFromTargetEntity(CtField persistentField, CtClass targetEntity) {
		// get mappedBy value by searching in the fields of the target entity class
		for ( CtField f : targetEntity.getDeclaredFields() ) {
			if ( enhancementContext.isPersistentField( f ) && getMappedByFromAnnotation( f ).equals( persistentField.getName() ) ) {
				log.debugf(
						"mappedBy association for field [%s:%s] is [%s:%s]",
						persistentField.getDeclaringClass().getName(),
						persistentField.getName(),
						targetEntity.getName(),
						f.getName()
				);
				return f.getName();
			}
		}
		return "";
	}

	private CtClass getTargetEntityClass(CtField persistentField) throws NotFoundException {
		// get targetEntity defined in the annotation
		try {
			Class<?> targetClass = null;
			if ( persistentField.hasAnnotation( OneToOne.class ) ) {
				targetClass = ( (OneToOne) persistentField.getAnnotation( OneToOne.class ) ).targetEntity();
			}
			if ( persistentField.hasAnnotation( OneToMany.class ) ) {
				targetClass = ( (OneToMany) persistentField.getAnnotation( OneToMany.class ) ).targetEntity();
			}
			if ( persistentField.hasAnnotation( ManyToOne.class ) ) {
				targetClass = ( (ManyToOne) persistentField.getAnnotation( ManyToOne.class ) ).targetEntity();
			}
			if ( persistentField.hasAnnotation( ManyToMany.class ) ) {
				targetClass = ( (ManyToMany) persistentField.getAnnotation( ManyToMany.class ) ).targetEntity();
			}
			if ( targetClass != null && targetClass != void.class ) {
				return classPool.get( targetClass.getName() );
			}
		}
		catch (ClassNotFoundException ignore) {
		}

		// infer targetEntity from generic type signature
		if ( persistentField.hasAnnotation( OneToOne.class ) || persistentField.hasAnnotation( ManyToOne.class ) ) {
			return persistentField.getType();
		}
		if ( persistentField.hasAnnotation( OneToMany.class ) || persistentField.hasAnnotation( ManyToMany.class ) ) {
			try {
				final SignatureAttribute.TypeArgument target = ( (SignatureAttribute.ClassType) SignatureAttribute.toFieldSignature(
						persistentField.getGenericSignature()
				) ).getTypeArguments()[0];
				return persistentField.getDeclaringClass().getClassPool().get( target.toString() );
			}
			catch (BadBytecode ignore) {
			}
		}
		return null;
	}

	private void handleCompositeField(CtClass managedCtClass, CtField persistentField, CtMethod fieldWriter)
			throws NotFoundException, CannotCompileException {
		if ( !persistentField.hasAnnotation( Embedded.class ) ) {
			return;
		}

		// make sure to add the CompositeOwner interface
		managedCtClass.addInterface( classPool.get( CompositeOwner.class.getName() ) );

		if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
			// if a composite have a embedded field we need to implement the TRACKER_CHANGER_NAME method as well
			MethodWriter.write(
					managedCtClass, "" +
							"public void %1$s(String name) {%n" +
							"  if (%2$s != null) { %2$s.callOwner(\".\" + name) ; }%n}",
					EnhancerConstants.TRACKER_CHANGER_NAME,
					EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME
			);
		}

		// cleanup previous owner
		fieldWriter.insertBefore(
				String.format(
						"" +
								"if (%1$s != null) { ((%2$s) %1$s).%3$s(\"%1$s\"); }%n",
						persistentField.getName(),
						CompositeTracker.class.getName(),
						EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER
				)
		);

		// trigger track changes
		fieldWriter.insertAfter(
				String.format(
						"" +
								"((%2$s) %1$s).%4$s(\"%1$s\", (%3$s) this);%n" +
								"%5$s(\"%1$s\");",
						persistentField.getName(),
						CompositeTracker.class.getName(),
						CompositeOwner.class.getName(),
						EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER,
						EnhancerConstants.TRACKER_CHANGER_NAME
				)
		);
	}

	protected void enhanceAttributesAccess(
			CtClass managedCtClass,
			IdentityHashMap<String, PersistentAttributeAccessMethods> attributeDescriptorMap) {
		final ConstPool constPool = managedCtClass.getClassFile().getConstPool();

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
						itr.writeByte( Opcode.INVOKESPECIAL, index );
						itr.write16bit( methodIndex, index + 1 );
					}
					else {
						final int methodIndex = MethodWriter.addMethod( constPool, attributeMethods.getWriter() );
						itr.writeByte( Opcode.INVOKESPECIAL, index );
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

}
