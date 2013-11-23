/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * Used to uniquely key an entity instance in relation to a particular session
 * by some unique property reference, as opposed to identifier.
 * <p/>
 * Uniqueing information consists of the entity-name, the referenced
 * property name, and the referenced property value.
 *
 * @author Gavin King
 * @see EntityKey
 */
public class EntityUniqueKey implements Serializable {
	private final String uniqueKeyName;
	private final String entityName;
	private final Object key;
	private final Type keyType;
	private final EntityMode entityMode;
	private final int hashCode;

	public EntityUniqueKey(
			final String entityName,
			final String uniqueKeyName,
			final Object semiResolvedKey,
			final Type keyType,
			final EntityMode entityMode,
			final SessionFactoryImplementor factory) {
		this.uniqueKeyName = uniqueKeyName;
		this.entityName = entityName;
		this.key = semiResolvedKey;
		this.keyType = keyType.getSemiResolvedType( factory );
		this.entityMode = entityMode;
		this.hashCode = generateHashCode( factory );
	}

	public String getEntityName() {
		return entityName;
	}

	public Object getKey() {
		return key;
	}

	public String getUniqueKeyName() {
		return uniqueKeyName;
	}

	public int generateHashCode(SessionFactoryImplementor factory) {
		int result = 17;
		result = 37 * result + entityName.hashCode();
		result = 37 * result + uniqueKeyName.hashCode();
		result = 37 * result + keyType.getHashCode( key, factory );
		return result;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		EntityUniqueKey that = (EntityUniqueKey) other;
		return that.entityName.equals( entityName )
				&& that.uniqueKeyName.equals( uniqueKeyName )
				&& keyType.isEqual( that.key, key );
	}

	@Override
	public String toString() {
		return "EntityUniqueKey" + MessageHelper.infoString( entityName, uniqueKeyName, key );
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		checkAbilityToSerialize();
		oos.defaultWriteObject();
	}

	private void checkAbilityToSerialize() {
		// The unique property value represented here may or may not be
		// serializable, so we do an explicit check here in order to generate
		// a better error message
		if ( key != null && !Serializable.class.isAssignableFrom( key.getClass() ) ) {
			throw new IllegalStateException(
					"Cannot serialize an EntityUniqueKey which represents a non " +
							"serializable property value [" + entityName + "." + uniqueKeyName + "]"
			);
		}
	}

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 *
	 * @throws IOException
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		checkAbilityToSerialize();
		oos.writeObject( uniqueKeyName );
		oos.writeObject( entityName );
		oos.writeObject( key );
		oos.writeObject( keyType );
		oos.writeObject( entityMode );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param session The session being deserialized.
	 *
	 * @return The deserialized EntityEntry
	 *
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static EntityUniqueKey deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		return new EntityUniqueKey(
				(String) ois.readObject(),
				(String) ois.readObject(),
				ois.readObject(),
				(Type) ois.readObject(),
				(EntityMode) ois.readObject(),
				session.getFactory()
		);
	}
}
