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
 * <p>
 * For temporal entities, use {@link TemporalEntityKey} which includes a changeset identifier
 * to isolate historical snapshots in the persistence context.
 *
 * @author Gavin King
 * @author Sanne Grinovero
 */
public sealed class EntityKey implements Serializable permits TemporalEntityKey {

	private final Object identifier;
	private final int hashCode;
	private final EntityPersister persister;

	/**
	 * Construct a unique identifier for an entity class instance.
	 * <p>
	 * For temporal (audit) contexts, prefer
	 * {@link SharedSessionContractImplementor#generateEntityKey} which
	 * automatically creates a {@link TemporalEntityKey} when operating
	 * in a temporal context.
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 */
	public EntityKey(@Nullable Object id, EntityPersister persister) {
		this( id, persister, 0 );
	}

	/**
	 * @param changesetIdHashCode hash code contribution from the changeset identifier
	 */
	EntityKey(@Nullable Object id, EntityPersister persister, int changesetIdHashCode) {
		this.persister = persister;
		if ( id == null ) {
			throw new AssertionFailure( "null identifier (" + persister.getEntityName() + ")" );
		}
		this.identifier = id;
		this.hashCode = generateHashCode( id, persister, changesetIdHashCode );
	}

	private static int generateHashCode(Object id, EntityPersister persister, int changesetIdHashCode) {
		int result = 17;
		final String rootEntityName = persister.getRootEntityName();
		result = 37 * result + rootEntityName.hashCode();
		final Type identifierType = persister.getIdentifierType().getTypeForEqualsHashCode();
		result = 37 * result + ( identifierType == null ? id.hashCode() : identifierType.getHashCode( id, persister.getFactory() ) );
		result = 37 * result + changesetIdHashCode;
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

	/**
	 * The audit changeset identifier for this key, or {@code null} for
	 * non-temporal entities.
	 * When non-null, this entity is a read-only historical snapshot.
	 */
	public @Nullable Object getChangesetId() {
		return null;
	}

	/**
	 * Whether this key refers to a temporal (historical) snapshot.
	 */
	public boolean isTemporal() {
		return false;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if ( this == other ) {
			return true;
		}
		if ( !(other instanceof EntityKey otherKey) ) {
			return false;
		}

		return samePersistentType( otherKey )
			&& sameIdentifier( otherKey )
			&& sameChangesetId( otherKey );

	}

	private boolean sameIdentifier(final EntityKey otherKey) {
		final Type identifierType;
		return this.identifier == otherKey.identifier || (
				(identifierType = persister.getIdentifierType().getTypeForEqualsHashCode()) == null && identifier.equals( otherKey.identifier )
						|| identifierType != null && identifierType.isEqual( otherKey.identifier, this.identifier, persister.getFactory() ) );
	}

	/**
	 * Compare changeset identifiers without virtual dispatch, using
	 * instanceof on the sealed hierarchy for optimal JIT performance.
	 */
	private boolean sameChangesetId(final EntityKey otherKey) {
		if ( this instanceof TemporalEntityKey t1 ) {
			return otherKey instanceof TemporalEntityKey t2
					&& t1.getChangesetId().equals( t2.getChangesetId() );
		}
		return !( otherKey instanceof TemporalEntityKey );
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
		oos.writeObject( getChangesetId() );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param sessionFactory The SessionFactory owning the Session being deserialized.
	 *
	 * @return The deserialized EntityKey
	 *
	 * @throws IOException Thrown by Java I/O
	 * @throws ClassNotFoundException Thrown by Java I/O
	 */
	public static EntityKey deserialize(ObjectInputStream ois, SessionFactoryImplementor sessionFactory) throws IOException, ClassNotFoundException {
		final Object id = ois.readObject();
		final String entityName = (String) ois.readObject();
		final Object changesetId = ois.readObject();
		final EntityPersister entityPersister =
				sessionFactory.getMappingMetamodel()
						.getEntityDescriptor( entityName );
		return changesetId != null
				? new TemporalEntityKey( id, entityPersister, changesetId )
				: new EntityKey( id, entityPersister );
	}
}
