/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Map;

import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.bytecode.enhance.internal.tracker.DirtyTracker;
import org.hibernate.bytecode.enhance.internal.tracker.NoopCollectionTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleCollectionTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleFieldTracker;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.ExtendedSelfDirtinessTracker;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.internal.util.collections.ArrayHelper;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.jar.asm.Opcodes;

import static org.hibernate.engine.internal.ManagedTypeHelper.asCompositeTracker;

class CodeTemplates {

	static class SetOwner {
		@Advice.OnMethodEnter
		static void $$_hibernate_setOwner(
				@Advice.Argument(0) String name,
				@Advice.Argument(1) CompositeOwner tracker,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME, readOnly = false) CompositeOwnerTracker $$_hibernate_compositeOwners) {
			if ( $$_hibernate_compositeOwners == null ) {
				$$_hibernate_compositeOwners = new CompositeOwnerTracker();
			}
			$$_hibernate_compositeOwners.add( name, tracker );
		}
	}

	static class ClearOwner {
		@Advice.OnMethodEnter
		static void $$_hibernate_setOwner(
				@Advice.Argument(0) String name,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME, readOnly = false) CompositeOwnerTracker $$_hibernate_compositeOwners) {
			if ( $$_hibernate_compositeOwners != null ) {
				$$_hibernate_compositeOwners.removeOwner( name );
			}
		}
	}

	static class TrackChange {
		@Advice.OnMethodEnter
		static void $$_hibernate_trackChange(
				@Advice.Argument(0) String name,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_FIELD_NAME, readOnly = false) DirtyTracker $$_hibernate_tracker) {
			if ( $$_hibernate_tracker == null ) {
				$$_hibernate_tracker = new SimpleFieldTracker();
			}
			$$_hibernate_tracker.add( name );
		}
	}

	static class GetDirtyAttributes {
		@Advice.OnMethodExit
		static void $$_hibernate_getDirtyAttributes(
				@Advice.This ExtendedSelfDirtinessTracker self,
				@Advice.Return(readOnly = false) String[] returned,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_FIELD_NAME, readOnly = false) DirtyTracker $$_hibernate_tracker,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_COLLECTION_NAME, readOnly = false) CollectionTracker $$_hibernate_collectionTracker) {
			if ( $$_hibernate_collectionTracker == null ) {
				returned = ( $$_hibernate_tracker == null ) ? ArrayHelper.EMPTY_STRING_ARRAY : $$_hibernate_tracker.get();
			}
			else {
				if ( $$_hibernate_tracker == null ) {
					$$_hibernate_tracker = new SimpleFieldTracker();
				}
				self.$$_hibernate_getCollectionFieldDirtyNames( $$_hibernate_tracker );
				returned = $$_hibernate_tracker.get();
			}
		}
	}

	static class GetDirtyAttributesWithoutCollections {
		@Advice.OnMethodExit
		static void $$_hibernate_getDirtyAttributes(
				@Advice.Return(readOnly = false) String[] returned,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_FIELD_NAME) DirtyTracker $$_hibernate_tracker) {
			returned = $$_hibernate_tracker == null ? ArrayHelper.EMPTY_STRING_ARRAY : $$_hibernate_tracker.get();
		}
	}

	static class GetCollectionTrackerWithoutCollections {
		@Advice.OnMethodExit
		static void $$_hibernate_getCollectionTracker( @Advice.Return(readOnly = false) CollectionTracker returned) {
			returned = NoopCollectionTracker.INSTANCE;
		}
	}

	static class AreFieldsDirty {
		@Advice.OnMethodExit
		static void $$_hibernate_hasDirtyAttributes(
				@Advice.This ExtendedSelfDirtinessTracker self,
				@Advice.Return(readOnly = false) boolean returned,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_FIELD_NAME, readOnly = false) DirtyTracker $$_hibernate_tracker) {
			returned = ( $$_hibernate_tracker != null && !$$_hibernate_tracker.isEmpty() ) || self.$$_hibernate_areCollectionFieldsDirty();
		}
	}

	static class AreFieldsDirtyWithoutCollections {
		@Advice.OnMethodExit
		static void $$_hibernate_hasDirtyAttributes(
				@Advice.Return(readOnly = false) boolean returned,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_FIELD_NAME) DirtyTracker $$_hibernate_tracker) {
			returned = $$_hibernate_tracker != null && !$$_hibernate_tracker.isEmpty();
		}
	}

	static class ClearDirtyAttributes {
		@Advice.OnMethodEnter
		static void $$_hibernate_clearDirtyAttributes(
				@Advice.This ExtendedSelfDirtinessTracker self,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_FIELD_NAME, readOnly = false) DirtyTracker $$_hibernate_tracker) {
			if ( $$_hibernate_tracker != null ) {
				$$_hibernate_tracker.clear();
			}
			self.$$_hibernate_clearDirtyCollectionNames();
		}
	}

	static class ClearDirtyAttributesWithoutCollections {
		@Advice.OnMethodEnter
		static void $$_hibernate_clearDirtyAttributes(
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_FIELD_NAME) DirtyTracker $$_hibernate_tracker) {
			if ( $$_hibernate_tracker != null ) {
				$$_hibernate_tracker.clear();
			}
		}
	}

	static class SuspendDirtyTracking {
		@Advice.OnMethodEnter
		static void $$_hibernate_suspendDirtyTracking(
				@Advice.Argument(0) boolean suspend,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_FIELD_NAME, readOnly = false) DirtyTracker $$_hibernate_tracker) {
			if ( $$_hibernate_tracker == null ) {
				$$_hibernate_tracker = new SimpleFieldTracker();
			}
			$$_hibernate_tracker.suspend( suspend );
		}
	}

	static class CollectionAreCollectionFieldsDirty {
		@Advice.OnMethodExit
		static void $$_hibernate_areCollectionFieldsDirty(
				@Advice.Return(readOnly = false) boolean returned,
				@FieldName String fieldName,
				@FieldValue Collection<?> collection,
				@Advice.FieldValue(EnhancerConstants.TRACKER_COLLECTION_NAME) CollectionTracker $$_hibernate_collectionTracker) {
			if ( !returned && $$_hibernate_collectionTracker != null ) {
				final int size = $$_hibernate_collectionTracker.getSize( fieldName );
				if ( collection == null && size != -1 ) {
					returned = true;
				}
				else if ( collection != null ) {
					// We only check sizes of non-persistent or initialized persistent collections
					if ( ( !( collection instanceof PersistentCollection ) || ( (PersistentCollection<?>) collection ).wasInitialized() )
							&& size != collection.size() ) {
						returned = true;
					}
				}
			}
		}
	}

	static class MapAreCollectionFieldsDirty {
		@Advice.OnMethodExit
		static void $$_hibernate_areCollectionFieldsDirty(
				@Advice.Return(readOnly = false) boolean returned,
				@FieldName String fieldName,
				@FieldValue Map<?, ?> map,
				@Advice.FieldValue(EnhancerConstants.TRACKER_COLLECTION_NAME) CollectionTracker $$_hibernate_collectionTracker) {
			if ( !returned && $$_hibernate_collectionTracker != null ) {
				final int size = $$_hibernate_collectionTracker.getSize( fieldName );
				if ( map == null && size != -1 ) {
					returned = true;
				}
				else if ( map != null ) {
					// We only check sizes of non-persistent or initialized persistent collections
					if ( ( !( map instanceof PersistentCollection ) || ( (PersistentCollection) map ).wasInitialized() )
							&& size != map.size() ) {
						returned = true;
					}
				}
			}
		}
	}

	static class CollectionGetCollectionFieldDirtyNames {
		@Advice.OnMethodExit
		static void $$_hibernate_areCollectionFieldsDirty(
				@FieldName String fieldName,
				@FieldValue Collection<?> collection,
				@Advice.Argument(0) DirtyTracker tracker,
				@Advice.FieldValue(EnhancerConstants.TRACKER_COLLECTION_NAME) CollectionTracker $$_hibernate_collectionTracker) {
			if ( $$_hibernate_collectionTracker != null ) {
				final int size = $$_hibernate_collectionTracker.getSize( fieldName );
				if ( collection == null && size != -1 ) {
					tracker.add( fieldName );
				}
				else if ( collection != null ) {
					// We only check sizes of non-persistent or initialized persistent collections
					if ( ( !( collection instanceof PersistentCollection ) || ( (PersistentCollection<?>) collection ).wasInitialized() )
							&& size != collection.size() ) {
						tracker.add( fieldName );
					}
				}
			}
		}
	}

	static class MapGetCollectionFieldDirtyNames {
		@Advice.OnMethodExit
		static void $$_hibernate_areCollectionFieldsDirty(
				@FieldName String fieldName,
				@FieldValue Map<?, ?> map,
				@Advice.Argument(0) DirtyTracker tracker,
				@Advice.FieldValue(EnhancerConstants.TRACKER_COLLECTION_NAME) CollectionTracker $$_hibernate_collectionTracker) {
			if ( $$_hibernate_collectionTracker != null ) {
				final int size = $$_hibernate_collectionTracker.getSize( fieldName );
				if ( map == null && size != -1 ) {
					tracker.add( fieldName );
				}
				else if ( map != null ) {
					// We only check sizes of non-persistent or initialized persistent collections
					if ( ( !( map instanceof PersistentCollection ) || ( (PersistentCollection<?>) map ).wasInitialized() )
							&& size != map.size() ) {
						tracker.add( fieldName );
					}
				}
			}
		}
	}

	static class CollectionGetCollectionClearDirtyNames {
		@Advice.OnMethodExit
		static void $$_hibernate_clearDirtyCollectionNames(
				@FieldName String fieldName,
				@FieldValue Collection<?> collection,
				@Advice.Argument(value = 0, readOnly = false) LazyAttributeLoadingInterceptor lazyInterceptor,
				@Advice.FieldValue(EnhancerConstants.TRACKER_COLLECTION_NAME) CollectionTracker $$_hibernate_collectionTracker) {
			if ( lazyInterceptor == null || lazyInterceptor.isAttributeLoaded( fieldName ) ) {
				if ( collection == null || collection instanceof PersistentCollection && !( (PersistentCollection<?>) collection ).wasInitialized() ) {
					$$_hibernate_collectionTracker.add( fieldName, -1 );
				}
				else {
					$$_hibernate_collectionTracker.add( fieldName, collection.size() );
				}
			}
		}
	}

	static class MapGetCollectionClearDirtyNames {
		@Advice.OnMethodExit
		static void $$_hibernate_clearDirtyCollectionNames(
				@FieldName String fieldName,
				@FieldValue Map<?, ?> map,
				@Advice.Argument(value = 0, readOnly = false) LazyAttributeLoadingInterceptor lazyInterceptor,
				@Advice.FieldValue(EnhancerConstants.TRACKER_COLLECTION_NAME) CollectionTracker $$_hibernate_collectionTracker) {
			if ( lazyInterceptor == null || lazyInterceptor.isAttributeLoaded( fieldName ) ) {
				if ( map == null || map instanceof PersistentCollection && !( (PersistentCollection<?>) map ).wasInitialized() ) {
					$$_hibernate_collectionTracker.add( fieldName, -1 );
				}
				else {
					$$_hibernate_collectionTracker.add( fieldName, map.size() );
				}
			}
		}
	}

	static class ClearDirtyCollectionNames {
		@Advice.OnMethodEnter
		static void $$_hibernate_clearDirtyCollectionNames(
				@Advice.This ExtendedSelfDirtinessTracker self,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_COLLECTION_NAME, readOnly = false) CollectionTracker $$_hibernate_collectionTracker) {
			if ( $$_hibernate_collectionTracker == null ) {
				$$_hibernate_collectionTracker = new SimpleCollectionTracker();
			}
			self.$$_hibernate_removeDirtyFields( null );
		}
	}

	static class InitializeLazyAttributeLoadingInterceptor {
		@Advice.OnMethodEnter
		static void $$_hibernate_removeDirtyFields(
				@Advice.Argument(value = 0, readOnly = false) LazyAttributeLoadingInterceptor lazyInterceptor,
				@Advice.FieldValue(value = EnhancerConstants.INTERCEPTOR_FIELD_NAME) PersistentAttributeInterceptor $$_hibernate_attributeInterceptor) {
			if ( $$_hibernate_attributeInterceptor instanceof LazyAttributeLoadingInterceptor ) {
				lazyInterceptor = (LazyAttributeLoadingInterceptor) $$_hibernate_attributeInterceptor;
			}
		}
	}

	static class CompositeFieldDirtyCheckingHandler {
		@Advice.OnMethodEnter
		static void enter(@FieldName String fieldName, @FieldValue Object field) {
			if ( field != null ) {
				asCompositeTracker( field ).$$_hibernate_clearOwner( fieldName );
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This CompositeOwner self, @FieldName String fieldName, @FieldValue Object field) {
			if ( field != null ) {
				asCompositeTracker( field ).$$_hibernate_setOwner( fieldName, self );
			}
			self.$$_hibernate_trackChange( fieldName );
		}
	}

	static class CompositeDirtyCheckingHandler {
		@Advice.OnMethodEnter
		static void enter(@Advice.FieldValue(EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME) CompositeOwnerTracker $$_hibernate_compositeOwners) {
			if ( $$_hibernate_compositeOwners != null ) {
				$$_hibernate_compositeOwners.callOwner( "" );
			}
		}
	}

	static class CompositeOwnerDirtyCheckingHandler {
		@Advice.OnMethodEnter
		static void $$_hibernate_trackChange(
				@Advice.Argument(0) String name,
				@Advice.FieldValue(EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME) CompositeOwnerTracker $$_hibernate_compositeOwners) {
			if ( $$_hibernate_compositeOwners != null ) {
				$$_hibernate_compositeOwners.callOwner( "." + name );
			}
		}
	}

	static class OneToOneHandler {
		@Advice.OnMethodEnter
		static void enter(@FieldValue Object field, @Advice.Argument(0) Object argument, @InverseSide boolean inverseSide) {
			if ( getterSelf() != null ) {
				// We copy the old value, then set the field to null which we must do before
				// unsetting the inverse attribute, as we'd otherwise run into a stack overflow situation
				// The field is writable, so setting it to null here is actually a field write.
				Object fieldCopy = field;
				field = null;
				setterNull( fieldCopy, null );
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Object argument, @InverseSide boolean inverseSide) {
			if ( argument != null && getter( argument ) != self ) {
				setterSelf( argument, self );
			}
		}

		static Object getter(Object target) {
			// is replaced by the actual method call
			throw new AssertionError();
		}

		static Object getterSelf() {
			// is replaced by the actual method call
			throw new AssertionError();
		}

		static void setterNull(Object target, Object argument) {
			// is replaced by the actual method call
			throw new AssertionError();
		}

		static void setterSelf(Object target, Object argument) {
			// is replaced by the actual method call
			throw new AssertionError();
		}
	}

	static class OneToManyOnCollectionHandler {
		@Advice.OnMethodEnter
		static void enter(@FieldValue Collection<?> field, @Advice.Argument(0) Collection<?> argument, @InverseSide boolean inverseSide) {
			if ( getterSelf() != null ) {
				Object[] array = field.toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( argument == null || !argument.contains( array[i] ) ) {
						setterNull( array[i], null );
					}
				}
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Collection<?> argument, @InverseSide boolean inverseSide) {
			if ( argument != null ) {
				Object[] array = argument.toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( getter( array[i] ) != self ) {
						setterSelf( array[i], self );
					}
				}
			}
		}

		static Object getter(Object target) {
			// is replaced by the actual method call
			throw new AssertionError();
		}

		static Object getterSelf() {
			// is replaced by the actual method call
			throw new AssertionError();
		}

		static void setterNull(Object target, Object argument) {
			// is replaced by the actual method call
			throw new AssertionError();
		}

		static void setterSelf(Object target, Object argument) {
			// is replaced by the actual method call
			throw new AssertionError();
		}
	}

	static class OneToManyOnMapHandler {
		@Advice.OnMethodEnter
		static void enter(@FieldValue Map<?, ?> field, @Advice.Argument(0) Map<?, ?> argument, @InverseSide boolean inverseSide) {
			if ( getterSelf() != null ) {
				Object[] array = field.values().toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( argument == null || !argument.containsValue( array[i] ) ) {
						setterNull( array[i], null );
					}
				}
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Map<?, ?> argument, @InverseSide boolean inverseSide) {
			if ( argument != null ) {
				Object[] array = argument.values().toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( getter( array[i] ) != self ) {
						setterSelf( array[i], self );
					}
				}
			}
		}

		static Object getter(Object target) {
			// is replaced with the actual getter call during instrumentation.
			throw new AssertionError();
		}

		static Object getterSelf() {
			// is replaced by the actual method call
			throw new AssertionError();
		}

		static void setterNull(Object target, Object argument) {
			// is replaced with the actual setter call during instrumentation.
			throw new AssertionError();
		}

		static void setterSelf(Object target, Object argument) {
			// is replaced with the actual setter call during instrumentation.
			throw new AssertionError();
		}
	}

	static class ManyToOneHandler {
		@Advice.OnMethodEnter
		static void enter(@Advice.This Object self, @FieldValue Object field, @BidirectionalAttribute String inverseAttribute) {
			if ( getterSelf() != null ) {
				Collection<?> c = getter( field );
				if ( c != null ) {
					if ( c instanceof PersistentCollection<?> ) {
						( (PersistentCollection) c ).queueRemoveOperation( self );
					}
					else {
						c.remove( self );
					}
				}
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Object argument, @BidirectionalAttribute String inverseAttribute) {
			if ( argument != null ) {
				Collection<Object> c = getter( argument );
				if ( c != null && c instanceof PersistentCollection<?> ) {
					( (PersistentCollection) c ).queueAddOperation( self );
				}
				else if ( c != null && !c.contains( self ) ) {
					c.add( self );
				}
			}
		}

		static Collection<Object> getter(Object target) {
			// is replaced by the actual method call
			throw new AssertionError();
		}

		static Object getterSelf() {
			// is replaced by the actual method call
			throw new AssertionError();
		}
	}

	static class ManyToManyHandler {
		@Advice.OnMethodEnter
		static void enter(@Advice.This Object self, @FieldValue Collection<?> field, @Advice.Argument(0) Collection<?> argument, @InverseSide boolean inverseSide, @BidirectionalAttribute String bidirectionalAttribute) {
			if ( getterSelf() != null ) {
				Object[] array = field.toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( argument == null || !argument.contains( array[i] ) ) {
						getter( array[i] ).remove( self );
					}
				}
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Collection<?> argument, @InverseSide boolean inverseSide, @BidirectionalAttribute String bidirectionalAttribute) {
			if ( argument != null ) {
				Object[] array = argument.toArray();
				for ( Object array1 : array ) {
					Collection<Object> c = getter( array1 );
					if ( c != null && !c.contains( self ) ) {
						c.add( self );
					}
				}
			}
		}

		static Collection<Object> getter(Object self) {
			// is replaced by the actual method call
			throw new AssertionError();
		}

		static Object getterSelf() {
			// is replaced by the actual method call
			throw new AssertionError();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface FieldName {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface FieldValue {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface InverseSide {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface BidirectionalAttribute {

	}

	// mapping to get private field from superclass by calling the enhanced reader, for use when field is not visible
	static class GetterMapping implements Advice.OffsetMapping {

		private final TypeDescription.Generic returnType;
		private final FieldDescription persistentField;

		GetterMapping(FieldDescription persistentField) {
			this( persistentField, persistentField.getType() );
		}

		GetterMapping(FieldDescription persistentField, TypeDescription.Generic returnType) {
			this.persistentField = persistentField;
			this.returnType = returnType;
		}

		@Override public Target resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod, Assigner assigner, Advice.ArgumentHandler argumentHandler, Sort sort) {
			MethodDescription.Token signature = new MethodDescription.Token( EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + persistentField.getName() , Opcodes.ACC_PUBLIC, returnType );
			MethodDescription method = new MethodDescription.Latent( persistentField.getDeclaringType().asErasure(), signature );

			return new Target.AbstractReadOnlyAdapter() {
				@Override
				public StackManipulation resolveRead() {
					return new StackManipulation.Compound( MethodVariableAccess.loadThis(), MethodInvocation.invoke( method ).special( method.getDeclaringType().asErasure() ) );
				}
			};
		}
	}
}
