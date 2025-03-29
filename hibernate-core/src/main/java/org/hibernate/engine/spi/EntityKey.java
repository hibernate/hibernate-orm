/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Uniquely identifies of an entity instance in a particular Session by identifier.
 * Note that it's only safe to be used within the scope of a Session: it doesn't consider for example the tenantId
 * as part of the equality definition.
 * <p>
 * Information used to determine uniqueness consists of the entity-name and the identifier value (see {@link #equals}).
 * <p>
 * Performance considerations: lots of instances of this type are created at runtime. Make sure each one is as small as possible
 * by storing just the essential needed.
 *
 * @author Gavin King
 * @author Sanne Grinovero
 */
public final class EntityKey implements Serializable {

	private final Object identifier;
	private final int hashCode;
	private final EntityPersister persister;

	/**
	 * Construct a unique identifier for an entity class instance.
	 *
	 * @apiNote This signature has changed to accommodate both entity mode and multi-tenancy, both of which relate to
	 *          the session to which this key belongs. To help minimize the impact of these changes in the future, the
	 *          {@link SessionImplementor#generateEntityKey} method was added to hide the session-specific changes.
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 */
	public EntityKey(@Nullable Object id, EntityPersister persister) {
		this.persister = persister;
		if ( id == null ) {
			throw new AssertionFailure( "null identifier (" + persister.getEntityName() + ")" );
		}
		this.identifier = id;
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 17;
		final String rootEntityName = persister.getRootEntityName();
		result = 37 * result + rootEntityName.hashCode();
		final Type identifierType = persister.getIdentifierType().getTypeForEqualsHashCode();
		result = 37 * result + ( identifierType == null ? identifier.hashCode() : identifierType.getHashCode( identifier, persister.getFactory() ) );
		return result;
	}

	public boolean isBatchLoadable(LoadQueryInfluencers influencers) {
		return influencers.effectivelyBatchLoadable( persister );
	}

	public Object getIdentifierValue() {
		return identifier;
	}

	public Object getIdentifier() {
		return identifier;
	}

	public String getEntityName() {
		return persister.getEntityName();
	}

	public EntityPersister getPersister() {
		return persister;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if ( this == other ) {
			return true;
		}
		if ( other == null || EntityKey.class != other.getClass() ) {
			return false;
		}

		final EntityKey otherKey = (EntityKey) other;
		return samePersistentType( otherKey )
				&& sameIdentifier( otherKey );

	}

	private boolean sameIdentifier(final EntityKey otherKey) {
		final Type identifierType;
		return this.identifier == otherKey.identifier || (
				(identifierType = persister.getIdentifierType().getTypeForEqualsHashCode()) == null && identifier.equals( otherKey.identifier )
						|| identifierType != null && identifierType.isEqual( otherKey.identifier, this.identifier, persister.getFactory() ) );
	}

	private boolean samePersistentType(final EntityKey otherKey) {
		return otherKey.persister == persister
			|| otherKey.persister.getRootEntityName().equals( persister.getRootEntityName() );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "EntityKey" + infoString( this.persister, identifier, persister.getFactory() );
	}

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 *
	 * @throws IOException Thrown by Java I/O
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( identifier );
		oos.writeObject( persister.getEntityName() );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param sessionFactory The SessionFactory owning the Session being deserialized.
	 *
	 * @return The deserialized EntityEntry
	 *
	 * @throws IOException Thrown by Java I/O
	 * @throws ClassNotFoundException Thrown by Java I/O
	 */
	public static EntityKey deserialize(ObjectInputStream ois, SessionFactoryImplementor sessionFactory) throws IOException, ClassNotFoundException {
		final Object id = ois.readObject();
		final String entityName = (String) ois.readObject();
		final EntityPersister entityPersister =
				sessionFactory.getMappingMetamodel()
						.getEntityDescriptor( entityName);
		return new EntityKey( id, entityPersister );
	}
}
