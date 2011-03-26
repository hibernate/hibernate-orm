/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 20082011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * Uniquely identifies of an entity instance in a particular session by identifier.
 * <p/>
 * Information used to determine uniqueness consists of the entity-name and the identifier value (see {@link #equals}).
 *
 * @author Gavin King
 */
public final class EntityKey implements Serializable {
	private final Serializable identifier;
	private final String entityName;
	private final String rootEntityName;
	private final EntityMode entityMode;
	private final String tenantId;

	private final int hashCode;

	private final Type identifierType;
	private final boolean isBatchLoadable;
	private final SessionFactoryImplementor factory;

	/**
	 * Construct a unique identifier for an entity class instance.
	 * <p>
	 * NOTE : This signature has changed to accommodate both entity mode and multi-tenancy, both of which relate to
	 * the Session to which this key belongs.  To help minimize the impact of these changes in the future, the
	 * {@link SessionImplementor#generateEntityKey} method was added to hide the session-specific changes.
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 * @param entityMode The entity mode of the session to which this key belongs
	 * @param tenantId The tenant identifier of the session to which this key belongs
	 */
	public EntityKey(Serializable id, EntityPersister persister, EntityMode entityMode, String tenantId) {
		if ( id == null ) {
			throw new AssertionFailure( "null identifier" );
		}
		this.identifier = id; 
		this.rootEntityName = persister.getRootEntityName();
		this.entityName = persister.getEntityName();
		this.entityMode = entityMode;
		this.tenantId = tenantId;

		this.identifierType = persister.getIdentifierType();
		this.isBatchLoadable = persister.isBatchLoadable();
		this.factory = persister.getFactory();
		this.hashCode = generateHashCode();
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
	 * @param tenantId The entity's tenant id (from the session that loaded it).
	 */
	private EntityKey(
			Serializable identifier,
	        String rootEntityName,
	        String entityName,
	        Type identifierType,
	        boolean batchLoadable,
	        SessionFactoryImplementor factory,
	        EntityMode entityMode,
			String tenantId) {
		this.identifier = identifier;
		this.rootEntityName = rootEntityName;
		this.entityName = entityName;
		this.identifierType = identifierType;
		this.isBatchLoadable = batchLoadable;
		this.factory = factory;
		this.entityMode = entityMode;
		this.tenantId = tenantId;
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 17;
		result = 37 * result + rootEntityName.hashCode();
		result = 37 * result + identifierType.getHashCode( identifier, entityMode, factory );
		return result;
	}

	public boolean isBatchLoadable() {
		return isBatchLoadable;
	}

	public Serializable getIdentifier() {
		return identifier;
	}

	public String getEntityName() {
		return entityName;
	}

	@Override
	public boolean equals(Object other) {
		EntityKey otherKey = (EntityKey) other;
		return otherKey.rootEntityName.equals(this.rootEntityName) && 
			identifierType.isEqual(otherKey.identifier, this.identifier, entityMode, factory);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "EntityKey" + 
			MessageHelper.infoString( factory.getEntityPersister( entityName ), identifier, factory );
	}

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 *
	 * @throws IOException Thrown by Java I/O
	 */
	void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( identifier );
		oos.writeUTF( rootEntityName );
		oos.writeUTF( entityName );
		oos.writeObject( identifierType );
		oos.writeBoolean( isBatchLoadable );
		oos.writeUTF( entityMode.toString() );
		oos.writeUTF( tenantId );
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
	 * @throws IOException Thrown by Java I/O
	 * @throws ClassNotFoundException Thrown by Java I/O
	 */
	static EntityKey deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		return new EntityKey(
				( Serializable ) ois.readObject(),
		        ois.readUTF(),
		        ois.readUTF(),
		        ( Type ) ois.readObject(),
		        ois.readBoolean(),
		        ( session == null ? null : session.getFactory() ),
		        EntityMode.parse( ois.readUTF() ),
				ois.readUTF()
		);
	}
}
