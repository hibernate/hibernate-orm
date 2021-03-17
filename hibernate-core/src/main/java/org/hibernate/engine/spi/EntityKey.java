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

import org.hibernate.AssertionFailure;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Uniquely identifies of an entity instance in a particular Session by identifier.
 * Note that it's only safe to be used within the scope of a Session: it doesn't consider for example the tenantId
 * as part of the equality definition.
 * <p/>
 * Information used to determine uniqueness consists of the entity-name and the identifier value (see {@link #equals}).
 * <p/>
 * Performance considerations: lots of instances of this type are created at runtime. Make sure each one is as small as possible
 * by storing just the essential needed.
 *
 * @author Gavin King
 * @author Sanne Grinovero
 */
public final class EntityKey implements Serializable {

	private final Serializable identifier;
	private final int hashCode;
	private final EntityPersister persister;

	/**
	 * Construct a unique identifier for an entity class instance.
	 * <p/>
	 * NOTE : This signature has changed to accommodate both entity mode and multi-tenancy, both of which relate to
	 * the Session to which this key belongs.  To help minimize the impact of these changes in the future, the
	 * {@link SessionImplementor#generateEntityKey} method was added to hide the session-specific changes.
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 */
	public EntityKey(Serializable id, EntityPersister persister) {
		this.persister = persister;
		if ( id == null ) {
			throw new AssertionFailure( "null identifier" );
		}
		this.identifier = id;
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 17;
		final String rootEntityName = persister.getRootEntityName();
		result = 37 * result + ( rootEntityName != null ? rootEntityName.hashCode() : 0 );
		result = 37 * result + persister.getIdentifierType().getHashCode( identifier, persister.getFactory() );
		return result;
	}

	public boolean isBatchLoadable() {
		return persister.isBatchLoadable();
	}

	public Serializable getIdentifier() {
		return identifier;
	}

	public String getEntityName() {
		return persister.getEntityName();
	}

	public EntityPersister getPersister() {
		return persister;
	}

	@Override
	public boolean equals(Object other) {
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
		return persister.getIdentifierType().isEqual( otherKey.identifier, this.identifier, persister.getFactory() );
	}

	private boolean samePersistentType(final EntityKey otherKey) {
		if ( otherKey.persister == persister ) {
			return true;
		}
		else {
			return Objects.equals( otherKey.persister.getRootEntityName(), persister.getRootEntityName() );
		}
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "EntityKey" +
				MessageHelper.infoString( this.persister, identifier, persister.getFactory() );
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
		final Serializable id = (Serializable) ois.readObject();
		final String entityName = (String) ois.readObject();
		final EntityPersister entityPersister = sessionFactory.getEntityPersister( entityName );
		return new EntityKey(id, entityPersister);
	}
}
