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
import javassist.Modifier;
import javassist.NotFoundException;
import org.hibernate.bytecode.enhance.internal.tracker.CollectionTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleDirtyTracker;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.SelfDirtinessTracker;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * enhancer for regular entities
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class EntityEnhancer extends Enhancer {

	public EntityEnhancer(EnhancementContext context) {
		super( context );
	}

	// for very small sizes SimpleDirtyTracker implementation ends up being faster
	private static final String TRACKER_IMPL = SimpleDirtyTracker.class.getName();

	public void enhance(CtClass managedCtClass) {
		// add the ManagedEntity interface
		managedCtClass.addInterface( managedEntityCtClass );

		addEntityInstanceHandling( managedCtClass );
		addEntityEntryHandling( managedCtClass );
		addLinkedPreviousHandling( managedCtClass );
		addLinkedNextHandling( managedCtClass );
		addInterceptorHandling( managedCtClass );

		if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
			addInLineDirtyHandling( managedCtClass );
		}

		new PersistentAttributesEnhancer( enhancementContext ).enhance( managedCtClass );
	}

	/* -- */

	private void addEntityInstanceHandling(CtClass managedCtClass) {
		try {
			MethodWriter.write( managedCtClass, "public Object %s() { return this; }", EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME );
		}
		catch (CannotCompileException cce) {
			final String msg = String.format( "Could not enhance entity class [%s] to add EntityEntry getter", managedCtClass.getName() );
			throw new EnhancementException(msg, cce);
		}
	}

	/* -- */

	private void addEntityEntryHandling(CtClass managedCtClass) {
		FieldWriter.addFieldWithGetterAndSetter( managedCtClass, entityEntryCtClass,
				EnhancerConstants.ENTITY_ENTRY_FIELD_NAME,
				EnhancerConstants.ENTITY_ENTRY_GETTER_NAME,
				EnhancerConstants.ENTITY_ENTRY_SETTER_NAME );
	}

	private void addLinkedPreviousHandling(CtClass managedCtClass) {
		FieldWriter.addFieldWithGetterAndSetter( managedCtClass, managedEntityCtClass,
				EnhancerConstants.PREVIOUS_FIELD_NAME,
				EnhancerConstants.PREVIOUS_GETTER_NAME,
				EnhancerConstants.PREVIOUS_SETTER_NAME );
	}

	private void addLinkedNextHandling(CtClass managedCtClass) {
		FieldWriter.addFieldWithGetterAndSetter( managedCtClass, managedEntityCtClass,
				EnhancerConstants.NEXT_FIELD_NAME,
				EnhancerConstants.NEXT_GETTER_NAME,
				EnhancerConstants.NEXT_SETTER_NAME );
	}

	/* --- */

	private void addInLineDirtyHandling(CtClass managedCtClass) {
		try {
			managedCtClass.addInterface( classPool.get( SelfDirtinessTracker.class.getName() ) );

			FieldWriter.addField( managedCtClass, classPool.get( TRACKER_IMPL ), EnhancerConstants.TRACKER_FIELD_NAME );
			FieldWriter.addField( managedCtClass, classPool.get( CollectionTracker.class.getName() ), EnhancerConstants.TRACKER_COLLECTION_NAME );

			createDirtyTrackerMethods( managedCtClass );
		}
		catch (NotFoundException nfe) {
			nfe.printStackTrace();
		}
	}

	private void createDirtyTrackerMethods(CtClass managedCtClass) {
		try {
			MethodWriter.write( managedCtClass, "" +
							"public void %1$s(String name) {%n" +
							"  if (%2$s == null) { %2$s = new %3$s(); }%n" +
							"  %2$s.add(name);%n" +
							"}",
					EnhancerConstants.TRACKER_CHANGER_NAME,
					EnhancerConstants.TRACKER_FIELD_NAME,
					TRACKER_IMPL );

			/* --- */

			createCollectionDirtyCheckMethod( managedCtClass );
			createCollectionDirtyCheckGetFieldsMethod( managedCtClass );
			createClearDirtyCollectionMethod( managedCtClass );

			/* --- */

			MethodWriter.write( managedCtClass, "" +
							"public java.util.Set %1$s() {%n" +
							"  if (%2$s == null) { %2$s = new %4$s(); }%n" +
							"  %3$s(%2$s);%n" +
							"  return %2$s.asSet();%n" +
							"}",
					EnhancerConstants.TRACKER_GET_NAME,
					EnhancerConstants.TRACKER_FIELD_NAME,
					EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME,
					TRACKER_IMPL );

			MethodWriter.write( managedCtClass, "" +
							"public boolean %1$s() {%n" +
							"  return (%2$s != null && !%2$s.isEmpty()) || %3$s();%n" +
							"}",
					EnhancerConstants.TRACKER_HAS_CHANGED_NAME,
					EnhancerConstants.TRACKER_FIELD_NAME,
					EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME );

			MethodWriter.write( managedCtClass, "" +
							"public void %1$s() {%n" +
							"  if (%2$s != null) { %2$s.clear(); }%n" +
							"  %3$s();%n" +
							"}",
					EnhancerConstants.TRACKER_CLEAR_NAME,
					EnhancerConstants.TRACKER_FIELD_NAME,
					EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME );
		}
		catch (CannotCompileException cce) {
			cce.printStackTrace();
		}
	}

	/* -- */

	private List<CtField> collectCollectionFields(CtClass managedCtClass) {
		final List<CtField> collectionList = new LinkedList<CtField>();
		try {
			for ( CtField ctField : managedCtClass.getDeclaredFields() ) {
				// skip static fields and skip fields added by enhancement
				if ( Modifier.isStatic( ctField.getModifiers() ) || ctField.getName().startsWith( "$$_hibernate_" ) ) {
					continue;
				}
				if ( enhancementContext.isPersistentField( ctField ) ) {
					for ( CtClass ctClass : ctField.getType().getInterfaces() ) {
						if ( ctClass.getName().equals( Collection.class.getName() ) ) {
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

	private void createCollectionDirtyCheckMethod(CtClass managedCtClass) {
		try {
			final StringBuilder body = new StringBuilder();

			body.append( String.format( "" +
							"private boolean %1$s() {%n" +
							"  if (%2$s() == null || %3$s == null) { return false; }%n",
					EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME,
					EnhancerConstants.INTERCEPTOR_GETTER_NAME,
					EnhancerConstants.TRACKER_COLLECTION_NAME ) );

			for ( CtField ctField : collectCollectionFields( managedCtClass ) ) {
				if ( !enhancementContext.isMappedCollection( ctField )) {
					body.append( String.format( "" +
									"  // collection field [%1$s]%n" +
									"  if (%1$s == null && %2$s.getSize(\"%1$s\") != -1) { return true; }%n"+
									"  if (%1$s != null && %2$s.getSize(\"%1$s\") != %1$s.size()) { return true; }%n",
							ctField.getName(),
							EnhancerConstants.TRACKER_COLLECTION_NAME ) );
				}
			}
			body.append( "  return false;%n}" );

			MethodWriter.write( managedCtClass, body.toString() );
		}
		catch (CannotCompileException cce) {
			cce.printStackTrace();
		}
	}

	private void createCollectionDirtyCheckGetFieldsMethod(CtClass managedCtClass) {
		try {
			final StringBuilder body = new StringBuilder();

			body.append( String.format( "" +
							"private void %1$s(%3$s tracker) {%n" +
							"  if (%2$s == null) { return; }%n",
					EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME,
					EnhancerConstants.TRACKER_COLLECTION_NAME,
					TRACKER_IMPL ) );

			for ( CtField ctField : collectCollectionFields( managedCtClass ) ) {
				if ( !enhancementContext.isMappedCollection( ctField )) {
					body.append( String.format( "" +
									"  // Collection field [%1$s]%n" +
									"  if (%1$s == null && %2$s.getSize(\"%1$s\") != -1) { tracker.add(\"%1$s\"); }%n"+
									"  if (%1$s != null && %2$s.getSize(\"%1$s\") != %1$s.size()) { tracker.add(\"%1$s\"); }%n",
							ctField.getName(),
							EnhancerConstants.TRACKER_COLLECTION_NAME ) );
				}
			}
			body.append( "}" );

			MethodWriter.write( managedCtClass, body.toString() );
		}
		catch (CannotCompileException cce) {
			cce.printStackTrace();
		}
	}

	private void createClearDirtyCollectionMethod(CtClass managedCtClass) throws CannotCompileException {
		try {
			final StringBuilder body = new StringBuilder();

			body.append( String.format( "" +
							"private void %1$s() {%n" +
							"  if (%2$s == null) { %2$s = new %3$s(); }%n",
					EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME,
					EnhancerConstants.TRACKER_COLLECTION_NAME,
					CollectionTracker.class.getName()) );

			for ( CtField ctField : collectCollectionFields( managedCtClass ) ) {
				if ( !enhancementContext.isMappedCollection( ctField ) ) {
					body.append( String.format( "" +
									"  // Collection field [%1$s]%n" +
									"  if (%1$s == null) { %2$s.add(\"%1$s\", -1); }%n"+
									"  else { %2$s.add(\"%1$s\", %1$s.size()); }%n",
							ctField.getName(),
							EnhancerConstants.TRACKER_COLLECTION_NAME) );
				}
			}
			body.append( "}" );

			MethodWriter.write( managedCtClass, body.toString() );
		}
		catch (CannotCompileException cce) {
			cce.printStackTrace();
		}
	}

}
