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
import java.util.Objects;

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
	private final String tenantIdentifier;

	public CollectionKey(CollectionPersister persister, Serializable key, String tenantIdentifier) {
		this(
				persister.getRole(),
				key,
				persister.getKeyType(),
				persister.getFactory(), tenantIdentifier );
	}

	/**
	 * The EntityMode parameter is now ignored. Use the other constructor.
	 * 
	 * @deprecated Use {@link #CollectionKey(CollectionPersister, Serializable)}
	 */
	@Deprecated
	public CollectionKey(CollectionPersister persister, Serializable key, EntityMode em) {
		this( persister.getRole(), key, persister.getKeyType(), persister.getFactory(), null );
	}

	private CollectionKey(
			String role,
			Serializable key,
			Type keyType,
			SessionFactoryImplementor factory,
			String tenantIdentifier) {
		this.role = role;
		this.key = key;
		this.keyType = keyType;
		this.factory = factory;
		this.tenantIdentifier = tenantIdentifier;
		// cache the hash-code
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 17;
		result = 37 * result + role.hashCode();
		result = 37 * result + keyType.getHashCode( key, factory );
		result = 37 * result + Objects.hashCode( tenantIdentifier );
		return result;
	}

	public String getRole() {
		return role;
	}

	public Serializable getKey() {
		return key;
	}

	public String getTenantIdentifier() {
		return this.tenantIdentifier;
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
				&& keyType.isEqual( that.key, key, factory ) && Objects.equals( this.tenantIdentifier, that.tenantIdentifier );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Custom serialization routine used during serialization of a Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 * @throws java.io.IOException
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( role );
		oos.writeObject( key );
		oos.writeObject( keyType );
		oos.writeObject( tenantIdentifier );
	}

	/**
	 * Custom deserialization routine used during deserialization of a Session/PersistenceContext for increased
	 * performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param session The session being deserialized.
	 * @return The deserialized CollectionKey
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
				( session == null ? null : session.getFactory() ), (String) ois.readObject() );
	}
}
