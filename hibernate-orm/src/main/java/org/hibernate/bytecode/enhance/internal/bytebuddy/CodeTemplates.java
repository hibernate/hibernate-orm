/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.bytecode.enhance.internal.tracker.DirtyTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleCollectionTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleFieldTracker;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.spi.ExtendedSelfDirtinessTracker;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;

import net.bytebuddy.asm.Advice;

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
				returned = ( $$_hibernate_tracker == null ) ? new String[0] : $$_hibernate_tracker.get();
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

	static class AreCollectionFieldsDirty {
		@Advice.OnMethodExit
		static void $$_hibernate_hasDirtyAttributes(
				@Advice.This ExtendedSelfDirtinessTracker self,
				@Advice.Return(readOnly = false) boolean returned,
				@Advice.FieldValue(value = EnhancerConstants.TRACKER_FIELD_NAME, readOnly = false) DirtyTracker $$_hibernate_tracker) {
			returned = ( $$_hibernate_tracker != null && !$$_hibernate_tracker.isEmpty() ) || self.$$_hibernate_areCollectionFieldsDirty();
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
				if ( collection == null && $$_hibernate_collectionTracker.getSize( fieldName ) != -1 ) {
					returned = true;
				}
				else if ( collection != null && $$_hibernate_collectionTracker.getSize( fieldName ) != collection.size() ) {
					returned = true;
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
				if ( map == null && $$_hibernate_collectionTracker.getSize( fieldName ) != -1 ) {
					returned = true;
				}
				else if ( map != null && $$_hibernate_collectionTracker.getSize( fieldName ) != map.size() ) {
					returned = true;
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
				if ( collection == null && $$_hibernate_collectionTracker.getSize( fieldName ) != -1 ) {
					tracker.add( fieldName );
				}
				else if ( collection != null && $$_hibernate_collectionTracker.getSize( fieldName ) != collection.size() ) {
					tracker.add( fieldName );
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
				if ( map == null && $$_hibernate_collectionTracker.getSize( fieldName ) != -1 ) {
					tracker.add( fieldName );
				}
				else if ( map != null && $$_hibernate_collectionTracker.getSize( fieldName ) != map.size() ) {
					tracker.add( fieldName );
				}
			}
		}
	}

	static class CollectionGetCollectionClearDirtyNames {
		@Advice.OnMethodExit
		static void $$_hibernate_clearDirtyCollectionNames(
				@FieldName String fieldName,
				@FieldValue Collection<?> collection,
				@Advice.Argument(0) LazyAttributeLoadingInterceptor lazyInterceptor,
				@Advice.FieldValue(EnhancerConstants.TRACKER_COLLECTION_NAME) CollectionTracker $$_hibernate_collectionTracker) {
			if ( lazyInterceptor == null || lazyInterceptor.isAttributeLoaded( fieldName ) ) {
				if ( collection == null ) {
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
				@Advice.Argument(0) LazyAttributeLoadingInterceptor lazyInterceptor,
				@Advice.FieldValue(EnhancerConstants.TRACKER_COLLECTION_NAME) CollectionTracker $$_hibernate_collectionTracker) {
			if ( lazyInterceptor == null || lazyInterceptor.isAttributeLoaded( fieldName ) ) {
				if ( map == null ) {
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
				@Advice.FieldValue(EnhancerConstants.TRACKER_COLLECTION_NAME) Object $$_hibernate_attributeInterceptor) {
			if ( $$_hibernate_attributeInterceptor instanceof LazyAttributeLoadingInterceptor ) {
				lazyInterceptor = (LazyAttributeLoadingInterceptor) $$_hibernate_attributeInterceptor;
			}
		}
	}

	static class CompositeFieldDirtyCheckingHandler {
		@Advice.OnMethodEnter
		static void enter(@FieldName String fieldName, @FieldValue Object field) {
			if ( field != null ) {
				( (CompositeTracker) field ).$$_hibernate_clearOwner( fieldName );
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This CompositeOwner self, @FieldName String fieldName, @FieldValue Object field) {
			if ( field != null ) {
				( (CompositeTracker) field ).$$_hibernate_setOwner( fieldName, self );
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
		static void enter(@FieldValue Object field, @Advice.Argument(0) Object argument, @MappedBy String mappedBy) {
			if ( field != null && Hibernate.isPropertyInitialized( field, mappedBy ) && argument != null ) {
				setterNull( field, null );
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Object argument, @MappedBy String mappedBy) {
			if ( argument != null && Hibernate.isPropertyInitialized( argument, mappedBy ) && getter( argument ) != self ) {
				setterSelf( argument, self );
			}
		}

		static Object getter(Object target) {
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
		static void enter(@FieldValue Collection<?> field, @Advice.Argument(0) Collection<?> argument, @MappedBy String mappedBy) {
			if ( field != null && Hibernate.isPropertyInitialized( field, mappedBy ) ) {
				Object[] array = field.toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( argument == null || !argument.contains( array[i] ) ) {
						setterNull( array[i], null );
					}
				}
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Collection<?> argument, @MappedBy String mappedBy) {
			if ( argument != null && Hibernate.isPropertyInitialized( argument, mappedBy ) ) {
				Object[] array = argument.toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( Hibernate.isPropertyInitialized( array[i], mappedBy ) && getter( array[i] ) != self ) {
						setterSelf( array[i], self );
					}
				}
			}
		}

		static Object getter(Object target) {
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
		static void enter(@FieldValue Map<?, ?> field, @Advice.Argument(0) Map<?, ?> argument, @MappedBy String mappedBy) {
			if ( field != null && Hibernate.isPropertyInitialized( field, mappedBy ) ) {
				Object[] array = field.values().toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( argument == null || !argument.values().contains( array[i] ) ) {
						setterNull( array[i], null );
					}
				}
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Map<?, ?> argument, @MappedBy String mappedBy) {
			if ( argument != null && Hibernate.isPropertyInitialized( argument, mappedBy ) ) {
				Object[] array = argument.values().toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( Hibernate.isPropertyInitialized( array[i], mappedBy ) && getter( array[i] ) != self ) {
						setterSelf( array[i], self );
					}
				}
			}
		}

		static Object getter(Object target) {
			// is replaced with the actual getter call during instrumentation.
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
		static void enter(@Advice.This Object self, @FieldValue Object field, @MappedBy String mappedBy) {
			if ( field != null && Hibernate.isPropertyInitialized( field, mappedBy ) ) {
				Collection<?> c = getter( field );
				if ( c != null ) {
					c.remove( self );
				}
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Object argument, @MappedBy String mappedBy) {
			if ( argument != null && Hibernate.isPropertyInitialized( argument, mappedBy ) ) {
				Collection<Object> c = getter( argument );
				if ( c != null && !c.contains( self ) ) {
					c.add( self );
				}
			}
		}

		static Collection<Object> getter(Object target) {
			// is replaced by the actual method call
			throw new AssertionError();
		}
	}

	static class ManyToManyHandler {
		@Advice.OnMethodEnter
		static void enter(@Advice.This Object self, @FieldValue Collection<?> field, @Advice.Argument(0) Collection<?> argument, @MappedBy String mappedBy) {
			if ( field != null && Hibernate.isPropertyInitialized( field, mappedBy ) ) {
				Object[] array = field.toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( argument == null || !argument.contains( array[i] ) ) {
						getter( array[i] ).remove( self );
					}
				}
			}
		}

		@Advice.OnMethodExit
		static void exit(@Advice.This Object self, @Advice.Argument(0) Collection<?> argument, @MappedBy String mappedBy) {
			if ( argument != null && Hibernate.isPropertyInitialized( argument, mappedBy ) ) {
				Object[] array = argument.toArray();
				for ( int i = 0; i < array.length; i++ ) {
					if ( Hibernate.isPropertyInitialized( array[i], mappedBy ) ) {
						Collection<Object> c = getter( array[i] );
						if ( c != self && c != null ) {
							c.add( self );
						}
					}
				}
			}
		}

		static Collection<Object> getter(Object self) {
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
	@interface MappedBy {

	}
}
