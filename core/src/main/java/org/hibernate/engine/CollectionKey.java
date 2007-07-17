//$Id: CollectionKey.java 9194 2006-02-01 19:59:07Z steveebersole $
package org.hibernate.engine;

import org.hibernate.EntityMode;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;



import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

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
		this.hashCode = generateHashCode(); //cache the hashcode
	}

	public boolean equals(Object other) {
		CollectionKey that = (CollectionKey) other;
		return that.role.equals(role) &&
		       keyType.isEqual(that.key, key, entityMode, factory);
	}

	public int generateHashCode() {
		int result = 17;
		result = 37 * result + role.hashCode();
		result = 37 * result + keyType.getHashCode(key, entityMode, factory);
		return result;
	}

	public int hashCode() {
		return hashCode;
	}

	public String getRole() {
		return role;
	}

	public Serializable getKey() {
		return key;
	}

	public String toString() {
		return "CollectionKey" +
		       MessageHelper.collectionInfoString( factory.getCollectionPersister(role), key, factory );
	}

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 * @throws java.io.IOException
	 */
	void serialize(ObjectOutputStream oos) throws IOException {
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
	 * @return The deserialized CollectionKey
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static CollectionKey deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		return new CollectionKey(
				( String ) ois.readObject(),
		        ( Serializable ) ois.readObject(),
		        ( Type ) ois.readObject(),
		        EntityMode.parse( ( String ) ois.readObject() ),
		        session.getFactory()
		);
	}
}