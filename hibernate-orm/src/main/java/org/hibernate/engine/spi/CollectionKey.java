/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * Uniquely identifies a collection instance in a particular session.
 *
 * @author Gavin King
 */
public final class CollectionKey implements Serializable {
	private final String role;
	private final Serializable key;
	private final Type keyType;
	private final SessionFactoryImplementor factory;
	private final int hashCode;
	private EntityMode entityMode;

	public CollectionKey(CollectionPersister persister, Serializable key) {
		this(
				persister.getRole(),
				key,
				persister.getKeyType(),
				persister.getOwnerEntityPersister().getEntityMetamodel().getEntityMode(),
				persister.getFactory()
		);
	}

	public CollectionKey(CollectionPersister persister, Serializable key, EntityMode em) {
		this( persister.getRole(), key, persister.getKeyType(), em, persister.getFactory() );
	}

	private CollectionKey(
			String role,
			Serializable key,
			Type keyType,
			EntityMode entityMode,
			SessionFactoryImplementor factory) {
		this.role = role;
		this.key = key;
		this.keyType = keyType;
		this.entityMode = entityMode;
		this.factory = factory;
		//cache the hash-code
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 17;
		result = 37 * result + role.hashCode();
		result = 37 * result + keyType.getHashCode( key, factory );
		return result;
	}


	public String getRole() {
		return role;
	}

	public Serializable getKey() {
		return key;
	}

	@Override
	public String toString() {
		return "CollectionKey"
				+ MessageHelper.collectionInfoString( factory.getCollectionPersister( role ), key, factory );
	}

	@Override
	public boolean equals(Object other) {
		if ( this == other ) {
			return true;
		}
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		final CollectionKey that = (CollectionKey) other;
		return that.role.equals( role )
				&& keyType.isEqual( that.key, key, factory );
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
	 * @throws java.io.IOException
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( role );
		oos.writeObject( key );
		oos.writeObject( keyType );
		oos.writeObject( entityMode.toString() );
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
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static CollectionKey deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		return new CollectionKey(
				(String) ois.readObject(),
				(Serializable) ois.readObject(),
				(Type) ois.readObject(),
				EntityMode.parse( (String) ois.readObject() ),
				(session == null ? null : session.getFactory())
		);
	}
}
