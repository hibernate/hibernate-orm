/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

/**
 * Constants used during enhancement.
 *
 * @author Steve Ebersole
 */
public class EnhancerConstants {
	/**
	 * Prefix for persistent-field reader methods.
	 */
	public static final String PERSISTENT_FIELD_READER_PREFIX = "$$_hibernate_read_";

	/**
	 * Prefix for persistent-field writer methods.
	 */
	public static final String PERSISTENT_FIELD_WRITER_PREFIX = "$$_hibernate_write_";

	/**
	 * Name of the method used to get reference the the entity instance (this in the case of enhanced classes).
	 */
	public static final String ENTITY_INSTANCE_GETTER_NAME = "$$_hibernate_getEntityInstance";

	/**
	 * Name of the field used to hold the {@link org.hibernate.engine.spi.EntityEntry}
	 */
	public static final String ENTITY_ENTRY_FIELD_NAME = "$$_hibernate_entityEntryHolder";

	/**
	 * Name of the method used to read the {@link org.hibernate.engine.spi.EntityEntry} field.
	 *
	 * @see #ENTITY_ENTRY_FIELD_NAME
	 */
	public static final String ENTITY_ENTRY_GETTER_NAME = "$$_hibernate_getEntityEntry";

	/**
	 * Name of the method used to write the {@link org.hibernate.engine.spi.EntityEntry} field.
	 *
	 * @see #ENTITY_ENTRY_FIELD_NAME
	 */
	public static final String ENTITY_ENTRY_SETTER_NAME = "$$_hibernate_setEntityEntry";

	/**
	 * Name of the field used to hold the previous {@link org.hibernate.engine.spi.ManagedEntity}.
	 * <p/>
	 * Together, previous/next are used to define a "linked list"
	 *
	 * @see #NEXT_FIELD_NAME
	 */
	public static final String PREVIOUS_FIELD_NAME = "$$_hibernate_previousManagedEntity";

	/**
	 * Name of the method used to read the previous {@link org.hibernate.engine.spi.ManagedEntity} field
	 *
	 * @see #PREVIOUS_FIELD_NAME
	 */
	public static final String PREVIOUS_GETTER_NAME = "$$_hibernate_getPreviousManagedEntity";

	/**
	 * Name of the method used to write the previous {@link org.hibernate.engine.spi.ManagedEntity} field
	 *
	 * @see #PREVIOUS_FIELD_NAME
	 */
	public static final String PREVIOUS_SETTER_NAME = "$$_hibernate_setPreviousManagedEntity";

	/**
	 * Name of the field used to hold the previous {@link org.hibernate.engine.spi.ManagedEntity}.
	 * <p/>
	 * Together, previous/next are used to define a "linked list"
	 *
	 * @see #PREVIOUS_FIELD_NAME
	 */
	public static final String NEXT_FIELD_NAME = "$$_hibernate_nextManagedEntity";

	/**
	 * Name of the method used to read the next {@link org.hibernate.engine.spi.ManagedEntity} field
	 *
	 * @see #NEXT_FIELD_NAME
	 */
	public static final String NEXT_GETTER_NAME = "$$_hibernate_getNextManagedEntity";

	/**
	 * Name of the method used to write the next {@link org.hibernate.engine.spi.ManagedEntity} field
	 *
	 * @see #NEXT_FIELD_NAME
	 */
	public static final String NEXT_SETTER_NAME = "$$_hibernate_setNextManagedEntity";

	/**
	 * Name of the field used to store the {@link org.hibernate.engine.spi.PersistentAttributeInterceptable}.
	 */
	public static final String INTERCEPTOR_FIELD_NAME = "$$_hibernate_attributeInterceptor";

	/**
	 * Name of the method used to read the interceptor
	 *
	 * @see #INTERCEPTOR_FIELD_NAME
	 */
	public static final String INTERCEPTOR_GETTER_NAME = "$$_hibernate_getInterceptor";

	/**
	 * Name of the method used to write the interceptor
	 *
	 * @see #INTERCEPTOR_FIELD_NAME
	 */
	public static final String INTERCEPTOR_SETTER_NAME = "$$_hibernate_setInterceptor";

	/**
	 * Name of tracker field
	 */
	public static final String TRACKER_FIELD_NAME = "$$_hibernate_tracker";

	/**
	 * Name of method that add changed fields
	 */
	public static final String TRACKER_CHANGER_NAME = "$$_hibernate_trackChange";

	/**
	 * Name of method to see if any fields has changed
	 */
	public static final String TRACKER_HAS_CHANGED_NAME = "$$_hibernate_hasDirtyAttributes";

	/**
	 * Name of method to fetch dirty attributes
	 */
	public static final String TRACKER_GET_NAME = "$$_hibernate_getDirtyAttributes";

	/**
	 * Name of method to clear stored dirty attributes
	 */
	public static final String TRACKER_CLEAR_NAME = "$$_hibernate_clearDirtyAttributes";

	/**
	 * Name of method to check if collection fields are dirty
	 */
	public static final String TRACKER_COLLECTION_CHANGED_NAME = "$$_hibernate_areCollectionFieldsDirty";

	public static final String TRACKER_COLLECTION_NAME = "$$_hibernate_collectionTracker";
	/**
	 * Name of method to get dirty collection field names
	 */
	public static final String TRACKER_COLLECTION_CHANGED_FIELD_NAME = "$$_hibernate_getCollectionFieldDirtyNames";

	public static final String TRACKER_COLLECTION_CLEAR_NAME = "$$_hibernate_clearDirtyCollectionNames";

	public static final String TRACKER_COMPOSITE_DIRTY_CHECK = "$$_hibernate_areCompositeFieldsDirty";

	public static final String TRACKER_COMPOSITE_DIRTY_FIELDS_GETTER = "$$_hibernate_getCompositeDirtyFields";

	public static final String TRACKER_COMPOSITE_FIELD_NAME = "$$_hibernate_compositeOwners";

	public static final String TRACKER_COMPOSITE_SET_OWNER = "$$_hibernate_setOwner";

	public static final String TRACKER_COMPOSITE_CLEAR_OWNER = "$$_hibernate_clearOwner";

	private EnhancerConstants() {
	}
}
