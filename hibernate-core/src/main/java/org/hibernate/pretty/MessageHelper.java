/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.pretty;

import java.io.Serializable;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * MessageHelper methods for rendering log messages relating to managed
 * entities and collections typically used in log statements and exception
 * messages.
 *
 * @author Max Andersen, Gavin King
 */
public final class MessageHelper {

	private MessageHelper() {
	}


	// entities ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Generate an info message string relating to a particular entity,
	 * based on the given entityName and id.
	 *
	 * @param entityName The defined entity name.
	 * @param id The entity id value.
	 * @return An info string, in the form [FooBar#1].
	 */
	public static String infoString(String entityName, Serializable id) {
		StringBuilder s = new StringBuilder();
		s.append( '[' );
		if( entityName == null ) {
			s.append( "<null entity name>" );
		}
		else {
			s.append( entityName );
		}
		s.append( '#' );

		if ( id == null ) {
			s.append( "<null>" );
		}
		else {
			s.append( id );
		}
		s.append( ']' );

		return s.toString();
	}

	/**
	 * Generate an info message string relating to a particular entity.
	 *
	 * @param entityDescriptor The entityDescriptor for the entity
	 * @param id The entity id value
	 * @param factory The session factory - Could be null!
	 * @return An info string, in the form [FooBar#1]
	 */
	public static String infoString(
			EntityDescriptor entityDescriptor,
			Object id, 
			SessionFactoryImplementor factory) {
		StringBuilder s = new StringBuilder();
		s.append( '[' );
		JavaTypeDescriptor idJavaTypeDescriptor;
		if( entityDescriptor == null ) {
			s.append( "<null EntityPersister>" );
			idJavaTypeDescriptor = null;
		}
		else {
			s.append( entityDescriptor.getEntityName() );
			idJavaTypeDescriptor = entityDescriptor.getIdentifierDescriptor().getJavaTypeDescriptor();
		}
		s.append( '#' );

		if ( id == null ) {
			s.append( "<null>" );
		}
		else {
			if ( idJavaTypeDescriptor == null ) {
				s.append( id );
			}
			else {
				if ( factory != null ) {
					s.append( idJavaTypeDescriptor.extractLoggableRepresentation( id ) );
				}
				else {
					s.append( "<not loggable>" );
				}
			}
		}
		s.append( ']' );

		return s.toString();

	}

	/**
	 * Generate an info message string relating to a particular entity,.
	 *
	 * @param entityDescriptor The entityDescriptor for the entity
	 * @param id The entity id value
	 * @param identifierJavaTypeDescriptor The entity identifier JavaTypeDescriptor
	 * @param factory The session factory
	 * @return An info string, in the form [FooBar#1]
	 */
	public static String infoString(
			EntityDescriptor entityDescriptor,
			Object id, 
			JavaTypeDescriptor identifierJavaTypeDescriptor,
			SessionFactoryImplementor factory) {
		StringBuilder s = new StringBuilder();
		s.append( '[' );
		if( entityDescriptor == null ) {
			s.append( "<null EntityPersister>" );
		}
		else {
			s.append( entityDescriptor.getEntityName() );
		}
		s.append( '#' );

		if ( id == null ) {
			s.append( "<null>" );
		}
		else {
			s.append( identifierJavaTypeDescriptor.extractLoggableRepresentation( id ) );
		}
		s.append( ']' );

		return s.toString();
	}

	/**
	 * Generate an info message string relating to a series of entities.
	 *
	 * @param entityDescriptor The entityDescriptor for the entities
	 * @param ids The entity id values
	 * @param factory The session factory
	 * @return An info string, in the form [FooBar#<1,2,3>]
	 */
	public static String infoString(
			EntityDescriptor entityDescriptor,
			Serializable[] ids, 
			SessionFactoryImplementor factory) {
		StringBuilder s = new StringBuilder();
		s.append( '[' );
		if( entityDescriptor == null ) {
			s.append( "<null EntityPersister>" );
		}
		else {
			s.append( entityDescriptor.getEntityName() );
			s.append( "#<" );
			for ( int i=0; i<ids.length; i++ ) {
				s.append( entityDescriptor.getIdentifierDescriptor()
								  .getJavaTypeDescriptor()
								  .extractLoggableRepresentation( ids[i] ) );
				if ( i < ids.length-1 ) {
					s.append( ", " );
				}
			}
			s.append( '>' );
		}
		s.append( ']' );

		return s.toString();

	}

	/**
	 * Generate an info message string relating to given entity persister.
	 *
	 * @param persister The persister.
	 * @return An info string, in the form [FooBar]
	 */
	public static String infoString(EntityDescriptor persister) {
		StringBuilder s = new StringBuilder();
		s.append( '[' );
		if ( persister == null ) {
			s.append( "<null EntityPersister>" );
		}
		else {
			s.append( persister.getEntityName() );
		}
		s.append( ']' );
		return s.toString();
	}

	/**
	 * Generate an info message string relating to a given property value
	 * for an entity.
	 *
	 * @param entityName The entity name
	 * @param propertyName The name of the property
	 * @param key The property value.
	 * @return An info string, in the form [Foo.bars#1]
	 */
	public static String infoString(String entityName, String propertyName, Object key) {
		StringBuilder s = new StringBuilder()
				.append( '[' )
				.append( entityName )
				.append( '.' )
				.append( propertyName )
				.append( '#' );

		if ( key == null ) {
			s.append( "<null>" );
		}
		else {
			s.append( key );
		}
		s.append( ']' );
		return s.toString();
	}


	// collections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * Generate an info message string relating to a particular managed
	 * collection.  Attempts to intelligently handle property-refs issues
	 * where the collection key is not the same as the owner key.
	 *
	 * @param collectionDescriptor The PersistentCollectionDescriptor for the collection
	 * @param collection The collection itself
	 * @param collectionKey The collection key
	 * @param session The session
	 * @return An info string, in the form [Foo.bars#1]
	 */
	public static String collectionInfoString( 
			PersistentCollectionDescriptor collectionDescriptor,
			PersistentCollection collection,
			Serializable collectionKey,
			SharedSessionContractImplementor session ) {
		
		StringBuilder s = new StringBuilder();
		s.append( '[' );
		if ( collectionDescriptor == null ) {
			s.append( "<unreferenced>" );
		}
		else {
			s.append( collectionDescriptor.getNavigableRole().getFullPath() );
			s.append( '#' );

			JavaTypeDescriptor ownerIdentifierJavaTypeDescriptor = collectionDescriptor.getCollectionKeyDescriptor()
					.getJavaTypeDescriptor();
			Serializable ownerKey;
			// TODO: Is it redundant to attempt to use the collectionKey,
			// or is always using the owner id sufficient?
			if ( collectionKey.getClass().isAssignableFrom( 
					ownerIdentifierJavaTypeDescriptor.getJavaType() ) ) {
				ownerKey = collectionKey;
			}
			else {
				ownerKey = session.getPersistenceContext()
						.getEntry( collection.getOwner() ).getId();
			}
			s.append( ownerIdentifierJavaTypeDescriptor.extractLoggableRepresentation( ownerKey ) );
		}
		s.append( ']' );

		return s.toString();
	}

	/**
	 * Generate an info message string relating to a series of managed
	 * collections.
	 *
	 * @param collectionDescriptor The PersistentCollectionDescriptor for the collections
	 * @param ids The id values of the owners
	 * @param factory The session factory
	 * @return An info string, in the form [Foo.bars#<1,2,3>]
	 */
	public static String collectionInfoString(
			PersistentCollectionDescriptor collectionDescriptor,
			Serializable[] ids, 
			SessionFactoryImplementor factory) {
		StringBuilder s = new StringBuilder();
		s.append( '[' );
		if ( collectionDescriptor == null ) {
			s.append( "<unreferenced>" );
		}
		else {
			s.append( collectionDescriptor.getNavigableRole().getFullPath() );
			s.append( "#<" );
			for ( int i = 0; i < ids.length; i++ ) {
				addIdToCollectionInfoString( collectionDescriptor, ids[i], factory, s );
				if ( i < ids.length-1 ) {
					s.append( ", " );
				}
			}
			s.append( '>' );
		}
		s.append( ']' );
		return s.toString();
	}

	/**
	 * Generate an info message string relating to a particular managed
	 * collection.
	 *
	 * @param collectionDescriptor The PersistentCollectionDescriptor for the collection
	 * @param id The id value of the owner
	 * @param factory The session factory
	 * @return An info string, in the form [Foo.bars#1]
	 */
	public static String collectionInfoString(
			PersistentCollectionDescriptor collectionDescriptor,
			Serializable id, 
			SessionFactoryImplementor factory) {
		StringBuilder s = new StringBuilder();
		s.append( '[' );
		if ( collectionDescriptor == null ) {
			s.append( "<unreferenced>" );
		}
		else {
			s.append( collectionDescriptor.getNavigableRole().getFullPath() );
			s.append( '#' );

			if ( id == null ) {
				s.append( "<null>" );
			}
			else {
				addIdToCollectionInfoString( collectionDescriptor, id, factory, s );
			}
		}
		s.append( ']' );

		return s.toString();
	}
	
	private static void addIdToCollectionInfoString(
			PersistentCollectionDescriptor collectionDescriptor,
			Serializable id,
			SessionFactoryImplementor factory,
			StringBuilder s ) {
		// Need to use the identifier type of the collection owner
		// since the incoming is value is actually the owner's id.
		// Using the collection's key type causes problems with
		// property-ref keys.
		// Also need to check that the expected identifier type matches
		// the given ID.  Due to property-ref keys, the collection key
		// may not be the owner key.
		JavaTypeDescriptor ownerIdentifierJavaTypeDescriptor = collectionDescriptor.getCollectionKeyDescriptor()
				.getJavaTypeDescriptor();
		if ( id.getClass().isAssignableFrom( 
				ownerIdentifierJavaTypeDescriptor.getJavaType() ) ) {
			s.append( ownerIdentifierJavaTypeDescriptor.extractLoggableRepresentation( id ) );
		}
		else {
			// TODO: This is a crappy backup if a property-ref is used.
			// If the reference is an object w/o toString(), this isn't going to work.
			s.append( id.toString() );
		}
	}

	/**
	 * Generate an info message string relating to a particular managed
	 * collection.
	 *
	 * @param role The role-name of the collection
	 * @param id The id value of the owner
	 * @return An info string, in the form [Foo.bars#1]
	 */
	public static String collectionInfoString(String role, Serializable id) {
		StringBuilder s = new StringBuilder();
		s.append( '[' );
		if( role == null ) {
			s.append( "<unreferenced>" );
		}
		else {
			s.append( role );
			s.append( '#' );

			if ( id == null ) {
				s.append( "<null>" );
			}
			else {
				s.append( id );
			}
		}
		s.append( ']' );
		return s.toString();
	}

}
