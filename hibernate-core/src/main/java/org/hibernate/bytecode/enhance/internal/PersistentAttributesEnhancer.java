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
package org.hibernate.bytecode.enhance.internal;

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
import javassist.bytecode.stackmap.MapMaker;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import javax.persistence.Embedded;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;

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
			attrDescriptorMap.put( persistentField.getName(), enhancePersistentAttribute( managedCtClass, persistentField ) );
		}

		// lastly, find all references to the transformed fields and replace with calls to the added reader/writer methods
		enhanceAttributesAccess( managedCtClass, attrDescriptorMap );
	}

	/* --- */

	// TODO: drive this from the Hibernate metamodel instance...
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
		return enhancementContext.order( persistentFieldList.toArray( new CtField[ persistentFieldList.size() ] ) );
	}

	/* --- */

	private PersistentAttributeAccessMethods enhancePersistentAttribute(CtClass managedCtClass, CtField persistentField) {
		try {
			final AttributeTypeDescriptor typeDescriptor = AttributeTypeDescriptor.resolve( persistentField );
			return new PersistentAttributeAccessMethods(
					generateFieldReader( managedCtClass, persistentField, typeDescriptor ),
					generateFieldWriter( managedCtClass, persistentField, typeDescriptor ) );
		}
		catch (Exception e) {
			final String msg = String.format( "Unable to enhance persistent attribute [%s:%s]", managedCtClass.getName(), persistentField.getName() );
			throw new EnhancementException( msg, e );
		}
	}

	private CtMethod generateFieldReader(CtClass managedCtClass, CtField persistentField, AttributeTypeDescriptor typeDescriptor) {
		final String fieldName = persistentField.getName();
		final String readerName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + fieldName;

		// read attempts only have to deal lazy-loading support, not dirty checking;
		// so if the field is not enabled as lazy-loadable return a plain simple getter as the reader
		if ( !enhancementContext.isLazyLoadable( persistentField ) ) {
			return MethodWriter.addGetter( managedCtClass, fieldName, readerName );
		}

		// TODO: temporary solution...
		try {
			return MethodWriter.write( managedCtClass, "private %s %s() {%n  %s%n  return this.%s;%n}",
					persistentField.getType().getName(),
					readerName,
					typeDescriptor.buildReadInterceptionBodyFragment( fieldName ),
					fieldName);
		}
		catch (CannotCompileException cce) {
			final String msg = String.format( "Could not enhance entity class [%s] to add field reader method [%s]",  managedCtClass.getName(), readerName );
			throw new EnhancementException( msg, cce );
		}
		catch (NotFoundException nfe) {
			final String msg = String.format( "Could not enhance entity class [%s] to add field reader method [%s]",  managedCtClass.getName(), readerName );
			throw new EnhancementException( msg, nfe );
		}
	}

	private CtMethod generateFieldWriter(CtClass managedCtClass, CtField persistentField, AttributeTypeDescriptor typeDescriptor) {
		final String fieldName = persistentField.getName();
		final String writerName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + fieldName;

		try {
			final CtMethod writer;

			if ( !enhancementContext.isLazyLoadable( persistentField ) ) {
				writer = MethodWriter.addSetter( managedCtClass, fieldName, writerName );
			}
			else {
				writer = MethodWriter.write( managedCtClass, "private void %s(%s %s) {%n  %s%n}",
						writerName,
						persistentField.getType().getName(),
						fieldName,
						typeDescriptor.buildWriteInterceptionBodyFragment( fieldName ) );
			}

			if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
				writer.insertBefore( String.format( "if (%s != null) { %<s.callOwner(\".%s\"); }%n",
						EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME,
						fieldName ) );
			}
			else if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
				writer.insertBefore( typeDescriptor.buildInLineDirtyCheckingBodyFragment( enhancementContext, persistentField ) );
			}

			// composite fields
			if ( persistentField.hasAnnotation( Embedded.class ) ) {
				// make sure to add the CompositeOwner interface
				managedCtClass.addInterface( classPool.get( CompositeOwner.class.getName() ) );

				if ( enhancementContext.isCompositeClass( managedCtClass ) ) {
					// if a composite have a embedded field we need to implement the TRACKER_CHANGER_NAME method as well
					MethodWriter.write( managedCtClass, "" +
							"public void %1$s(String name) {%n" +
							"  if (%2$s != null) { %2$s.callOwner(\".\" + name) ; }%n}",
							EnhancerConstants.TRACKER_CHANGER_NAME,
							EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME );
				}

				// cleanup previous owner
				writer.insertBefore( String.format( "" +
								"if (%1$s != null) { ((%2$s) %1$s).%3$s(\"%1$s\"); }%n",
						fieldName,
						CompositeTracker.class.getName(),
						EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER ) );

				// trigger track changes
				writer.insertAfter( String.format( "" +
								"((%2$s) %1$s).%4$s(\"%1$s\", (%3$s) this);%n" +
								"%5$s(\"%1$s\");",
						fieldName,
						CompositeTracker.class.getName(),
						CompositeOwner.class.getName(),
						EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER,
						EnhancerConstants.TRACKER_CHANGER_NAME ) );
			}
			return writer;
		}
		catch (CannotCompileException cce) {
			final String msg = String.format( "Could not enhance entity class [%s] to add field writer method [%s]",  managedCtClass.getName(), writerName );
			throw new EnhancementException( msg, cce );
		}
		catch (NotFoundException nfe) {
			final String msg = String.format( "Could not enhance entity class [%s] to add field writer method [%s]",  managedCtClass.getName(), writerName );
			throw new EnhancementException( msg, nfe );
		}
	}

	/* --- */

	protected void enhanceAttributesAccess(CtClass managedCtClass, IdentityHashMap<String, PersistentAttributeAccessMethods> attributeDescriptorMap) {
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
				final String msg = String.format( "Unable to perform field access transformation in method [%s]",  methodName );
				throw new EnhancementException( msg, bb );
			}
		}
	}

	/* --- */

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
