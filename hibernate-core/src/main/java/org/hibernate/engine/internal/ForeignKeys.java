/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.TransientObjectException;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

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
		private final SessionImplementor session;
		private final Object self;

		/**
		 * Constructs a Nullifier
		 *
		 * @param self The entity
		 * @param isDelete Are we in the middle of a delete action?
		 * @param isEarlyInsert Is this an early insert (INSERT generated id strategy)?
		 * @param session The session
		 */
		public Nullifier(Object self, boolean isDelete, boolean isEarlyInsert, SessionImplementor session) {
			this.isDelete = isDelete;
			this.isEarlyInsert = isEarlyInsert;
			this.session = session;
			this.self = self;
		}

		/**
		 * Nullify all references to entities that have not yet been inserted in the database, where the foreign key
		 * points toward that entity.
		 *
		 * @param values The entity attribute values
		 * @param types The entity attribute types
		 */
		public void nullifyTransientReferences(final Object[] values, final Type[] types) {
			for ( int i = 0; i < types.length; i++ ) {
				values[i] = nullifyTransientReferences( values[i], types[i] );
			}
		}

		/**
		 * Return null if the argument is an "unsaved" entity (ie. one with no existing database row), or the
		 * input argument otherwise.  This is how Hibernate avoids foreign key constraint violations.
		 *
		 * @param value An entity attribute value
		 * @param type An entity attribute type
		 *
		 * @return {@code null} if the argument is an unsaved entity; otherwise return the argument.
		 */
		private Object nullifyTransientReferences(final Object value, final Type type) {
			if ( value == null ) {
				return null;
			}
			else if ( type.isEntityType() ) {
				final EntityType entityType = (EntityType) type;
				if ( entityType.isOneToOne() ) {
					return value;
				}
				else {
					final String entityName = entityType.getAssociatedEntityName();
					return isNullifiable( entityName, value ) ? null : value;
				}
			}
			else if ( type.isAnyType() ) {
				return isNullifiable( null, value ) ? null : value;
			}
			else if ( type.isComponentType() ) {
				final CompositeType actype = (CompositeType) type;
				final Object[] subvalues = actype.getPropertyValues( value, session );
				final Type[] subtypes = actype.getSubtypes();
				boolean substitute = false;
				for ( int i = 0; i < subvalues.length; i++ ) {
					final Object replacement = nullifyTransientReferences( subvalues[i], subtypes[i] );
					if ( replacement != subvalues[i] ) {
						substitute = true;
						subvalues[i] = replacement;
					}
				}
				if ( substitute ) {
					// todo : need to account for entity mode on the CompositeType interface :(
					actype.setPropertyValues( value, subvalues, EntityMode.POJO );
				}
				return value;
			}
			else {
				return value;
			}
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
			if ( object == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				// this is the best we can do...
				return false;
			}

			if ( object instanceof HibernateProxy ) {
				// if its an uninitialized proxy it can't be transient
				final LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
				if ( li.getImplementation( session ) == null ) {
					return false;
					// ie. we never have to null out a reference to
					// an uninitialized proxy
				}
				else {
					//unwrap it
					object = li.getImplementation();
				}
			}

			// if it was a reference to self, don't need to nullify
			// unless we are using native id generation, in which
			// case we definitely need to nullify
			if ( object == self ) {
				return isEarlyInsert
						|| ( isDelete && session.getFactory().getDialect().hasSelfReferentialForeignKeyBug() );
			}

			// See if the entity is already bound to this session, if not look at the
			// entity identifier and assume that the entity is persistent if the
			// id is not "unsaved" (that is, we rely on foreign keys to keep
			// database integrity)

			final EntityEntry entityEntry = session.getPersistenceContext().getEntry( object );
			if ( entityEntry == null ) {
				return isTransient( entityName, object, null, session );
			}
			else {
				return entityEntry.isNullifiable( isEarlyInsert, session );
			}

		}

	}

	/**
	 * Is this instance persistent or detached?
	 * <p/>
	 * If <tt>assumed</tt> is non-null, don't hit the database to make the determination, instead assume that
	 * value; the client code must be prepared to "recover" in the case that this assumed result is incorrect.
	 *
	 * @param entityName The name of the entity
	 * @param entity The entity instance
	 * @param assumed The assumed return value, if avoiding database hit is desired
	 * @param session The session
	 *
	 * @return {@code true} if the given entity is not transient (meaning it is either detached/persistent)
	 */
	@SuppressWarnings("SimplifiableIfStatement")
	public static boolean isNotTransient(String entityName, Object entity, Boolean assumed, SessionImplementor session) {
		if ( entity instanceof HibernateProxy ) {
			return true;
		}

		if ( session.getPersistenceContext().isEntryFor( entity ) ) {
			return true;
		}

		// todo : shouldnt assumed be revered here?

		return !isTransient( entityName, entity, assumed, session );
	}

	/**
	 * Is this instance, which we know is not persistent, actually transient?
	 * <p/>
	 * If <tt>assumed</tt> is non-null, don't hit the database to make the determination, instead assume that
	 * value; the client code must be prepared to "recover" in the case that this assumed result is incorrect.
	 *
	 * @param entityName The name of the entity
	 * @param entity The entity instance
	 * @param assumed The assumed return value, if avoiding database hit is desired
	 * @param session The session
	 *
	 * @return {@code true} if the given entity is transient (unsaved)
	 */
	public static boolean isTransient(String entityName, Object entity, Boolean assumed, SessionImplementor session) {
		if ( entity == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			// an unfetched association can only point to
			// an entity that already exists in the db
			return false;
		}

		// let the interceptor inspect the instance to decide
		Boolean isUnsaved = session.getInterceptor().isTransient( entity );
		if ( isUnsaved != null ) {
			return isUnsaved;
		}

		// let the persister inspect the instance to decide
		final EntityPersister persister = session.getEntityPersister( entityName, entity );
		isUnsaved = persister.isTransient( entity, session );
		if ( isUnsaved != null ) {
			return isUnsaved;
		}

		// we use the assumed value, if there is one, to avoid hitting
		// the database
		if ( assumed != null ) {
			return assumed;
		}

		// hit the database, after checking the session cache for a snapshot
		final Object[] snapshot = session.getPersistenceContext().getDatabaseSnapshot(
				persister.getIdentifier( entity, session ),
				persister
		);
		return snapshot == null;

	}

	/**
	 * Return the identifier of the persistent or transient object, or throw
	 * an exception if the instance is "unsaved"
	 * <p/>
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
	public static Serializable getEntityIdentifierIfNotUnsaved(
			final String entityName,
			final Object object,
			final SessionImplementor session) throws TransientObjectException {
		if ( object == null ) {
			return null;
		}
		else {
			Serializable id = session.getContextEntityIdentifier( object );
			if ( id == null ) {
				// context-entity-identifier returns null explicitly if the entity
				// is not associated with the persistence context; so make some
				// deeper checks...
				if ( isTransient( entityName, object, Boolean.FALSE, session ) ) {
					throw new TransientObjectException(
							"object references an unsaved transient instance - save the transient instance before flushing: " +
									(entityName == null ? session.guessEntityName( object ) : entityName)
					);
				}
				id = session.getEntityPersister( entityName, object ).getIdentifier( object, session );
			}
			return id;
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
			SessionImplementor session) {
		final Nullifier nullifier = new Nullifier( entity, false, isEarlyInsert, session );
		final EntityPersister persister = session.getEntityPersister( entityName, entity );
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
			SessionImplementor session,
			NonNullableTransientDependencies nonNullableTransientEntities) {
		if ( value == null ) {
			return;
		}

		if ( type.isEntityType() ) {
			final EntityType entityType = (EntityType) type;
			if ( !isNullable
					&& !entityType.isOneToOne()
					&& nullifier.isNullifiable( entityType.getAssociatedEntityName(), value ) ) {
				nonNullableTransientEntities.add( propertyName, value );
			}
		}
		else if ( type.isAnyType() ) {
			if ( !isNullable && nullifier.isNullifiable( null, value ) ) {
				nonNullableTransientEntities.add( propertyName, value );
			}
		}
		else if ( type.isComponentType() ) {
			final CompositeType actype = (CompositeType) type;
			final boolean[] subValueNullability = actype.getPropertyNullability();
			if ( subValueNullability != null ) {
				final String[] subPropertyNames = actype.getPropertyNames();
				final Object[] subvalues = actype.getPropertyValues( value, session );
				final Type[] subtypes = actype.getSubtypes();
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
