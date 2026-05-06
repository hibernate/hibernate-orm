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
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * Uniquely identifies a collection instance in a particular session.
 * <p>
 * For temporal collections, use {@link TemporalCollectionKey} which includes a transaction identifier
 * to isolate historical snapshots in the persistence context.
 *
 * @author Gavin King
 */
public sealed class CollectionKey implements Serializable permits TemporalCollectionKey {
	private final String role;
	private final Object key;
	private final @Nullable Type keyType;
	private final SessionFactoryImplementor factory;
	private final int hashCode;

	public CollectionKey(CollectionPersister persister, Object key) {
		this(
				persister.getRole(),
				key,
				persister.getKeyType().getTypeForEqualsHashCode(),
				persister.getFactory(),
				0
		);
	}

	/**
	 * @param txIdHashCode hash code contribution from the transaction identifier
	 */
	CollectionKey(
			String role,
			@Nullable Object key,
			@Nullable Type keyType,
			SessionFactoryImplementor factory,
			int txIdHashCode) {
		this.role = role;
		if ( key == null ) {
			throw new AssertionFailure( "null identifier for collection of role (" + role + ")" );
		}
		this.key = key;
		this.keyType = keyType;
		this.factory = factory;
		//cache the hash-code
		this.hashCode = generateHashCode( role, key, keyType, factory, txIdHashCode );
	}

	private static int generateHashCode(
			String role,
			Object key,
			@Nullable Type keyType,
			SessionFactoryImplementor factory,
			int txIdHashCode) {
		int result = 17;
		result = 37 * result + role.hashCode();
		result = 37 * result + (keyType == null ? key.hashCode() : keyType.getHashCode( key, factory ));
		result = 37 * result + txIdHashCode;
		return result;
	}

	public String getRole() {
		return role;
	}

	public Object getKey() {
		return key;
	}

	/**
	 * The audit changeset identifier for this key, or {@code null} for
	 * non-temporal collections.
	 */
	public @Nullable Object getChangesetId() {
		return null;
	}

	/**
	 * Whether this key refers to a temporal (historical) collection snapshot.
	 */
	public boolean isTemporal() {
		return false;
	}

	@Override
	public String toString() {
		final CollectionPersister collectionDescriptor =
				factory.getMappingMetamodel()
						.getCollectionDescriptor( role );
		return "CollectionKey" + collectionInfoString( collectionDescriptor, key, factory );
	}

	@Override
	public boolean equals(final @Nullable Object other) {
		if ( this == other ) {
			return true;
		}
		if ( !(other instanceof CollectionKey that) ) {
			return false;
		}

		return that.role.equals( role )
			&& sameKey( that )
			&& sameChangesetId( that );
	}

	private boolean sameKey(final CollectionKey that) {
		return this.key == that.key
			|| (keyType == null ? this.key.equals( that.key ) : keyType.isEqual( this.key, that.key, factory ));
	}

	/**
	 * Compare transaction identifiers without virtual dispatch, using
	 * instanceof on the sealed hierarchy for optimal JIT performance.
	 */
	private boolean sameChangesetId(final CollectionKey otherKey) {
		if ( this instanceof TemporalCollectionKey t1 ) {
			return otherKey instanceof TemporalCollectionKey t2
				&& t1.getChangesetId().equals( t2.getChangesetId() );
		}
		return !(otherKey instanceof TemporalCollectionKey);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}


	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 *
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( role );
		oos.writeObject( key );
		oos.writeObject( keyType );
		oos.writeObject( getChangesetId() );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param session The session being deserialized.
	 *
	 * @return The deserialized CollectionKey
	 *
	 */
	public static CollectionKey deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		final String role = (String) ois.readObject();
		final Object key = ois.readObject();
		final Type keyType = (Type) ois.readObject();
		final Object txId = ois.readObject();
		final SessionFactoryImplementor factory = session.getFactory();
		return txId != null
				? new TemporalCollectionKey( role, key, keyType, factory, txId )
				: new CollectionKey( role, key, keyType, factory, 0 );
	}
}
