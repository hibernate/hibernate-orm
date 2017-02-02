/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;

import javassist.NotFoundException;

import org.hibernate.bytecode.enhance.internal.tracker.DirtyTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleCollectionTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleFieldTracker;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;

/**
 * enhancer for regular entities
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class EntityEnhancer extends PersistentAttributesEnhancer {

	public EntityEnhancer(JavassistEnhancementContext context) {
		super( context );
	}

	// assuming the number of fields is not very high, SimpleFieldTracker implementation it's the fastest
	private static final String DIRTY_TRACKER_IMPL = SimpleFieldTracker.class.getName();
	private static final String COLLECTION_TRACKER_IMPL = SimpleCollectionTracker.class.getName();

	public void enhance(CtClass managedCtClass) {
		// add the ManagedEntity interface
		managedCtClass.addInterface( loadCtClassFromClass( ManagedEntity.class ) );

		addEntityInstanceHandling( managedCtClass );
		addEntityEntryHandling( managedCtClass );
		addLinkedPreviousHandling( managedCtClass );
		addLinkedNextHandling( managedCtClass );
		addInterceptorHandling( managedCtClass );

		if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
			addInLineDirtyHandling( managedCtClass );
		}

		super.enhance( managedCtClass );
	}

	private void addEntityInstanceHandling(CtClass managedCtClass) {
		try {
			MethodWriter.write(
					managedCtClass,
					"public Object %s() { return this; }",
					EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME
			);
		}
		catch (CannotCompileException cce) {
			throw new EnhancementException(
					String.format(
							Locale.ROOT,
							"Could not enhance entity class [%s] to add EntityEntry getter",
							managedCtClass.getName()
					),
					cce
			);
		}
	}

	private void addEntityEntryHandling(CtClass managedCtClass) {
		FieldWriter.addFieldWithGetterAndSetter(
				managedCtClass, loadCtClassFromClass( EntityEntry.class ),
				EnhancerConstants.ENTITY_ENTRY_FIELD_NAME,
				EnhancerConstants.ENTITY_ENTRY_GETTER_NAME,
				EnhancerConstants.ENTITY_ENTRY_SETTER_NAME
		);
	}

	private void addLinkedPreviousHandling(CtClass managedCtClass) {
		FieldWriter.addFieldWithGetterAndSetter(
				managedCtClass, loadCtClassFromClass( ManagedEntity.class ),
				EnhancerConstants.PREVIOUS_FIELD_NAME,
				EnhancerConstants.PREVIOUS_GETTER_NAME,
				EnhancerConstants.PREVIOUS_SETTER_NAME
		);
	}

	private void addLinkedNextHandling(CtClass managedCtClass) {
		FieldWriter.addFieldWithGetterAndSetter(
				managedCtClass, loadCtClassFromClass( ManagedEntity.class ),
				EnhancerConstants.NEXT_FIELD_NAME,
				EnhancerConstants.NEXT_GETTER_NAME,
				EnhancerConstants.NEXT_SETTER_NAME
		);
	}

	private void addInLineDirtyHandling(CtClass managedCtClass) {
		managedCtClass.addInterface( loadCtClassFromClass( SelfDirtinessTracker.class ) );

		FieldWriter.addField(
				managedCtClass,
				loadCtClassFromClass( DirtyTracker.class ),
				EnhancerConstants.TRACKER_FIELD_NAME
		);
		FieldWriter.addField(
				managedCtClass,
				loadCtClassFromClass( CollectionTracker.class ),
				EnhancerConstants.TRACKER_COLLECTION_NAME
		);

		createDirtyTrackerMethods( managedCtClass );
	}

	private void createDirtyTrackerMethods(CtClass managedCtClass) {
		try {
			MethodWriter.write(
					managedCtClass,
							"public void %1$s(String name) {%n" +
							"  if (%2$s == null) { %2$s = new %3$s(); }%n" +
							"  %2$s.add(name);%n" +
							"}",
					EnhancerConstants.TRACKER_CHANGER_NAME,
					EnhancerConstants.TRACKER_FIELD_NAME,
					DIRTY_TRACKER_IMPL
			);

			createCollectionDirtyCheckMethod( managedCtClass );
			createCollectionDirtyCheckGetFieldsMethod( managedCtClass );
			createClearDirtyCollectionMethod( managedCtClass );

			MethodWriter.write(
					managedCtClass,
							"public String[] %1$s() {%n" +
							"  if(%3$s == null) {%n" +
							"    return (%2$s == null) ? new String[0] : %2$s.get();%n" +
							"  } else {%n" +
							"    if (%2$s == null) %2$s = new %5$s();%n" +
							"    %4$s(%2$s);%n" +
							"    return %2$s.get();%n" +
							"  }%n" +
							"}",
					EnhancerConstants.TRACKER_GET_NAME,
					EnhancerConstants.TRACKER_FIELD_NAME,
					EnhancerConstants.TRACKER_COLLECTION_NAME,
					EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME,
					DIRTY_TRACKER_IMPL
			);

			MethodWriter.write(
					managedCtClass,
							"public boolean %1$s() {%n" +
							"  return (%2$s != null && !%2$s.isEmpty()) || %3$s();%n" +
							"}",
					EnhancerConstants.TRACKER_HAS_CHANGED_NAME,
					EnhancerConstants.TRACKER_FIELD_NAME,
					EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME
			);

			MethodWriter.write(
					managedCtClass,
							"public void %1$s() {%n" +
							"  if (%2$s != null) { %2$s.clear(); }%n" +
							"  %3$s();%n" +
							"}",
					EnhancerConstants.TRACKER_CLEAR_NAME,
					EnhancerConstants.TRACKER_FIELD_NAME,
					EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME
			);

			MethodWriter.write(
					managedCtClass,
							"public void %1$s(boolean f) {%n" +
							"  if (%2$s == null) %2$s = new %3$s();%n  %2$s.suspend(f);%n" +
							"}",
					EnhancerConstants.TRACKER_SUSPEND_NAME,
					EnhancerConstants.TRACKER_FIELD_NAME  ,
					DIRTY_TRACKER_IMPL
			);

			MethodWriter.write(
					managedCtClass,
							"public %s %s() { return %s; }",
					CollectionTracker.class.getName(),
					EnhancerConstants.TRACKER_COLLECTION_GET_NAME,
					EnhancerConstants.TRACKER_COLLECTION_NAME
			);
		}
		catch (CannotCompileException cce) {
			cce.printStackTrace();
		}
	}

	private List<CtField> collectCollectionFields(CtClass managedCtClass) {
		List<CtField> collectionList = new ArrayList<>();

		for ( CtField ctField : managedCtClass.getDeclaredFields() ) {
			// skip static fields and skip fields added by enhancement
			if ( Modifier.isStatic( ctField.getModifiers() ) || ctField.getName().startsWith( "$$_hibernate_" ) ) {
				continue;
			}
			if ( enhancementContext.isPersistentField( ctField ) ) {
				if ( PersistentAttributesHelper.isAssignable( ctField, Collection.class.getName() ) ||
						PersistentAttributesHelper.isAssignable( ctField, Map.class.getName() ) ) {
					collectionList.add( ctField );
				}
			}
		}

		// HHH-10646 Add fields inherited from @MappedSuperclass
		// HHH-10981 There is no need to do it for @MappedSuperclass
		if ( !enhancementContext.isMappedSuperclassClass( managedCtClass ) ) {
			collectionList.addAll( collectInheritCollectionFields( managedCtClass ) );
		}

		return collectionList;
	}

	private Collection<CtField> collectInheritCollectionFields(CtClass managedCtClass) {
		if ( managedCtClass == null || Object.class.getName().equals( managedCtClass.getName() ) ) {
			return Collections.emptyList();
		}
		try {
			CtClass managedCtSuperclass = managedCtClass.getSuperclass();

			if ( !enhancementContext.isMappedSuperclassClass( managedCtSuperclass ) ) {
				return collectInheritCollectionFields( managedCtSuperclass );
			}
			List<CtField> collectionList = new ArrayList<CtField>();

			for ( CtField ctField : managedCtSuperclass.getDeclaredFields() ) {
				if ( !Modifier.isStatic( ctField.getModifiers() ) && enhancementContext.isPersistentField( ctField ) ) {
					if ( PersistentAttributesHelper.isAssignable( ctField, Collection.class.getName() ) ||
							PersistentAttributesHelper.isAssignable( ctField, Map.class.getName() ) ) {
						collectionList.add( ctField );
					}
				}
			}
			collectionList.addAll( collectInheritCollectionFields( managedCtSuperclass ) );
			return collectionList;
		}
		catch ( NotFoundException nfe ) {
			return Collections.emptyList();
		}
	}

	private void createCollectionDirtyCheckMethod(CtClass managedCtClass) {
		try {
			final StringBuilder body = new StringBuilder();

			body.append(
					String.format(
									"private boolean %1$s() {%n" +
									"  if (%2$s == null) { return false; }%n%n",
							EnhancerConstants.TRACKER_COLLECTION_CHANGED_NAME,
							EnhancerConstants.TRACKER_COLLECTION_NAME
					)
			);

			for ( CtField ctField : collectCollectionFields( managedCtClass ) ) {
				if ( !enhancementContext.isMappedCollection( ctField ) ) {
					body.append(
							String.format(
											"  // collection field [%1$s]%n" +
											"  if (%1$s == null && %2$s.getSize(\"%1$s\") != -1) { return true; }%n" +
											"  if (%1$s != null && %2$s.getSize(\"%1$s\") != %1$s.size()) { return true; }%n%n",
									ctField.getName(),
									EnhancerConstants.TRACKER_COLLECTION_NAME
							)
					);
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

			body.append(
					String.format(
									"private void %1$s(%3$s tracker) {%n" +
									"  if (%2$s == null) { return; }%n%n",
							EnhancerConstants.TRACKER_COLLECTION_CHANGED_FIELD_NAME,
							EnhancerConstants.TRACKER_COLLECTION_NAME,
							DirtyTracker.class.getName()
					)
			);

			for ( CtField ctField : collectCollectionFields( managedCtClass ) ) {
				if ( !enhancementContext.isMappedCollection( ctField ) ) {
					body.append(
							String.format(
											"  // Collection field [%1$s]%n" +
											"  if (%1$s == null && %2$s.getSize(\"%1$s\") != -1) { tracker.add(\"%1$s\"); }%n" +
											"  if (%1$s != null && %2$s.getSize(\"%1$s\") != %1$s.size()) { tracker.add(\"%1$s\"); }%n%n",
									ctField.getName(),
									EnhancerConstants.TRACKER_COLLECTION_NAME
							)
					);
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

			body.append(
					String.format(
							"private void %1$s() {%n" +
									"  if (%2$s == null) { %2$s = new %3$s(); }%n" +
									"  %4$s lazyInterceptor = null;%n",
							EnhancerConstants.TRACKER_COLLECTION_CLEAR_NAME,
							EnhancerConstants.TRACKER_COLLECTION_NAME,
							COLLECTION_TRACKER_IMPL,
							LazyAttributeLoadingInterceptor.class.getName()
					)
			);

			if ( PersistentAttributesHelper.isAssignable( managedCtClass, PersistentAttributeInterceptable.class.getName() ) ) {
				body.append(
						String.format(
										"  if(%1$s != null && %1$s instanceof %2$s) lazyInterceptor = (%2$s) %1$s;%n%n",
								EnhancerConstants.INTERCEPTOR_FIELD_NAME,
								LazyAttributeLoadingInterceptor.class.getName()
						)
				);
			}

			for ( CtField ctField : collectCollectionFields( managedCtClass ) ) {
				if ( !enhancementContext.isMappedCollection( ctField ) ) {
					body.append(
							String.format(
										"  // collection field [%1$s]%n" +
										"  if (lazyInterceptor == null || lazyInterceptor.isAttributeLoaded(\"%1$s\")) {%n" +
										"    if (%1$s == null) { %2$s.add(\"%1$s\", -1); }%n" +
										"    else { %2$s.add(\"%1$s\", %1$s.size()); }%n" +
										"  }%n%n",
									ctField.getName(),
									EnhancerConstants.TRACKER_COLLECTION_NAME
							)
					);
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
