//$Id: MessageHelper.java 9561 2006-03-07 14:17:16Z steve.ebersole@jboss.com $
package org.hibernate.pretty;

import java.io.Serializable;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

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
		StringBuffer s = new StringBuffer();
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
	 * @param persister The persister for the entity
	 * @param id The entity id value
	 * @param factory The session factory
	 * @return An info string, in the form [FooBar#1]
	 */
	public static String infoString(
			EntityPersister persister, 
			Object id, 
			SessionFactoryImplementor factory) {
		StringBuffer s = new StringBuffer();
		s.append( '[' );
		Type idType;
		if( persister == null ) {
			s.append( "<null EntityPersister>" );
			idType = null;
		}
		else {
			s.append( persister.getEntityName() );
			idType = persister.getIdentifierType();
		}
		s.append( '#' );

		if ( id == null ) {
			s.append( "<null>" );
		}
		else {
			if ( idType == null ) {
				s.append( id );
			}
			else {
				s.append( idType.toLoggableString( id, factory ) );
			}
		}
		s.append( ']' );

		return s.toString();

	}

	/**
	 * Generate an info message string relating to a particular entity,.
	 *
	 * @param persister The persister for the entity
	 * @param id The entity id value
	 * @param identifierType The entity identifier type mapping
	 * @param factory The session factory
	 * @return An info string, in the form [FooBar#1]
	 */
	public static String infoString(
			EntityPersister persister, 
			Object id, 
			Type identifierType,
			SessionFactoryImplementor factory) {
		StringBuffer s = new StringBuffer();
		s.append( '[' );
		if( persister == null ) {
			s.append( "<null EntityPersister>" );
		}
		else {
			s.append( persister.getEntityName() );
		}
		s.append( '#' );

		if ( id == null ) {
			s.append( "<null>" );
		}
		else {
			s.append( identifierType.toLoggableString( id, factory ) );
		}
		s.append( ']' );

		return s.toString();
	}

	/**
	 * Generate an info message string relating to a series of entities.
	 *
	 * @param persister The persister for the entities
	 * @param ids The entity id values
	 * @param factory The session factory
	 * @return An info string, in the form [FooBar#<1,2,3>]
	 */
	public static String infoString(
			EntityPersister persister, 
			Serializable[] ids, 
			SessionFactoryImplementor factory) {
		StringBuffer s = new StringBuffer();
		s.append( '[' );
		if( persister == null ) {
			s.append( "<null EntityPersister>" );
		}
		else {
			s.append( persister.getEntityName() );
			s.append( "#<" );
			for ( int i=0; i<ids.length; i++ ) {
				s.append( persister.getIdentifierType().toLoggableString( ids[i], factory ) );
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
	public static String infoString(EntityPersister persister) {
		StringBuffer s = new StringBuffer();
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
		StringBuffer s = new StringBuffer()
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
	 * Generate an info message string relating to a series of managed
	 * collections.
	 *
	 * @param persister The persister for the collections
	 * @param ids The id values of the owners
	 * @param factory The session factory
	 * @return An info string, in the form [Foo.bars#<1,2,3>]
	 */
	public static String collectionInfoString(
			CollectionPersister persister, 
			Serializable[] ids, 
			SessionFactoryImplementor factory) {
		StringBuffer s = new StringBuffer();
		s.append( '[' );
		if ( persister == null ) {
			s.append( "<unreferenced>" );
		}
		else {
			s.append( persister.getRole() );
			s.append( "#<" );
			for ( int i = 0; i < ids.length; i++ ) {
				// Need to use the identifier type of the collection owner
				// since the incoming is value is actually the owner's id.
				// Using the collection's key type causes problems with
				// property-ref keys...
				s.append( persister.getOwnerEntityPersister().getIdentifierType().toLoggableString( ids[i], factory ) );
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
	 * @param persister The persister for the collection
	 * @param id The id value of the owner
	 * @param factory The session factory
	 * @return An info string, in the form [Foo.bars#1]
	 */
	public static String collectionInfoString(
			CollectionPersister persister, 
			Serializable id, 
			SessionFactoryImplementor factory) {
		StringBuffer s = new StringBuffer();
		s.append( '[' );
		if ( persister == null ) {
			s.append( "<unreferenced>" );
		}
		else {
			s.append( persister.getRole() );
			s.append( '#' );

			if ( id == null ) {
				s.append( "<null>" );
			}
			else {
				// Need to use the identifier type of the collection owner
				// since the incoming is value is actually the owner's id.
				// Using the collection's key type causes problems with
				// property-ref keys...
				s.append( persister.getOwnerEntityPersister().getIdentifierType().toLoggableString( id, factory ) );
			}
		}
		s.append( ']' );

		return s.toString();
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
		StringBuffer s = new StringBuffer();
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
