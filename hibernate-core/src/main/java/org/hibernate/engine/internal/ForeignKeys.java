/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.TransientObjectException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.AnyType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.engine.internal.ManagedTypeHelper.isHibernateProxy;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfSelfDirtinessTracker;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Algorithms related to foreign key constraint transparency
 *
 * @author Gavin King
 */
public final class ForeignKeys {

	/**
	 * Delegate for handling nullifying ("null"ing-out) non-cascaded associations
	 */
	public static class Nullifier {
		private final boolean isDelete;
		private final boolean isEarlyInsert;
		private final SharedSessionContractImplementor session;
		private final Object self;
		private final EntityPersister persister;

		/**
		 * Constructs a Nullifier
		 *
		 * @param self The entity
		 * @param isDelete Are we in the middle of a delete action?
		 * @param isEarlyInsert Is this an early insert (INSERT generated id strategy)?
		 * @param session The session
		 * @param persister The EntityPersister for {@code self}
		 */
		public Nullifier(
				final Object self,
				final boolean isDelete,
				final boolean isEarlyInsert,
				final SharedSessionContractImplementor session,
				final EntityPersister persister) {
			this.isDelete = isDelete;
			this.isEarlyInsert = isEarlyInsert;
			this.session = session;
			this.persister = persister;
			this.self = self;
		}

		/**
		 * Nullify all references to entities that have not yet been inserted in the database, where the foreign key
		 * points toward that entity.
		 *
		 * @param values The entity attribute values
		 */
		public void nullifyTransientReferences(final Object[] values) {
			final String[] propertyNames = persister.getPropertyNames();
			final Type[] types = persister.getPropertyTypes();
			for ( int i = 0; i < types.length; i++ ) {
				values[i] = nullifyTransientReferences( values[i], propertyNames[i], types[i] );
			}
		}

		/**
		 * Return null if the argument is an "unsaved" entity (ie. one with no existing database row), or the
		 * input argument otherwise.  This is how Hibernate avoids foreign key constraint violations.
		 *
		 * @param value An entity attribute value
		 * @param propertyName An entity attribute name
		 * @param type An entity attribute type
		 *
		 * @return {@code null} if the argument is an unsaved entity; otherwise return the argument.
		 */
		private Object nullifyTransientReferences(final Object value, final String propertyName, final Type type) {
			final Object returnedValue = nullifyTransient(value, propertyName, type);
			// value != returnedValue if either:
			// 1) returnedValue was nullified (set to null);
			// or 2) returnedValue was initialized, but not nullified.
			// When bytecode-enhancement is used for dirty-checking, the change should
			// only be tracked when returnedValue was nullified (1).
			if ( value != returnedValue && returnedValue == null ) {
				processIfSelfDirtinessTracker( self, SelfDirtinessTracker::$$_hibernate_trackChange, propertyName );
			}
			return returnedValue;
		}

		private Object nullifyTransient(Object value, String propertyName, Type type) {
			if ( value == null ) {
				return null;
			}
			else if ( type instanceof EntityType ) {
				return nullifyEntityType( value, propertyName, (EntityType) type );
			}
			else if ( type instanceof AnyType ) {
				return isNullifiable( null, value) ? null : value;
			}
			else if ( type instanceof ComponentType ) {
				return nullifyCompositeType( value, propertyName, (ComponentType) type );
			}
			else {
				return value;
			}
		}

		private Object nullifyCompositeType(Object value, String propertyName, ComponentType compositeType) {
			final Object[] subvalues = compositeType.getPropertyValues(value, session );
			final Type[] subtypes = compositeType.getSubtypes();
			final String[] subPropertyNames = compositeType.getPropertyNames();
			boolean substitute = false;
			for ( int i = 0; i < subvalues.length; i++ ) {
				final Object replacement = nullifyTransientReferences(
						subvalues[i],
						qualify( propertyName, subPropertyNames[i] ),
						subtypes[i]
				);
				if ( replacement != subvalues[i] ) {
					substitute = true;
					subvalues[i] = replacement;
				}
			}
			if ( substitute ) {
				// todo : need to account for entity mode on the CompositeType interface :(
				compositeType.setPropertyValues( value, subvalues );
			}
			return value;
		}

		private Object nullifyEntityType(Object value, String propertyName, EntityType entityType) {
			if ( entityType.isOneToOne() ) {
				return value;
			}
			else {
				// If value is lazy, it may need to be initialized to
				// determine if the value is nullifiable.
				final Object possiblyInitializedValue = initializeIfNecessary(value, propertyName, entityType );
				if ( possiblyInitializedValue == null ) {
					// The uninitialized value was initialized to null
					return null;
				}
				else {
					// If the value is not nullifiable, make sure that the
					// possibly initialized value is returned.
					return isNullifiable(entityType.getAssociatedEntityName(), possiblyInitializedValue)
							? null
							: possiblyInitializedValue;
				}
			}
		}

		private Object initializeIfNecessary(final Object value, final String propertyName, final Type type) {
			if ( initializationIsNecessary( value,  type ) ) {
				// IMPLEMENTATION NOTE: If cascade-remove was mapped for the attribute,
				// then value should have been initialized previously, when the remove operation was
				// cascaded to the property (because CascadingAction.DELETE.performOnLazyProperty()
				// returns true). This particular situation can only arise when cascade-remove is not
				// mapped for the association.

				// There is at least one nullifiable entity. We don't know if the lazy
				// associated entity is one of the nullifiable entities. If it is, and
				// the property is not nullified, then a constraint violation will result.
				// The only way to find out if the associated entity is nullifiable is
				// to initialize it.
				return ( (LazyPropertyInitializer) persister )
						.initializeLazyProperty( propertyName, self, session );
			}
			else {
				return value;
			}
		}

		private boolean initializationIsNecessary(Object value, Type type) {
			// TODO: there may be ways to fine-tune when initialization is necessary
			//       (e.g., only initialize when the associated entity type is a
			//       superclass or the same as the entity type of a nullifiable entity).
			//       It is unclear if a more complicated check would impact performance
			//       more than just initializing the associated entity.
			return isDelete
				&& value == UNFETCHED_PROPERTY
				&& type instanceof EntityType
				&& !session.getPersistenceContextInternal().isNullifiableEntityKeysEmpty();
		}

		/**
		 * Determine if the object already exists in the database,
		 * using a "best guess"
		 *
		 * @param entityName The name of the entity
		 * @param object The entity instance
		 */
		private boolean isNullifiable(final String entityName, Object object)
				throws HibernateException {
			if ( object == UNFETCHED_PROPERTY ) {
				// this is the best we can do...
				return false;
			}

			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			final LazyInitializer lazyInitializer = extractLazyInitializer( object );
			if ( lazyInitializer != null ) {
				// if it's an uninitialized proxy it can only be
				// transient if we did an unloaded-delete on the
				// proxy itself, in which case there is no entry
				// for it, but its key has already been registered
				// as nullifiable
				final Object entity = lazyInitializer.getImplementation( session );
				if ( entity == null ) {
					// an unloaded proxy might be scheduled for deletion
					return persistenceContext.containsDeletedUnloadedEntityKey(
							session.generateEntityKey(
									lazyInitializer.getInternalIdentifier(),
									session.getFactory().getMappingMetamodel()
											.getEntityDescriptor( lazyInitializer.getEntityName() )
							)
					);
				}
				else {
					//unwrap it
					object = entity;
				}
			}

			// if it was a reference to self, don't need to nullify
			// unless we are using native id generation, in which
			// case we definitely need to nullify
			if ( object == self ) {
				return isEarlyInsert
					|| isDelete && hasSelfReferentialForeignKeyBug();
			}

			// See if the entity is already bound to this session, if not look at the
			// entity identifier and assume that the entity is persistent if the
			// id is not "unsaved" (that is, we rely on foreign keys to keep
			// database integrity)

			final EntityEntry entityEntry = persistenceContext.getEntry( object );
			return entityEntry == null
					? isTransient( entityName, object, null, session )
					: entityEntry.isNullifiable( isEarlyInsert, session );
		}

		private boolean hasSelfReferentialForeignKeyBug() {
			return session.getFactory().getJdbcServices().getDialect()
					.hasSelfReferentialForeignKeyBug();
		}
	}

	/**
	 * Is this instance persistent or detached?
	 * <p>
	 * If {@code assumed} is non-null, don't hit the database to make the determination, instead assume that
	 * value; the client code must be prepared to "recover" in the case that this assumed result is incorrect.
	 *
	 * @param entityName The name of the entity
	 * @param entity The entity instance
	 * @param assumed The assumed return value, if avoiding database hit is desired
	 * @param session The session
	 *
	 * @return {@code true} if the given entity is not transient (meaning it is either detached/persistent)
	 */
	public static boolean isNotTransient(
			String entityName, Object entity, Boolean assumed, SharedSessionContractImplementor session) {
		return isHibernateProxy( entity )
			|| session.getPersistenceContextInternal().isEntryFor( entity )
			// todo : shouldn't assumed be reversed here?
			|| !isTransient( entityName, entity, assumed, session );
	}

	/**
	 * Is this instance, which we know is not persistent, actually transient?
	 * <p>
	 * If {@code assumed} is non-null, don't hit the database to make the determination, instead assume that
	 * value; the client code must be prepared to "recover" in the case that this assumed result is incorrect.
	 *
	 * @param entityName The name of the entity
	 * @param entity The entity instance
	 * @param assumed The assumed return value, if avoiding database hit is desired
	 * @param session The session
	 *
	 * @return {@code true} if the given entity is transient (unsaved)
	 */
	public static boolean isTransient(
			String entityName, Object entity, @Nullable Boolean assumed, SharedSessionContractImplementor session) {
		if ( entity == UNFETCHED_PROPERTY ) {
			// an unfetched association can only point to
			// an entity that already exists in the db
			return false;
		}

		// let the interceptor inspect the instance to decide
		final Boolean isUnsavedAccordingToInterceptor = session.getInterceptor().isTransient( entity );
		if ( isUnsavedAccordingToInterceptor != null ) {
			return isUnsavedAccordingToInterceptor;
		}

		// let the persister inspect the instance to decide
		final EntityPersister persister = session.getEntityPersister( entityName, entity );
		final Boolean isUnsavedAccordingToPersister = persister.isTransient( entity, session );
		if ( isUnsavedAccordingToPersister != null ) {
			return isUnsavedAccordingToPersister;
		}

		// we use the assumed value, if there is one, to
		// avoid hitting the database
		if ( assumed != null ) {
			return assumed;
		}

		// hit the database, after checking the session
		// cache for a snapshot
		final Object[] snapshot =
				session.getPersistenceContextInternal()
						.getDatabaseSnapshot( persister.getIdentifier( entity, session ), persister );
		return snapshot == null;
	}

	/**
	 * Return the identifier of the persistent or transient object, or throw
	 * an exception if the instance is "unsaved"
	 * <p>
	 * Used by OneToOneType and ManyToOneType to determine what id value should
	 * be used for an object that may or may not be associated with the session.
	 * This does a "best guess" using any/all info available to use (not just the
	 * EntityEntry).
	 *
	 * @param entityName The name of the entity
	 * @param object The entity instance
	 * @param session The session
	 *
	 * @return The identifier
	 *
	 * @throws TransientObjectException if the entity is transient (does not yet have an identifier)
	 */
	public static Object getEntityIdentifierIfNotUnsaved(
			final String entityName,
			final Object object,
			final SharedSessionContractImplementor session) throws TransientObjectException {
		if ( object == null ) {
			return null;
		}
		else {
			final Object id = session.getContextEntityIdentifier( object );
			if ( id == null ) {
				// context-entity-identifier returns null explicitly if the entity
				// is not associated with the persistence context; so make some
				// deeper checks...
				throwIfTransient( entityName, object, session );
				return session.getEntityPersister( entityName, object ).getIdentifier( object, session );
			}
			else {
				return id;
			}
		}
	}

	public static Object getEntityIdentifier(
			final String entityName,
			final Object object,
			final SharedSessionContractImplementor session) {
		if ( object == null ) {
			return null;
		}
		else {
			final Object id = session.getContextEntityIdentifier( object );
			return id == null
					? session.getEntityPersister( entityName, object ).getIdentifier( object, session )
					: id;
		}
	}

	private static void throwIfTransient(String entityName, Object object, SharedSessionContractImplementor session) {
		if ( isTransient( entityName, object, Boolean.FALSE, session ) ) {
			throw new TransientObjectException(
					"object references an unsaved transient instance - save the transient instance before flushing: " +
							(entityName == null ? session.guessEntityName(object) : entityName)
			);
		}
	}

	/**
	 * Find all non-nullable references to entities that have not yet
	 * been inserted in the database, where the foreign key
	 * is a reference to an unsaved transient entity. .
	 *
	 * @param entityName - the entity name
	 * @param entity - the entity instance
	 * @param values - insertable properties of the object (including backrefs),
	 * possibly with substitutions
	 * @param isEarlyInsert - true if the entity needs to be executed as soon as possible
	 * (e.g., to generate an ID)
	 * @param session - the session
	 *
	 * @return the transient unsaved entity dependencies that are non-nullable,
	 *         or null if there are none.
	 */
	public static NonNullableTransientDependencies findNonNullableTransientEntities(
			String entityName,
			Object entity,
			Object[] values,
			boolean isEarlyInsert,
			SharedSessionContractImplementor session) {
		final EntityPersister persister = session.getEntityPersister( entityName, entity );
		final Nullifier nullifier = new Nullifier( entity, false, isEarlyInsert, session, persister );
		final String[] propertyNames = persister.getPropertyNames();
		final Type[] types = persister.getPropertyTypes();
		final boolean[] nullability = persister.getPropertyNullability();
		final NonNullableTransientDependencies nonNullableTransientEntities = new NonNullableTransientDependencies();
		for ( int i = 0; i < types.length; i++ ) {
			collectNonNullableTransientEntities(
					nullifier,
					values[i],
					propertyNames[i],
					types[i],
					nullability[i],
					session,
					nonNullableTransientEntities
			);
		}
		return nonNullableTransientEntities.isEmpty() ? null : nonNullableTransientEntities;
	}

	private static void collectNonNullableTransientEntities(
			Nullifier nullifier,
			Object value,
			String propertyName,
			Type type,
			boolean isNullable,
			SharedSessionContractImplementor session,
			NonNullableTransientDependencies nonNullableTransientEntities) {
		if ( value == null ) {
			// do nothing
		}
		else if ( type instanceof EntityType ) {
			final EntityType entityType = (EntityType) type;
			if ( !isNullable
					&& !entityType.isOneToOne()
					&& nullifier.isNullifiable( entityType.getAssociatedEntityName(), value ) ) {
				nonNullableTransientEntities.add( propertyName, value );
			}
		}
		else if ( type instanceof AnyType ) {
			if ( !isNullable && nullifier.isNullifiable( null, value ) ) {
				nonNullableTransientEntities.add( propertyName, value );
			}
		}
		else if ( type instanceof ComponentType ) {
			final ComponentType compositeType = (ComponentType) type;
			final boolean[] subValueNullability = compositeType.getPropertyNullability();
			if ( subValueNullability != null ) {
				final String[] subPropertyNames = compositeType.getPropertyNames();
				final Object[] subvalues = compositeType.getPropertyValues( value, session );
				final Type[] subtypes = compositeType.getSubtypes();
				for ( int j = 0; j < subvalues.length; j++ ) {
					collectNonNullableTransientEntities(
							nullifier,
							subvalues[j],
							subPropertyNames[j],
							subtypes[j],
							subValueNullability[j],
							session,
							nonNullableTransientEntities
					);
				}
			}
		}
	}

	/**
	 * Disallow instantiation
	 */
	private ForeignKeys() {
	}

}
