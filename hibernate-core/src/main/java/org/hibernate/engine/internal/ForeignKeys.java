/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.TransientObjectException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute.SingularAttributeClassification;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * Algorithms related to foreign key constraint transparency
 *
 * @author Gavin King
 */
@Internal
public final class ForeignKeys<T> {

	/**
	 * Delegate for handling nullifying ("null"ing-out) non-cascaded associations
	 */
	@Internal
	public static class Nullifier {
		private final boolean isDelete;
		private final boolean isEarlyInsert;
		private final SharedSessionContractImplementor session;
		private final Object self;

		/**
		 * Constructs a Nullifier
		 *
		 * @param self The entity
		 * @param isDelete Are we in the middle of a delete action?
		 * @param isEarlyInsert Is this an early insert (INSERT generated id strategy)?
		 * @param session The session
		 */
		public Nullifier(Object self, boolean isDelete, boolean isEarlyInsert, SharedSessionContractImplementor session) {
			this.isDelete = isDelete;
			this.isEarlyInsert = isEarlyInsert;
			this.session = session;
			this.self = self;
		}

		/**
		 * Nullify all references to entities that have not yet been inserted in the database, where the foreign key
		 * points toward that entity.
		 *  @param values The entity attribute values
		 * @param attributes The entity attributes */
		public void nullifyTransientReferences(
				final Object[] values,
				final List<NonIdPersistentAttribute<?, ?>> attributes) {
			int i = 0;
			for(NonIdPersistentAttribute attribute : attributes){
				values[i] = nullifyTransientReferences( values[i], attribute );
				i ++;
			}
		}

		/**
		 * Return null if the argument is an "unsaved" entity (ie. one with no existing database row), or the
		 * input argument otherwise.  This is how Hibernate avoids foreign key constraint violations.
		 *
		 * @param value An entity attribute value
		 * @param attribute An entity attribute
		 *
		 * @return {@code null} if the argument is an unsaved entity; otherwise return the argument.
		 */
		private Object nullifyTransientReferences(final Object value, final NonIdPersistentAttribute attribute) {
			if ( value == null ) {
				return null;
			}

			if ( attribute instanceof SingularPersistentAttribute ) {
				final SingularAttributeClassification attributeClassification =
						( (SingularPersistentAttribute) attribute ).getAttributeTypeClassification();
				if ( attributeClassification == SingularAttributeClassification.ONE_TO_ONE ) {
					return value;
				}
				else if ( attributeClassification == SingularAttributeClassification.MANY_TO_ONE ) {
					final SingularPersistentAttributeEntity singularPersistentAttribute = (SingularPersistentAttributeEntity) attribute;
					final String entityName = singularPersistentAttribute.getEntityName();
					return isNullifiable( entityName, value ) ? null : value;
				}
				else if ( attributeClassification == SingularAttributeClassification.ANY ) {
					return isNullifiable( null, value ) ? null : value;
				}
				else if ( attributeClassification == SingularAttributeClassification.EMBEDDED ) {
					final SingularPersistentAttributeEmbedded embedded = (SingularPersistentAttributeEmbedded) attribute;
					final EmbeddedTypeDescriptor<?> embeddedDescriptor = embedded.getEmbeddedDescriptor();

					final Map<String, Object> embeddedValues = new LinkedHashMap<>();
					boolean substitute = false;

					for ( NonIdPersistentAttribute<?, ?> subAttribute : embeddedDescriptor.getDeclaredPersistentAttributes() ) {
						final Object subAttributeValue = subAttribute.getPropertyAccess().getGetter().get( value );
						final Object replacement = nullifyTransientReferences( subAttributeValue, subAttribute );
						if ( replacement != subAttributeValue ) {
							// todo (6.0) : grrr.. this is another place we should not have to pass in SessionFactory
							//		the attribute AND the property-access are both scoped to the SF
							subAttribute.getPropertyAccess().getSetter().set( value, replacement, null );
						}
					}
					return value;
				}
			}
			return value;
		}

		/**
		 * Determine if the object already exists in the database,
		 * using a "best guess"
		 *
		 * @param entityName The name of the entity
		 * @param object The entity instance
		 */
		public boolean isNullifiable(String entityName, Object object) throws HibernateException {
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
					object = li.getImplementation( session );
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
	public static boolean isNotTransient(String entityName, Object entity, Boolean assumed, SharedSessionContractImplementor session) {
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
	public static boolean isTransient(String entityName, Object entity, Boolean assumed, SharedSessionContractImplementor session) {
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

		// let the descriptor inspect the instance to decide
		final EntityTypeDescriptor descriptor = session.getEntityDescriptor( entityName, entity );
		isUnsaved = descriptor.isTransient( entity, session );
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
				descriptor.getIdentifier( entity, session ),
				descriptor
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
	public static Object getEntityIdentifierIfNotUnsaved(
			final String entityName,
			final Object object,
			final SharedSessionContractImplementor session) throws TransientObjectException {
		if ( object == null ) {
			return null;
		}
		else {
			Object id = session.getContextEntityIdentifier( object );
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
				id = session.getEntityDescriptor( entityName, object ).getHierarchy()
						.getIdentifierDescriptor()
						.extractIdentifier( object, session );
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
	@SuppressWarnings("unchecked")
	public static NonNullableTransientDependencies findNonNullableTransientEntities(
			String entityName,
			Object entity,
			Object[] values,
			boolean isEarlyInsert,
			SharedSessionContractImplementor session) {
		if ( values == null ) {
			return null;
		}

		final NonNullableTransientDependencies nonNullableTransientEntities = new NonNullableTransientDependencies();
		final Nullifier nullifier = new Nullifier( entity, false, isEarlyInsert, session );
		final EntityTypeDescriptor descriptor = session.getEntityDescriptor( entityName, entity );

		// todo (6.0) : this is a good example of potential performance trade off - evaluate
//		specifically, because the method below *could be* (and partially is) a non-polymorphic call site
//		but moving to StateArrayContributor would be polymorphic
		descriptor.visitStateArrayContributors(
				stateArrayContributor -> {
					final Object value = values[ stateArrayContributor.getStateArrayPosition() ];
					if ( value == null ) {
						return;
					}

					stateArrayContributor.collectNonNullableTransientEntities(
							value,
							nullifier,
							nonNullableTransientEntities,
							session
					);
				}
		);

		return nonNullableTransientEntities.isEmpty() ? null : nonNullableTransientEntities;
	}

	/**
	 * Disallow instantiation
	 */
	private ForeignKeys() {
	}

}
