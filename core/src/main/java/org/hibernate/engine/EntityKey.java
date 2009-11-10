/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * Uniquely identifies of an entity instance in a particular session by identifier.
 * <p/>
 * Uniqueing information consists of the entity-name and the identifier value.
 *
 * @see EntityUniqueKey
 * @author Gavin King
 */
public final class EntityKey implements Serializable {
	private final Serializable identifier;
	private final String rootEntityName;
	private final String entityName;
	private final Type identifierType;
	private final boolean isBatchLoadable;
	private final SessionFactoryImplementor factory;
	private final int hashCode;
	private final EntityMode entityMode;

	/**
	 * Construct a unique identifier for an entity class instance
	 */
	public EntityKey(Serializable id, EntityPersister persister, EntityMode entityMode) {
		if ( id == null ) {
			throw new AssertionFailure( "null identifier" );
		}
		this.identifier = id; 
		this.entityMode = entityMode;
		this.rootEntityName = persister.getRootEntityName();
		this.entityName = persister.getEntityName();
		this.identifierType = persister.getIdentifierType();
		this.isBatchLoadable = persister.isBatchLoadable();
		this.factory = persister.getFactory();
		hashCode = generateHashCode(); //cache the hashcode
	}

	/**
	 * Used to reconstruct an EntityKey during deserialization.
	 *
	 * @param identifier The identifier value
	 * @param rootEntityName The root entity name
	 * @param entityName The specific entity name
	 * @param identifierType The type of the identifier value
	 * @param batchLoadable Whether represented entity is eligible for batch loading
	 * @param factory The session factory
	 * @param entityMode The entity's entity mode
	 */
	private EntityKey(
			Serializable identifier,
	        String rootEntityName,
	        String entityName,
	        Type identifierType,
	        boolean batchLoadable,
	        SessionFactoryImplementor factory,
	        EntityMode entityMode) {
		this.identifier = identifier;
		this.rootEntityName = rootEntityName;
		this.entityName = entityName;
		this.identifierType = identifierType;
		this.isBatchLoadable = batchLoadable;
		this.factory = factory;
		this.entityMode = entityMode;
		this.hashCode = generateHashCode();
	}

	public boolean isBatchLoadable() {
		return isBatchLoadable;
	}

	/**
	 * Get the user-visible identifier
	 */
	public Serializable getIdentifier() {
		return identifier;
	}

	public String getEntityName() {
		return entityName;
	}

	public boolean equals(Object other) {
		EntityKey otherKey = (EntityKey) other;
		return otherKey.rootEntityName.equals(this.rootEntityName) && 
			identifierType.isEqual(otherKey.identifier, this.identifier, entityMode, factory);
	}
	
	private int generateHashCode() {
		int result = 17;
		result = 37 * result + rootEntityName.hashCode();
		result = 37 * result + identifierType.getHashCode( identifier, entityMode, factory );
		return result;
	}

	public int hashCode() {
		return hashCode;
	}

	public String toString() {
		return "EntityKey" + 
			MessageHelper.infoString( factory.getEntityPersister( entityName ), identifier, factory );
	}

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 * @throws IOException
	 */
	void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( identifier );
		oos.writeObject( rootEntityName );
		oos.writeObject( entityName );
		oos.writeObject( identifierType );
		oos.writeBoolean( isBatchLoadable );
		oos.writeObject( entityMode.toString() );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param session The session being deserialized.
	 * @return The deserialized EntityEntry
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static EntityKey deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		return new EntityKey(
				( Serializable ) ois.readObject(),
		        ( String ) ois.readObject(),
		        ( String ) ois.readObject(),
		        ( Type ) ois.readObject(),
		        ois.readBoolean(),
		        ( session == null ? null : session.getFactory() ),
		        EntityMode.parse( ( String ) ois.readObject() )
		);
	}
}
