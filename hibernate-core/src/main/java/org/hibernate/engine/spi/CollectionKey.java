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
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Uniquely identifies a collection instance in a particular session.
 *
 * @author Gavin King
 */
public final class CollectionKey implements Serializable {
	private final NavigableRole role;
	private final Object key;
	private final JavaTypeDescriptor keyType;
	private final int hashCode;

	public CollectionKey(
			PersistentCollectionDescriptor collectionDescriptor,
			Object key) {
		this(
				collectionDescriptor.getNavigableRole(),
				key,
				collectionDescriptor.getKeyJavaTypeDescriptor()
		);
	}

	public CollectionKey(PersistentCollectionDescriptor collectionDescriptor, Object key, EntityMode em) {
		this( collectionDescriptor.getNavigableRole(), key, collectionDescriptor.getKeyJavaTypeDescriptor());
	}

	private CollectionKey(
			NavigableRole role,
			Object key,
			JavaTypeDescriptor keyType) {
		this.role = role;
		this.key = key;
		this.keyType = keyType;
		//cache the hash-code
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 17;
		result = 37 * result + role.hashCode();
		result = 37 * result + keyType.extractHashCode( key );
		return result;
	}

	/**
	 * @deprecated (since 6.0) use {@link #getNavigableRole}
	 */
	@Deprecated
	public String getRole() {
		return role.getFullPath();
	}

	public NavigableRole getNavigableRole(){
		return role;
	}


	public Object getKey() {
		return key;
	}

	@Override
	public String toString() {
		return "CollectionKey" + MessageHelper.collectionInfoString( role.getFullPath(), key );
	}

	public String toLoggableString() {
		return MessageHelper.collectionInfoString( role.getFullPath(), key );
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
				&& keyType.areEqual( that.key, key );
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
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 *
	 * @return The deserialized CollectionKey
	 *
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static CollectionKey deserialize(
			ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new CollectionKey(
				(NavigableRole) ois.readObject(),
				ois.readObject(),
				(JavaTypeDescriptor) ois.readObject()
		);
	}
}
